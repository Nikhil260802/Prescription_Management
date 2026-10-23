package Prescription_Management.Service;

import Prescription_Management.DTO.RequestDto.RecordEntryRequest;
import Prescription_Management.DTO.RequestDto.RecordRequest;
import Prescription_Management.DTO.ResponseDto.RecordResponse;
import Prescription_Management.Entity.Client;
import Prescription_Management.Entity.Record;
import Prescription_Management.Entity.RecordEntry;
import Prescription_Management.Entity.RecordStatus;
import Prescription_Management.Entity.User;
import Prescription_Management.Exception.ResourceNotFoundException;
import Prescription_Management.Repository.ClientRepository;
import Prescription_Management.Repository.RecordRepository;
import Prescription_Management.Repository.UserRepository;
import Prescription_Management.Service.ServiceImpl.RecordServiceImpl;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordServiceImpl Unit Tests")
class RecordServiceImplTest {

    @Mock private RecordRepository recordRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks private RecordServiceImpl recordService;

    private User user;
    private Client client;
    private Record record;
    private RecordRequest recordRequest;
    private RecordResponse recordResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Dr. Test");

        client = new Client();
        client.setId(2L);
        client.setName("Patient Jane");

        record = new Record();
        record.setId(10L);
        record.setUser(user);
        record.setClient(client);
        record.setRecordDate(LocalDate.now());
        record.setDiagnosis("Hypertension");
        record.setStatus(RecordStatus.DRAFT);

        RecordEntry entry = new RecordEntry();
        entry.setId(100L);
        entry.setEntryName("Amlodipine");
        entry.setQuantity(30);
        entry.setFrequency("Once daily");
        entry.setDuration(30);
        record.addEntry(entry);

        RecordEntryRequest entryReq = new RecordEntryRequest();
        entryReq.setEntryName("Amlodipine");
        entryReq.setQuantity(30);
        entryReq.setFrequency("Once daily");
        entryReq.setDuration(30);

        recordRequest = new RecordRequest();
        recordRequest.setUserId(1L);
        recordRequest.setClientId(2L);
        recordRequest.setRecordDate(LocalDate.now());
        recordRequest.setDiagnosis("Hypertension");
        recordRequest.setStatus(RecordStatus.DRAFT);
        recordRequest.setEntries(new ArrayList<>(List.of(entryReq)));

        recordResponse = new RecordResponse();
        recordResponse.setId(10L);
        recordResponse.setDiagnosis("Hypertension");
        recordResponse.setStatus(RecordStatus.DRAFT);
    }

    @Nested
    @DisplayName("createRecord()")
    class CreateRecord {

        @Test
        @DisplayName("should create record successfully with entries")
        void shouldCreateRecordSuccessfully() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(clientRepository.findById(2L)).willReturn(Optional.of(client));
            given(recordRepository.save(any(Record.class))).willReturn(record);
            given(modelMapper.map(record, RecordResponse.class)).willReturn(recordResponse);

            RecordResponse result = recordService.createRecord(recordRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            then(recordRepository).should().save(any(Record.class));
        }

        @Test
        @DisplayName("should return existing record when duplicate idempotency key is provided")
        void shouldReturnExistingRecordWhenDuplicateIdempotencyKeyProvided() {
            String idempKey = "KEY-12345";
            recordRequest.setIdempotencyKey(idempKey);

            given(recordRepository.findByIdempotencyKey(idempKey)).willReturn(Optional.of(record));
            given(modelMapper.map(record, RecordResponse.class)).willReturn(recordResponse);

            RecordResponse result = recordService.createRecord(recordRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            then(recordRepository).should(never()).save(any(Record.class));
        }

        @Test
        @DisplayName("should save record with idempotency key when provided and not previously recorded")
        void shouldSaveRecordWithIdempotencyKey() {
            String idempKey = "NEW-KEY-12345";
            given(recordRepository.findByIdempotencyKey(idempKey)).willReturn(Optional.empty());
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(clientRepository.findById(2L)).willReturn(Optional.of(client));
            given(recordRepository.save(any(Record.class))).willAnswer(inv -> {
                Record r = inv.getArgument(0);
                assertThat(r.getIdempotencyKey()).isEqualTo(idempKey);
                return r;
            });
            given(modelMapper.map(any(), eq(RecordResponse.class))).willReturn(recordResponse);

            RecordResponse result = recordService.createRecord(recordRequest, idempKey);

            assertThat(result).isNotNull();
            then(recordRepository).should().save(any(Record.class));
        }

        @Test
        @DisplayName("should default status to DRAFT when request status is null")
        void shouldDefaultStatusToDraft() {
            recordRequest.setStatus(null);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(clientRepository.findById(2L)).willReturn(Optional.of(client));
            given(recordRepository.save(any(Record.class))).willAnswer(inv -> {
                Record r = inv.getArgument(0);
                assertThat(r.getStatus()).isEqualTo(RecordStatus.DRAFT);
                return r;
            });
            given(modelMapper.map(any(), eq(RecordResponse.class))).willReturn(recordResponse);

            recordService.createRecord(recordRequest);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> recordService.createRecord(recordRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when client not found")
        void shouldThrowWhenClientNotFound() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(clientRepository.findById(2L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> recordService.createRecord(recordRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Client not found");
        }
    }

    @Nested
    @DisplayName("getRecordById()")
    class GetRecordById {

        @Test
        @DisplayName("should return record when found")
        void shouldReturnRecordWhenFound() {
            given(recordRepository.findById(10L)).willReturn(Optional.of(record));
            given(modelMapper.map(record, RecordResponse.class)).willReturn(recordResponse);

            RecordResponse result = recordService.getRecordById(10L);

            assertThat(result).isEqualTo(recordResponse);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when record not found")
        void shouldThrowWhenRecordNotFound() {
            given(recordRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> recordService.getRecordById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("getAllRecords()")
    class GetAllRecords {

        @Test
        @DisplayName("should return paginated records")
        void shouldReturnPagedRecords() {
            Pageable pageable = PageRequest.of(0, 10);
            given(recordRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(record)));
            given(modelMapper.map(record, RecordResponse.class)).willReturn(recordResponse);

            Page<RecordResponse> result = recordService.getAllRecords(pageable);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getRecordsByUser()")
    class GetRecordsByUser {

        @Test
        @DisplayName("should return records for existing user")
        void shouldReturnRecordsForUser() {
            Pageable pageable = PageRequest.of(0, 10);
            given(userRepository.existsById(1L)).willReturn(true);
            given(recordRepository.findByUserId(1L, pageable)).willReturn(new PageImpl<>(List.of(record)));
            given(modelMapper.map(record, RecordResponse.class)).willReturn(recordResponse);

            Page<RecordResponse> result = recordService.getRecordsByUser(1L, pageable);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrowWhenUserNotExist() {
            Pageable pageable = PageRequest.of(0, 10);
            given(userRepository.existsById(99L)).willReturn(false);

            assertThatThrownBy(() -> recordService.getRecordsByUser(99L, pageable))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getRecordsByClient()")
    class GetRecordsByClient {

        @Test
        @DisplayName("should return records for existing client")
        void shouldReturnRecordsForClient() {
            Pageable pageable = PageRequest.of(0, 10);
            given(clientRepository.existsById(2L)).willReturn(true);
            given(recordRepository.findByClientId(2L, pageable)).willReturn(new PageImpl<>(List.of(record)));
            given(modelMapper.map(record, RecordResponse.class)).willReturn(recordResponse);

            Page<RecordResponse> result = recordService.getRecordsByClient(2L, pageable);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when client does not exist")
        void shouldThrowWhenClientNotExist() {
            Pageable pageable = PageRequest.of(0, 10);
            given(clientRepository.existsById(99L)).willReturn(false);

            assertThatThrownBy(() -> recordService.getRecordsByClient(99L, pageable))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateRecord()")
    class UpdateRecord {

        @Test
        @DisplayName("should update draft record successfully")
        void shouldUpdateDraftRecord() {
            given(recordRepository.findById(10L)).willReturn(Optional.of(record));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(clientRepository.findById(2L)).willReturn(Optional.of(client));
            given(recordRepository.save(any(Record.class))).willReturn(record);
            given(modelMapper.map(record, RecordResponse.class)).willReturn(recordResponse);

            RecordResponse result = recordService.updateRecord(10L, recordRequest);

            assertThat(result).isNotNull();
            then(recordRepository).should().save(record);
        }

        @Test
        @DisplayName("should throw IllegalStateException when updating ISSUED record")
        void shouldThrowWhenUpdatingIssuedRecord() {
            record.setStatus(RecordStatus.ISSUED);
            given(recordRepository.findById(10L)).willReturn(Optional.of(record));

            assertThatThrownBy(() -> recordService.updateRecord(10L, recordRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Issued records cannot be edited");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when entries list is empty")
        void shouldThrowWhenEntriesEmpty() {
            recordRequest.setEntries(new ArrayList<>());
            given(recordRepository.findById(10L)).willReturn(Optional.of(record));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(clientRepository.findById(2L)).willReturn(Optional.of(client));

            assertThatThrownBy(() -> recordService.updateRecord(10L, recordRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("A record must contain at least one entry");
        }
    }

    @Nested
    @DisplayName("deleteRecord()")
    class DeleteRecord {

        @Test
        @DisplayName("should delete DRAFT record successfully")
        void shouldDeleteDraftRecord() {
            given(recordRepository.findById(10L)).willReturn(Optional.of(record));

            recordService.deleteRecord(10L);

            then(recordRepository).should().delete(record);
        }

        @Test
        @DisplayName("should throw IllegalStateException when deleting ISSUED record")
        void shouldThrowWhenDeletingIssuedRecord() {
            record.setStatus(RecordStatus.ISSUED);
            given(recordRepository.findById(10L)).willReturn(Optional.of(record));

            assertThatThrownBy(() -> recordService.deleteRecord(10L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Issued records cannot be deleted");

            then(recordRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when record to delete not found")
        void shouldThrowWhenRecordNotFound() {
            given(recordRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> recordService.deleteRecord(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
