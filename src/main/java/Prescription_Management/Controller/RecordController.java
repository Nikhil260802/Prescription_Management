package Prescription_Management.Controller;

import Prescription_Management.DTO.RequestDto.RecordRequest;
import Prescription_Management.DTO.ResponseDto.RecordResponse;
import Prescription_Management.Service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/records")
@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
@Tag(name = "4. Prescription Records", description = "Endpoints for creating and viewing medical prescription records (Doctor and Admin)")
public class RecordController {

    @Autowired
    private RecordService recordService;

    @PostMapping
    @Operation(
            summary = "Create prescription record",
            description = "Creates a new prescription record with diagnosis and medication entries. Supports optional Idempotency-Key header to prevent duplicate submissions."
    )
    public ResponseEntity<RecordResponse> createRecord(
            @Parameter(description = "Optional unique idempotency key (e.g. UUID) to prevent duplicate submissions")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RecordRequest record) {
        String key = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey.trim()
                : record.getIdempotencyKey();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordService.createRecord(record, key));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prescription by ID", description = "Retrieves full prescription details including attached medication entries.")
    public ResponseEntity<RecordResponse> getRecordById(
            @PathVariable Long id) {
        return ResponseEntity.ok(recordService.getRecordById(id));
    }

    @GetMapping
    @Operation(summary = "List all prescriptions", description = "Returns a paginated list of all prescriptions in the system.")
    public ResponseEntity<Page<RecordResponse>> getAllRecords(
            @PageableDefault(
                    size = 10,
                    sort = "recordDate",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {
        return ResponseEntity.ok(recordService.getAllRecords(pageable));
    }

    @GetMapping("/getRecordsByUser/{userId}")
    @Operation(summary = "Get prescriptions by User (Doctor)", description = "Retrieves a paginated list of prescriptions issued by a specific doctor/user.")
    public ResponseEntity<Page<RecordResponse>> getRecordsByUser(
            @PathVariable Long userId,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "userId",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {
        return ResponseEntity.ok(
                recordService.getRecordsByUser(userId, pageable));
    }

    @GetMapping("/getRecordsByClient/{clientId}")
    @Operation(summary = "Get prescriptions by Client (Patient)", description = "Retrieves a paginated medical history of prescriptions for a specific patient/client.")
    public ResponseEntity<Page<RecordResponse>> getRecordsByClient(
            @PathVariable Long clientId,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "clientId",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {
        return ResponseEntity.ok(
                recordService.getRecordsByClient(clientId, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update prescription record", description = "Updates a DRAFT prescription. Modifying ISSUED prescriptions is rejected (409 Conflict).")
    public ResponseEntity<RecordResponse> updateRecord(
            @PathVariable Long id,
            @Valid @RequestBody RecordRequest request) {
        RecordResponse response =
                recordService.updateRecord(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete prescription record", description = "Deletes a DRAFT prescription. Deleting ISSUED prescriptions is rejected (409 Conflict).")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        recordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}
