package Prescription_Management.Controller;

import Prescription_Management.DTO.RequestDto.RecordEntryRequest;
import Prescription_Management.DTO.ResponseDto.RecordEntryResponse;
import Prescription_Management.Service.RecordEntryService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/records/{recordId}/entries")
@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
@Tag(name = "5. Prescription Entries", description = "Endpoints for managing individual medication entries on prescriptions (Doctor and Admin)")
public class RecordEntryController {

    @Autowired
    private RecordEntryService entryService;

    // CREATE ENTRY
    @PostMapping
    @Operation(summary = "Add medication entry", description = "Adds a new medication entry to a DRAFT prescription record.")
    public ResponseEntity<RecordEntryResponse> createEntry(
            @PathVariable Long recordId,
            @Valid @RequestBody RecordEntryRequest request) {

        RecordEntryResponse response =
                entryService.createEntry(recordId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    // GET ALL ENTRIES FOR A RECORD
    @GetMapping
    @Operation(summary = "List entries for prescription", description = "Retrieves all medication entries belonging to the specified prescription.")
    public ResponseEntity<Page<RecordEntryResponse>> getEntryById(
            @PathVariable Long recordId,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "id",
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {

        return ResponseEntity.ok(entryService.getEntriesByRecordId(recordId, pageable));
    }

    // GET ONE ENTRY
    @GetMapping("/{entryId}")
    @Operation(summary = "Get single entry", description = "Retrieves a specific medication entry by ID within a prescription.")
    public ResponseEntity<RecordEntryResponse> getEntriesByRecordId(
            @PathVariable Long recordId,
            @PathVariable Long entryId) {

        return ResponseEntity.ok(
                entryService.getEntryById(recordId, entryId));
    }

    // UPDATE ENTRY
    @PutMapping("/{entryId}")
    @Operation(summary = "Update medication entry", description = "Updates a medication entry on a DRAFT prescription.")
    public ResponseEntity<RecordEntryResponse> updateEntry(
            @PathVariable Long recordId,
            @PathVariable Long entryId,
            @Valid @RequestBody RecordEntryRequest request) {

        return ResponseEntity.ok(
                entryService.updateEntry(
                        recordId, entryId, request));
    }

    // DELETE ENTRY
    @DeleteMapping("/{entryId}")
    @Operation(summary = "Delete medication entry", description = "Deletes a medication entry. Prescriptions must maintain at least one entry.")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable Long recordId,
            @PathVariable Long entryId) {

        entryService.deleteEntry(recordId, entryId);

        return ResponseEntity.noContent().build();
    }
}
