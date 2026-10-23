package Prescription_Management.DTO.RequestDto;

import Prescription_Management.Entity.RecordStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class RecordRequest {

    private String idempotencyKey;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Client ID is required")
    private Long clientId;

    @NotNull(message = "Record date is required")
    @PastOrPresent(message = "Record date cannot be in the future")
    private LocalDate recordDate;

    @NotBlank(message = "Diagnosis is required")
    @Size(max = 1000)
    private String diagnosis;

    @Enumerated(EnumType.STRING)
    private RecordStatus status;

    @Valid
    @NotEmpty(message = "A record must contain at least one entry")
    private List<RecordEntryRequest> entries;
}
