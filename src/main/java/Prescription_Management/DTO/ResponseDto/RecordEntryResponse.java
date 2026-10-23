package Prescription_Management.DTO.ResponseDto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RecordEntryResponse {
    private Long id;
    private String entryName;
    private Integer quantity;
    private String frequency;
    private Integer duration;
    private String instructions;
}
