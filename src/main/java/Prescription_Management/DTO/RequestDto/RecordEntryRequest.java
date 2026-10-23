package Prescription_Management.DTO.RequestDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecordEntryRequest {

    @NotBlank(message = "Entry name is required")
    @Size(max = 200)
    private String entryName;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;

    @NotBlank(message = "Frequency is required")
    @Size(max = 100)
    private String frequency;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be greater than zero")
    private Integer duration;

    @Size(max = 1000)
    private String instructions;
}
