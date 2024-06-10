package one.digitalinnovation.gof.service.impl;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import one.digitalinnovation.gof.service.ClienteObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import one.digitalinnovation.gof.model.Cliente;
import one.digitalinnovation.gof.model.ClienteRepository;
import one.digitalinnovation.gof.model.Endereco;
import one.digitalinnovation.gof.model.EnderecoRepository;
import one.digitalinnovation.gof.service.ClienteService;
import one.digitalinnovation.gof.service.ViaCepService;

import java.util.ArrayList;

@Service
public class ClienteServiceImpl implements ClienteService {

    private List<ClienteObserver> observers = new ArrayList<>();

    @Override
    public void registrarObserver(ClienteObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removerObserver(ClienteObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notificarObservers(Cliente cliente) {
        for (ClienteObserver observer : observers) {
            observer.receberNotificacao(cliente);
        }
    }

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private EnderecoRepository enderecoRepository;
    @Autowired
    private ViaCepService viaCepService;

    @Override
    public Iterable<Cliente> buscarTodos() {

        return clienteRepository.findAll();
    }

    @Override
    public Cliente buscarPorId(Long id) {
        // Buscar Cliente por ID.
        Optional<Cliente> clienteOptional = clienteRepository.findById(id);
        return clienteOptional.orElseThrow(() -> new NoSuchElementException("Cliente não encontrado!"));

    }

    @Override
    public void inserir(Cliente cliente) {
        salvarClienteComCep(cliente);
        notificarObservers(cliente);
    }

    @Override
    public void atualizar(Long id, Cliente cliente) {
        // Buscar Cliente por ID, caso exista:
        Optional<Cliente> clienteBd = clienteRepository.findById(id);
        if (clienteBd.isPresent()) {
            salvarClienteComCep(cliente);
            notificarObservers(cliente);
        }
    }

    @Override
    public void deletar(Long id) {
        // Deletar Cliente por ID.
        Optional<Cliente>clienteBd = clienteRepository.findById(id);
        if(clienteBd.isPresent()){
            Cliente cliente = clienteBd.get();
            clienteRepository.deleteById(id);
            notificarObservers(cliente);
        }


    }

    private void salvarClienteComCep(Cliente cliente) {
        // Verificar se o Endereco do Cliente já existe (pelo CEP).
        String cep = cliente.getEndereco().getCep();
        Endereco endereco = enderecoRepository.findById(cep).orElseGet(() -> {
            // Caso não exista, integrar com o ViaCEP e persistir o retorno.
            Endereco novoEndereco = viaCepService.consultarCep(cep);
            enderecoRepository.save(novoEndereco);
            return novoEndereco;
        });
        cliente.setEndereco(endereco);
        // Inserir Cliente, vinculando o Endereco (novo ou existente).
        clienteRepository.save(cliente);
    }

}
