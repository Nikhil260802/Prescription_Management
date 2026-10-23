
package Prescription_Management.Service.ServiceImpl;

import Prescription_Management.DTO.RequestDto.RecordEntryRequest;
import Prescription_Management.DTO.ResponseDto.RecordEntryResponse;
import Prescription_Management.Entity.Record;
import Prescription_Management.Entity.RecordEntry;
import Prescription_Management.Entity.RecordStatus;
import Prescription_Management.Exception.ResourceNotFoundException;
import Prescription_Management.Repository.RecordEntryRepository;
import Prescription_Management.Repository.RecordRepository;
import Prescription_Management.Service.RecordEntryService;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecordEntryServiceImpl
        implements RecordEntryService {

    @Autowired
    private RecordEntryRepository entryRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private ModelMapper modelMapper;

    // CREATE ENTRY
    @Override
    @Transactional
    public RecordEntryResponse createEntry(
            Long recordId,
            RecordEntryRequest request) {

        Record record = getEditableRecord(recordId);

        RecordEntry entry = new RecordEntry();
        copyFields(request, entry);

        // Set both sides of the relationship.
        record.addEntry(entry);

        // Save the entry and obtain its generated ID.
        RecordEntry savedEntry = entryRepository.save(entry);

        return modelMapper.map(
                savedEntry, RecordEntryResponse.class);
    }

    // GET ENTRY BY ID, ENSURING IT BELONGS TO THE RECORD
    @Override
    @Transactional(readOnly = true)
    public RecordEntryResponse getEntryById(
            Long recordId,
            Long entryId) {

        RecordEntry entry = findEntry(entryId);

        validateEntryOwnership(recordId, entry);

        return modelMapper.map(
                entry, RecordEntryResponse.class);
    }

    // GET ALL ENTRIES FOR A RECORD
    @Override
    @Transactional(readOnly = true)
    public Page<RecordEntryResponse> getEntriesByRecordId(
            Long recordId, Pageable pageable) {

        if (!recordRepository.existsById(recordId)) {
            throw new ResourceNotFoundException(
                    "Record not found with ID: " + recordId);
        }

        return entryRepository.findByRecordId(recordId, pageable)
                .map(entry -> modelMapper.map(
                        entry, RecordEntryResponse.class));
    }

    // UPDATE ENTRY
    @Override
    @Transactional
    public RecordEntryResponse updateEntry(
            Long recordId,
            Long entryId,
            RecordEntryRequest request) {

        // Ensure the parent exists and is editable.
        getEditableRecord(recordId);

        RecordEntry existing = findEntry(entryId);

        validateEntryOwnership(recordId, existing);

        // Update the actual entity, not a temporary DTO.
        copyFields(request, existing);

        RecordEntry updatedEntry =
                entryRepository.save(existing);

        return modelMapper.map(
                updatedEntry, RecordEntryResponse.class);
    }

    // DELETE ENTRY
    @Override
    @Transactional
    public void deleteEntry(
            Long recordId,
            Long entryId) {

        Record record = getEditableRecord(recordId);

        RecordEntry entry = findEntry(entryId);

        validateEntryOwnership(recordId, entry);

        if (record.getEntries().size() <= 1) {
            throw new IllegalStateException(
                    "A record must contain at least one entry");
        }

        // orphanRemoval = true deletes the removed child.
        record.removeEntry(entry);

        recordRepository.save(record);
    }

    // FIND ENTRY OR THROW EXCEPTION
    private RecordEntry findEntry(Long entryId) {

        return entryRepository.findById(entryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Record entry not found with ID: " + entryId));
    }

    // VALIDATE THAT ENTRY BELONGS TO THE SPECIFIED RECORD
    private void validateEntryOwnership(
            Long recordId,
            RecordEntry entry) {

        if (entry.getRecord() == null
                || !entry.getRecord().getId().equals(recordId)) {

            throw new IllegalArgumentException(
                    "Entry does not belong to record ID: "
                            + recordId);
        }
    }

    // ENSURE RECORD EXISTS AND CAN BE MODIFIED
    private Record getEditableRecord(Long recordId) {

        Record record = recordRepository.findById(recordId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Record not found with ID: "
                                        + recordId));

        if (record.getStatus() == RecordStatus.ISSUED) {
            throw new IllegalStateException(
                    "Entries of an issued record cannot be modified");
        }

        return record;
    }

    // COPY REQUEST FIELDS INTO THE ENTITY
    private void copyFields(
            RecordEntryRequest source,
            RecordEntry target) {

        target.setEntryName(source.getEntryName());
        target.setQuantity(source.getQuantity());
        target.setFrequency(source.getFrequency());
        target.setDuration(source.getDuration());
        target.setInstructions(source.getInstructions());
    }
}
