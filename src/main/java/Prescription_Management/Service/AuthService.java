package Prescription_Management.Service;

import Prescription_Management.DTO.RequestDto.LoginRequest;
import Prescription_Management.DTO.RequestDto.RegisterRequest;
import Prescription_Management.DTO.ResponseDto.AuthResponse;
import Prescription_Management.DTO.ResponseDto.UserResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getCurrentUser(String registrationNumber);
}
