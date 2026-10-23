package Prescription_Management.DTO.ResponseDto;

import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ClientResponse {
    private long id;
    private String name;
    private LocalDate dateOfBirth;
    private String phoneNumber;
}
