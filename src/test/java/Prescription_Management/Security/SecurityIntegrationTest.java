package Prescription_Management.Security;

import Prescription_Management.DTO.RequestDto.LoginRequest;
import Prescription_Management.DTO.RequestDto.RegisterRequest;
import Prescription_Management.Entity.Role;
import Prescription_Management.Entity.User;
import Prescription_Management.Repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String doctorToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Ensure test admin exists
        User admin = userRepository.findByRegistrationNumber("TEST_ADMIN").orElseGet(() -> {
            User u = new User();
            u.setName("Test Admin");
            u.setRegistrationNumber("TEST_ADMIN");
            u.setPhoneNumber("+1234567890");
            u.setPassword(passwordEncoder.encode("Password123!"));
            u.setRole(Role.ADMIN);
            return userRepository.save(u);
        });

        // Ensure test doctor exists
        User doctor = userRepository.findByRegistrationNumber("TEST_DOC").orElseGet(() -> {
            User u = new User();
            u.setName("Test Doctor");
            u.setRegistrationNumber("TEST_DOC");
            u.setPhoneNumber("+1987654321");
            u.setPassword(passwordEncoder.encode("Password123!"));
            u.setRole(Role.DOCTOR);
            return userRepository.save(u);
        });

        CustomUserDetails adminDetails = new CustomUserDetails(admin);
        adminToken = jwtService.generateToken(adminDetails, admin.getId(), admin.getRole().name(), admin.getName());

        CustomUserDetails docDetails = new CustomUserDetails(doctor);
        doctorToken = jwtService.generateToken(docDetails, doctor.getId(), doctor.getRole().name(), doctor.getName());
    }

    @Test
    @DisplayName("Should successfully register a new Doctor")
    void testRegisterSuccess() throws Exception {
        String uniqueReg = "DOC_" + System.currentTimeMillis();
        RegisterRequest req = new RegisterRequest(
                "Dr. John Doe",
                uniqueReg,
                "+1234567890",
                "Secret1234!",
                Role.DOCTOR
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.registrationNumber").value(uniqueReg))
                .andExpect(jsonPath("$.role").value("DOCTOR"));
    }

    @Test
    @DisplayName("Should fail registration when registration number already exists")
    void testRegisterDuplicateRegistrationNumber() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "Duplicate Doctor",
                "TEST_DOC",
                "+1234567890",
                "Secret1234!",
                Role.DOCTOR
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLoginSuccess() throws Exception {
        LoginRequest req = new LoginRequest("TEST_ADMIN", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Should fail login with incorrect password")
    void testLoginInvalidPassword() throws Exception {
        LoginRequest req = new LoginRequest("TEST_ADMIN", "WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint should return 401")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Doctor attempting to access /api/users should be forbidden (403)")
    void testDoctorCannotAccessUsersEndpoint() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Admin can access /api/users endpoint (200)")
    void testAdminCanAccessUsersEndpoint() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Both Doctor and Admin can access /api/clients endpoint (200)")
    void testDoctorAndAdminCanAccessClients() throws Exception {
        mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated user can view own profile at /api/auth/me")
    void testGetCurrentUser() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationNumber").value("TEST_DOC"))
                .andExpect(jsonPath("$.role").value("DOCTOR"));
    }
}
