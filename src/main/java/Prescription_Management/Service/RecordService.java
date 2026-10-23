package Prescription_Management.Service;

import Prescription_Management.DTO.RequestDto.RecordRequest;
import Prescription_Management.DTO.ResponseDto.RecordResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RecordService {
    RecordResponse createRecord(@Valid RecordRequest request);
    RecordResponse createRecord(@Valid RecordRequest request, String idempotencyKey);
    RecordResponse getRecordById(Long id);
    Page<RecordResponse> getAllRecords(Pageable pageable);
    Page<RecordResponse> getRecordsByUser(Long userId, Pageable pageable);
    Page<RecordResponse> getRecordsByClient(Long clientId, Pageable pageable);
    RecordResponse updateRecord(Long id, RecordRequest request);
    void deleteRecord(Long id);
}
