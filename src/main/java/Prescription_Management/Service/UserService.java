package Prescription_Management.Service;

import Prescription_Management.DTO.RequestDto.UserRequest;
import Prescription_Management.DTO.ResponseDto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse createUser(UserRequest request);

    UserResponse getUserById(Long id);

    Page<UserResponse> getAllUsers(Pageable pageable);

    UserResponse updateUser(Long id, UserRequest request);

    void deleteUser(Long id);
}
