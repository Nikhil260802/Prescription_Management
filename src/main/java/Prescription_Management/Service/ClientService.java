package Prescription_Management.Service;

import Prescription_Management.DTO.RequestDto.ClientRequest;
import Prescription_Management.DTO.ResponseDto.ClientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientService {
    ClientResponse createClient(ClientRequest request);

    ClientResponse getClientById(Long id);

    Page<ClientResponse> getAllClients(Pageable pageable);

    ClientResponse updateClient(Long id, ClientRequest request);

    void deleteClient(Long id);
}
