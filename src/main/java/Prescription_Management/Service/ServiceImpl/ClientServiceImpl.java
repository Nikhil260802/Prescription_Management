package Prescription_Management.Service.ServiceImpl;

import Prescription_Management.DTO.RequestDto.ClientRequest;
import Prescription_Management.DTO.ResponseDto.ClientResponse;
import Prescription_Management.Entity.Client;
import Prescription_Management.Exception.ResourceNotFoundException;
import Prescription_Management.Repository.ClientRepository;
import Prescription_Management.Service.ClientService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class ClientServiceImpl implements ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional
    public ClientResponse createClient(ClientRequest request) {
        Client client = modelMapper.map(request, Client.class);
        client.setId(null);
        Client saved = clientRepository.save(client);
        return modelMapper.map(saved, ClientResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with ID: " + id));

        return modelMapper.map(client, ClientResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientResponse> getAllClients(Pageable pageable) {
        return clientRepository.findAll(pageable)
                .map(client -> modelMapper.map(client, ClientResponse.class));
    }

    @Override
    @Transactional
    public ClientResponse updateClient(Long id, ClientRequest request) {
        Client existing = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with ID: " + id));

        existing.setName(request.getName());
        existing.setDateOfBirth(request.getDateOfBirth());
        existing.setPhoneNumber(request.getPhoneNumber());

        Client saved = clientRepository.save(existing);
        return modelMapper.map(saved, ClientResponse.class);
    }

    @Override
    @Transactional
    public void deleteClient(Long id) {
        Client client = clientRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Client is not found with ID: " + id));

        if (!client.getRecords().isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete a client associated with prescription records");
        }

        clientRepository.delete(client);
    }
}
