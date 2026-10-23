package Prescription_Management.Service.ServiceImpl;

import Prescription_Management.DTO.RequestDto.LoginRequest;
import Prescription_Management.DTO.RequestDto.RegisterRequest;
import Prescription_Management.DTO.ResponseDto.AuthResponse;
import Prescription_Management.DTO.ResponseDto.UserResponse;
import Prescription_Management.Entity.Role;
import Prescription_Management.Entity.User;
import Prescription_Management.Exception.ResourceNotFoundException;
import Prescription_Management.Repository.UserRepository;
import Prescription_Management.Security.CustomUserDetails;
import Prescription_Management.Security.JwtService;
import Prescription_Management.Service.AuthService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new IllegalArgumentException("Registration number already exists: " + request.getRegistrationNumber());
        }

        User user = new User();
        user.setName(request.getName());
        user.setRegistrationNumber(request.getRegistrationNumber());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : Role.DOCTOR);

        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String token = jwtService.generateToken(
                userDetails,
                savedUser.getId(),
                savedUser.getRole().name(),
                savedUser.getName()
        );

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .userId(savedUser.getId())
                .registrationNumber(savedUser.getRegistrationNumber())
                .name(savedUser.getName())
                .role(savedUser.getRole())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getRegistrationNumber(),
                        request.getPassword()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        String token = jwtService.generateToken(
                userDetails,
                user.getId(),
                user.getRole().name(),
                user.getName()
        );

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .userId(user.getId())
                .registrationNumber(user.getRegistrationNumber())
                .name(user.getName())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String registrationNumber) {
        User user = userRepository.findByRegistrationNumber(registrationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with registration number: " + registrationNumber));

        return modelMapper.map(user, UserResponse.class);
    }
}
