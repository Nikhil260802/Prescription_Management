package Prescription_Management.Service;

import Prescription_Management.DTO.RequestDto.ClientRequest;
import Prescription_Management.DTO.ResponseDto.ClientResponse;
import Prescription_Management.Entity.Client;
import Prescription_Management.Entity.Record;
import Prescription_Management.Exception.ResourceNotFoundException;
import Prescription_Management.Repository.ClientRepository;
import Prescription_Management.Service.ServiceImpl.ClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientServiceImpl Unit Tests")
class ClientServiceImplTest {

    @Mock private ClientRepository clientRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks private ClientServiceImpl clientService;

    private Client client;
    private ClientRequest clientRequest;
    private ClientResponse clientResponse;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(1L);
        client.setName("Jane Doe");
        client.setPhoneNumber("+9876543210");
        client.setDateOfBirth(LocalDate.of(1990, 5, 15));

        clientRequest = new ClientRequest("Jane Doe", LocalDate.of(1990, 5, 15), "+9876543210");
        clientResponse = new ClientResponse(1L, "Jane Doe", LocalDate.of(1990, 5, 15), "+9876543210");
    }

    @Nested
    @DisplayName("createClient()")
    class CreateClient {

        @Test
        @DisplayName("should create and return client response")
        void shouldCreateClientSuccessfully() {
            given(modelMapper.map(clientRequest, Client.class)).willReturn(client);
            given(clientRepository.save(client)).willReturn(client);
            given(modelMapper.map(client, ClientResponse.class)).willReturn(clientResponse);

            ClientResponse result = clientService.createClient(clientRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Jane Doe");
            then(clientRepository).should().save(client);
        }

        @Test
        @DisplayName("should set client ID to null before saving")
        void shouldNullifyIdBeforeSave() {
            client.setId(99L);
            given(modelMapper.map(clientRequest, Client.class)).willReturn(client);
            given(clientRepository.save(any(Client.class))).willReturn(client);
            given(modelMapper.map(any(Client.class), eq(ClientResponse.class))).willReturn(clientResponse);

            clientService.createClient(clientRequest);

            assertThat(client.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("getClientById()")
    class GetClientById {

        @Test
        @DisplayName("should return client when found")
        void shouldReturnClientWhenFound() {
            given(clientRepository.findById(1L)).willReturn(Optional.of(client));
            given(modelMapper.map(client, ClientResponse.class)).willReturn(clientResponse);

            ClientResponse result = clientService.getClientById(1L);

            assertThat(result.getName()).isEqualTo("Jane Doe");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when client not found")
        void shouldThrowWhenClientNotFound() {
            given(clientRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.getClientById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("getAllClients()")
    class GetAllClients {

        @Test
        @DisplayName("should return paginated client responses")
        void shouldReturnPagedClients() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Client> page = new PageImpl<>(List.of(client));
            given(clientRepository.findAll(pageable)).willReturn(page);
            given(modelMapper.map(client, ClientResponse.class)).willReturn(clientResponse);

            Page<ClientResponse> result = clientService.getAllClients(pageable);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("updateClient()")
    class UpdateClient {

        @Test
        @DisplayName("should update client fields successfully")
        void shouldUpdateClientSuccessfully() {
            ClientRequest updatedData = new ClientRequest("Jane Updated", LocalDate.of(1995, 3, 20), "+1111111111");

            given(clientRepository.findById(1L)).willReturn(Optional.of(client));
            given(clientRepository.save(any(Client.class))).willReturn(client);
            given(modelMapper.map(any(Client.class), eq(ClientResponse.class))).willReturn(clientResponse);

            ClientResponse result = clientService.updateClient(1L, updatedData);

            assertThat(result).isNotNull();
            assertThat(client.getName()).isEqualTo("Jane Updated");
            then(clientRepository).should().save(client);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when client to update does not exist")
        void shouldThrowWhenClientNotFound() {
            given(clientRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.updateClient(99L, clientRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteClient()")
    class DeleteClient {

        @Test
        @DisplayName("should delete client when no records are associated")
        void shouldDeleteClientWithNoRecords() {
            client.getRecords().clear();
            given(clientRepository.findById(1L)).willReturn(Optional.of(client));

            clientService.deleteClient(1L);

            then(clientRepository).should().delete(client);
        }

        @Test
        @DisplayName("should throw IllegalStateException when client has associated records")
        void shouldThrowWhenClientHasRecords() {
            client.getRecords().add(new Record());
            given(clientRepository.findById(1L)).willReturn(Optional.of(client));

            assertThatThrownBy(() -> clientService.deleteClient(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot delete a client associated with prescription records");

            then(clientRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when client to delete does not exist")
        void shouldThrowWhenClientToDeleteNotFound() {
            given(clientRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.deleteClient(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
