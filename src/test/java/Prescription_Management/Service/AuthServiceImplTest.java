package Prescription_Management.Service;

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
import Prescription_Management.Service.ServiceImpl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private ModelMapper modelMapper;

    @InjectMocks private AuthServiceImpl authService;

    private User user;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Dr. Test");
        user.setRegistrationNumber("DOC_AUTH_01");
        user.setPhoneNumber("+1234567890");
        user.setPassword("encoded_pass");
        user.setRole(Role.DOCTOR);

        registerRequest = new RegisterRequest("Dr. Test", "DOC_AUTH_01", "+1234567890", "RawPass@123", Role.DOCTOR);
        loginRequest = new LoginRequest("DOC_AUTH_01", "RawPass@123");
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should register user successfully and return AuthResponse with token")
        void shouldRegisterSuccessfully() {
            given(userRepository.existsByRegistrationNumber("DOC_AUTH_01")).willReturn(false);
            given(passwordEncoder.encode("RawPass@123")).willReturn("encoded_pass");
            given(userRepository.save(any(User.class))).willReturn(user);
            given(jwtService.generateToken(any(), eq(1L), eq("DOCTOR"), eq("Dr. Test"))).willReturn("mock_jwt_token");
            given(jwtService.getExpirationTime()).willReturn(86400000L);

            AuthResponse response = authService.register(registerRequest);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("mock_jwt_token");
            assertThat(response.getRegistrationNumber()).isEqualTo("DOC_AUTH_01");
            assertThat(response.getRole()).isEqualTo(Role.DOCTOR);
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when registration number exists")
        void shouldThrowWhenDuplicateRegistration() {
            given(userRepository.existsByRegistrationNumber("DOC_AUTH_01")).willReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Registration number already exists");

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should default role to DOCTOR when role is null in register request")
        void shouldDefaultRoleToDoctor() {
            registerRequest.setRole(null);
            given(userRepository.existsByRegistrationNumber("DOC_AUTH_01")).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn("encoded");
            given(userRepository.save(any(User.class))).willAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            given(jwtService.generateToken(any(), any(), any(), any())).willReturn("token");

            AuthResponse res = authService.register(registerRequest);

            assertThat(res.getRole()).isEqualTo(Role.DOCTOR);
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("should authenticate and return AuthResponse on valid credentials")
        void shouldLoginSuccessfully() {
            CustomUserDetails userDetails = new CustomUserDetails(user);
            Authentication auth = mock(Authentication.class);
            given(auth.getPrincipal()).willReturn(userDetails);

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(auth);
            given(jwtService.generateToken(any(), eq(1L), eq("DOCTOR"), eq("Dr. Test"))).willReturn("login_jwt");
            given(jwtService.getExpirationTime()).willReturn(86400000L);

            AuthResponse response = authService.login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("login_jwt");
            assertThat(response.getRegistrationNumber()).isEqualTo("DOC_AUTH_01");
        }

        @Test
        @DisplayName("should throw BadCredentialsException when authentication fails")
        void shouldThrowOnBadCredentials() {
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("getCurrentUser()")
    class GetCurrentUser {

        @Test
        @DisplayName("should return UserResponse for existing registration number")
        void shouldReturnUserResponse() {
            UserResponse expected = new UserResponse(1L, "Dr. Test", "DOC_AUTH_01", "+1234567890", Role.DOCTOR);
            given(userRepository.findByRegistrationNumber("DOC_AUTH_01")).willReturn(Optional.of(user));
            given(modelMapper.map(user, UserResponse.class)).willReturn(expected);

            UserResponse result = authService.getCurrentUser("DOC_AUTH_01");

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            given(userRepository.findByRegistrationNumber("UNKNOWN")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getCurrentUser("UNKNOWN"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("UNKNOWN");
        }
    }
}
