package Prescription_Management.Service;

import Prescription_Management.DTO.RequestDto.RecordEntryRequest;
import Prescription_Management.DTO.ResponseDto.RecordEntryResponse;
import Prescription_Management.Entity.Record;
import Prescription_Management.Entity.RecordEntry;
import Prescription_Management.Entity.RecordStatus;
import Prescription_Management.Exception.ResourceNotFoundException;
import Prescription_Management.Repository.RecordEntryRepository;
import Prescription_Management.Repository.RecordRepository;
import Prescription_Management.Service.ServiceImpl.RecordEntryServiceImpl;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordEntryServiceImpl Unit Tests")
class RecordEntryServiceImplTest {

    @Mock private RecordEntryRepository entryRepository;
    @Mock private RecordRepository recordRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks private RecordEntryServiceImpl entryService;

    private Record draftRecord;
    private Record issuedRecord;
    private RecordEntry entry;
    private RecordEntryRequest entryRequest;
    private RecordEntryResponse entryResponse;

    @BeforeEach
    void setUp() {
        draftRecord = new Record();
        draftRecord.setId(1L);
        draftRecord.setStatus(RecordStatus.DRAFT);

        issuedRecord = new Record();
        issuedRecord.setId(2L);
        issuedRecord.setStatus(RecordStatus.ISSUED);

        entry = new RecordEntry();
        entry.setId(10L);
        entry.setRecord(draftRecord);
        entry.setEntryName("Paracetamol");
        entry.setQuantity(2);
        entry.setFrequency("Twice daily");
        entry.setDuration(7);

        draftRecord.setEntries(new ArrayList<>(List.of(entry)));

        entryRequest = new RecordEntryRequest();
        entryRequest.setEntryName("Amoxicillin");
        entryRequest.setQuantity(1);
        entryRequest.setFrequency("Once daily");
        entryRequest.setDuration(5);

        entryResponse = new RecordEntryResponse(10L, "Paracetamol", 2, "Twice daily", 7, null);
    }

    @Nested
    @DisplayName("createEntry()")
    class CreateEntry {

        @Test
        @DisplayName("should create entry on a DRAFT record")
        void shouldCreateEntryOnDraftRecord() {
            given(recordRepository.findById(1L)).willReturn(Optional.of(draftRecord));
            given(entryRepository.save(any(RecordEntry.class))).willReturn(entry);
            given(modelMapper.map(entry, RecordEntryResponse.class)).willReturn(entryResponse);

            RecordEntryResponse result = entryService.createEntry(1L, entryRequest);

            assertThat(result).isNotNull();
            then(entryRepository).should().save(any(RecordEntry.class));
        }

        @Test
        @DisplayName("should throw IllegalStateException when adding entry to ISSUED record")
        void shouldThrowWhenAddingToIssuedRecord() {
            given(recordRepository.findById(2L)).willReturn(Optional.of(issuedRecord));

            assertThatThrownBy(() -> entryService.createEntry(2L, entryRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Entries of an issued record cannot be modified");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when record not found")
        void shouldThrowWhenRecordNotFound() {
            given(recordRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> entryService.createEntry(99L, entryRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("getEntryById()")
    class GetEntryById {

        @Test
        @DisplayName("should return entry when found and belongs to the record")
        void shouldReturnEntryWhenValid() {
            given(entryRepository.findById(10L)).willReturn(Optional.of(entry));
            given(modelMapper.map(entry, RecordEntryResponse.class)).willReturn(entryResponse);

            RecordEntryResponse result = entryService.getEntryById(1L, 10L);

            assertThat(result.getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when entry belongs to different record")
        void shouldThrowWhenEntryBelongsToDifferentRecord() {
            given(entryRepository.findById(10L)).willReturn(Optional.of(entry)); // entry belongs to record 1

            assertThatThrownBy(() -> entryService.getEntryById(999L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Entry does not belong to record ID");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when entry not found")
        void shouldThrowWhenEntryNotFound() {
            given(entryRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> entryService.getEntryById(1L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getEntriesByRecordId()")
    class GetEntriesByRecordId {

        @Test
        @DisplayName("should return paginated entries for a valid record")
        void shouldReturnPagedEntries() {
            Pageable pageable = PageRequest.of(0, 10);
            given(recordRepository.existsById(1L)).willReturn(true);
            given(entryRepository.findByRecordId(1L, pageable))
                    .willReturn(new PageImpl<>(List.of(entry)));
            given(modelMapper.map(entry, RecordEntryResponse.class)).willReturn(entryResponse);

            Page<RecordEntryResponse> result = entryService.getEntriesByRecordId(1L, pageable);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when record not found")
        void shouldThrowWhenRecordNotFound() {
            Pageable pageable = PageRequest.of(0, 10);
            given(recordRepository.existsById(99L)).willReturn(false);

            assertThatThrownBy(() -> entryService.getEntriesByRecordId(99L, pageable))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateEntry()")
    class UpdateEntry {

        @Test
        @DisplayName("should update entry on a DRAFT record")
        void shouldUpdateEntrySuccessfully() {
            given(recordRepository.findById(1L)).willReturn(Optional.of(draftRecord));
            given(entryRepository.findById(10L)).willReturn(Optional.of(entry));
            given(entryRepository.save(entry)).willReturn(entry);
            given(modelMapper.map(entry, RecordEntryResponse.class)).willReturn(entryResponse);

            RecordEntryResponse result = entryService.updateEntry(1L, 10L, entryRequest);

            assertThat(result).isNotNull();
            assertThat(entry.getEntryName()).isEqualTo("Amoxicillin");
        }

        @Test
        @DisplayName("should throw IllegalStateException when updating entry on ISSUED record")
        void shouldThrowWhenUpdatingOnIssuedRecord() {
            given(recordRepository.findById(2L)).willReturn(Optional.of(issuedRecord));

            assertThatThrownBy(() -> entryService.updateEntry(2L, 10L, entryRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Entries of an issued record cannot be modified");
        }
    }

    @Nested
    @DisplayName("deleteEntry()")
    class DeleteEntry {

        @Test
        @DisplayName("should delete entry when record has more than one entry")
        void shouldDeleteEntryWhenMoreThanOneEntryExists() {
            // Add a second entry so deletion is allowed
            RecordEntry second = new RecordEntry();
            second.setId(11L);
            second.setRecord(draftRecord);
            draftRecord.getEntries().add(second);

            given(recordRepository.findById(1L)).willReturn(Optional.of(draftRecord));
            given(entryRepository.findById(10L)).willReturn(Optional.of(entry));
            given(recordRepository.save(draftRecord)).willReturn(draftRecord);

            entryService.deleteEntry(1L, 10L);

            then(recordRepository).should().save(draftRecord);
        }

        @Test
        @DisplayName("should throw IllegalStateException when deleting the last entry")
        void shouldThrowWhenDeletingLastEntry() {
            // draftRecord has only one entry
            given(recordRepository.findById(1L)).willReturn(Optional.of(draftRecord));
            given(entryRepository.findById(10L)).willReturn(Optional.of(entry));

            assertThatThrownBy(() -> entryService.deleteEntry(1L, 10L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("A record must contain at least one entry");
        }

        @Test
        @DisplayName("should throw IllegalStateException when parent record is ISSUED")
        void shouldThrowWhenParentRecordIsIssued() {
            RecordEntry issuedEntry = new RecordEntry();
            issuedEntry.setId(20L);
            issuedEntry.setRecord(issuedRecord);

            given(recordRepository.findById(2L)).willReturn(Optional.of(issuedRecord));

            assertThatThrownBy(() -> entryService.deleteEntry(2L, 20L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Entries of an issued record cannot be modified");
        }
    }
}
