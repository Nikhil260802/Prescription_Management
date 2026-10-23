package Prescription_Management.DTO.ResponseDto;

import Prescription_Management.Entity.Role;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserResponse {

    private Long id;
    private String name;
    private String registrationNumber;
    private String phoneNumber;
    private Role role;
}
