package Prescription_Management.Service.ServiceImpl;

import Prescription_Management.DTO.RequestDto.UserRequest;
import Prescription_Management.DTO.ResponseDto.UserResponse;
import Prescription_Management.Entity.Role;
import Prescription_Management.Entity.User;
import Prescription_Management.Exception.ResourceNotFoundException;
import Prescription_Management.Repository.UserRepository;
import Prescription_Management.Service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new IllegalArgumentException("Registration number already exists: " + request.getRegistrationNumber());
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        User user = new User();
        user.setName(request.getName());
        user.setRegistrationNumber(request.getRegistrationNumber());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : Role.DOCTOR);

        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        return modelMapper.map(user, UserResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(u -> modelMapper.map(u, UserResponse.class));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        userRepository.findByRegistrationNumber(request.getRegistrationNumber())
                .ifPresent(found -> {
                    if (!found.getId().equals(id)) {
                        throw new IllegalArgumentException("Registration number already exists: " + request.getRegistrationNumber());
                    }
                });

        existingUser.setName(request.getName());
        existingUser.setRegistrationNumber(request.getRegistrationNumber());
        existingUser.setPhoneNumber(request.getPhoneNumber());

        if (request.getRole() != null) {
            existingUser.setRole(request.getRole());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User saved = userRepository.save(existingUser);
        return modelMapper.map(saved, UserResponse.class);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("User not found with ID: " + id));

        if (!user.getRecords().isEmpty()) {
            throw new IllegalStateException("Cannot delete a user associated with prescription records");
        }

        userRepository.delete(user);
    }
}
