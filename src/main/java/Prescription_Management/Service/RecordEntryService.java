
package Prescription_Management.Service;

import Prescription_Management.DTO.RequestDto.RecordEntryRequest;
import Prescription_Management.DTO.ResponseDto.RecordEntryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RecordEntryService {

    RecordEntryResponse createEntry(Long recordId, RecordEntryRequest request);

    RecordEntryResponse getEntryById(Long recordId, Long entryId);

    Page<RecordEntryResponse> getEntriesByRecordId(Long recordId, Pageable pageable);

    RecordEntryResponse updateEntry(Long recordId, Long entryId, RecordEntryRequest request);

    void deleteEntry(Long recordId, Long entryId);
}
