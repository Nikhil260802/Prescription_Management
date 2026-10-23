package Prescription_Management.Service.ServiceImpl;

import Prescription_Management.DTO.RequestDto.RecordEntryRequest;
import Prescription_Management.DTO.RequestDto.RecordRequest;
import Prescription_Management.DTO.ResponseDto.ClientResponse;
import Prescription_Management.DTO.ResponseDto.RecordEntryResponse;
import Prescription_Management.DTO.ResponseDto.RecordResponse;
import Prescription_Management.DTO.ResponseDto.UserResponse;
import Prescription_Management.Entity.Client;
import Prescription_Management.Entity.Record;
import Prescription_Management.Entity.RecordEntry;
import Prescription_Management.Entity.RecordStatus;
import Prescription_Management.Entity.User;
import Prescription_Management.Exception.ResourceNotFoundException;
import Prescription_Management.Repository.ClientRepository;
import Prescription_Management.Repository.RecordRepository;
import Prescription_Management.Repository.UserRepository;
import Prescription_Management.Service.RecordService;

import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Validated
public class RecordServiceImpl implements RecordService {

    @Autowired
    private RecordRepository recordRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private ModelMapper modelMapper;

    // CREATE RECORD (without explicit key parameter)
    @Override
    @Transactional
    public RecordResponse createRecord(@Valid RecordRequest request) {
        return createRecord(request, request.getIdempotencyKey());
    }

    // CREATE RECORD (with idempotency key)
    @Override
    @Transactional
    public RecordResponse createRecord(@Valid RecordRequest request, String idempotencyKey) {

        String key = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey.trim()
                : (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()
                    ? request.getIdempotencyKey().trim()
                    : null);

        // Check if a record with this idempotency key has already been created
        if (key != null) {
            Optional<Record> existingRecord = recordRepository.findByIdempotencyKey(key);
            if (existingRecord.isPresent()) {
                return modelMapper.map(existingRecord.get(), RecordResponse.class);
            }
        }

        Long userId = request.getUserId();
        Long clientId = request.getClientId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId));

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with ID: " + clientId));

        Record record = new Record();
        record.setIdempotencyKey(key);
        record.setUser(user);
        record.setClient(client);
        record.setRecordDate(request.getRecordDate());
        record.setDiagnosis(request.getDiagnosis());
        record.setStatus(request.getStatus() != null
                ? request.getStatus()
                : RecordStatus.DRAFT);

        for (RecordEntryRequest requestedEntry : request.getEntries()) {

            RecordEntry entry = new RecordEntry();

            entry.setEntryName(requestedEntry.getEntryName());
            entry.setQuantity(requestedEntry.getQuantity());
            entry.setFrequency(requestedEntry.getFrequency());
            entry.setDuration(requestedEntry.getDuration());
            entry.setInstructions(requestedEntry.getInstructions());

            // Automatically assigns entry.record = record
            record.addEntry(entry);
        }

        Record savedRecord = recordRepository.save(record);

        return modelMapper.map(savedRecord, RecordResponse.class);
    }

    // GET RECORD BY ID
    @Override
    @Transactional(readOnly = true)
    public RecordResponse getRecordById(Long id) {

        Record record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Record not found with ID: " + id));

        return modelMapper.map(record, RecordResponse.class);
    }

    // GET ALL RECORDS WITH PAGINATION
    @Override
    @Transactional(readOnly = true)
    public Page<RecordResponse> getAllRecords(Pageable pageable) {
        return recordRepository.findAll(pageable)
                .map(record -> modelMapper.map(record, RecordResponse.class));
    }

    // GET RECORDS BY USER
    @Override
    @Transactional(readOnly = true)
    public Page<RecordResponse> getRecordsByUser(Long userId, Pageable pageable) {

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException(
                    "User not found with ID: " + userId);
        }

        return recordRepository.findByUserId(userId, pageable)
                .map(record -> modelMapper.map(record, RecordResponse.class));
    }

    // GET RECORDS BY CLIENT
    @Override
    @Transactional(readOnly = true)
    public Page<RecordResponse> getRecordsByClient(Long clientId, Pageable pageable) {

        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException(
                    "Client not found with ID: " + clientId);
        }

        return recordRepository.findByClientId(clientId, pageable)
                .map(record -> modelMapper.map(record, RecordResponse.class));
    }

    // UPDATE RECORD
    @Override
    @Transactional
    public RecordResponse updateRecord(
            Long id,
            RecordRequest request) {

        Record existing = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Record not found with ID: " + id));

        if (existing.getStatus() == RecordStatus.ISSUED) {
            throw new IllegalStateException(
                    "Issued records cannot be edited");
        }

        Long userId = request.getUserId();
        Long clientId = request.getClientId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId));

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with ID: " + clientId));

        if (request.getEntries() == null
                || request.getEntries().isEmpty()) {
            throw new IllegalArgumentException(
                    "A record must contain at least one entry");
        }

        existing.setUser(user);
        existing.setClient(client);
        existing.setRecordDate(request.getRecordDate());
        existing.setDiagnosis(request.getDiagnosis());

        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        // Remove existing entries.
        for (RecordEntry oldEntry :
                new ArrayList<>(existing.getEntries())) {
            existing.removeEntry(oldEntry);
        }

        // Create and attach replacement entries.
        for (RecordEntryRequest requestedEntry :
                request.getEntries()) {

            RecordEntry entry = new RecordEntry();

            entry.setEntryName(requestedEntry.getEntryName());
            entry.setQuantity(requestedEntry.getQuantity());
            entry.setFrequency(requestedEntry.getFrequency());
            entry.setDuration(requestedEntry.getDuration());
            entry.setInstructions(requestedEntry.getInstructions());

            // Sets entry.record to the existing record.
            existing.addEntry(entry);
        }

        Record savedRecord = recordRepository.save(existing);

        return modelMapper.map(savedRecord, RecordResponse.class);
    }

    // DELETE RECORD
    @Override
    @Transactional
    public void deleteRecord(Long id) {

        Record record = recordRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Record not found with ID: " + id));

        if (record.getStatus() == RecordStatus.ISSUED) {
            throw new IllegalStateException(
                    "Issued records cannot be deleted");
        }

        recordRepository.delete(record);
    }

    // COPY ENTRY FIELDS WITHOUT ACCEPTING A CLIENT-SUPPLIED ID
    private RecordEntry copyEntry(RecordEntry source) {

        if (source == null) {
            throw new IllegalArgumentException(
                    "Record entry cannot be null");
        }

        RecordEntry entry = new RecordEntry();

        entry.setEntryName(source.getEntryName());
        entry.setQuantity(source.getQuantity());
        entry.setFrequency(source.getFrequency());
        entry.setDuration(source.getDuration());
        entry.setInstructions(source.getInstructions());

        return entry;
    }
}
