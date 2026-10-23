package Prescription_Management.DTO.ResponseDto;

import Prescription_Management.Entity.RecordStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RecordResponse {
    private Long id;
    private String idempotencyKey;
    private UserResponse user;
    private ClientResponse client;
    private LocalDate recordDate;
    private String diagnosis;
    private RecordStatus status;
    private List<RecordEntryResponse> entries;
}
