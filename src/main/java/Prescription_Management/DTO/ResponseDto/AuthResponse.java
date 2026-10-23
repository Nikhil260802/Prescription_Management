package Prescription_Management.DTO.ResponseDto;

import Prescription_Management.Entity.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn;
    private Long userId;
    private String registrationNumber;
    private String name;
    private Role role;
}
