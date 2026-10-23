package Prescription_Management.Controller;

import Prescription_Management.DTO.RequestDto.LoginRequest;
import Prescription_Management.DTO.RequestDto.RegisterRequest;
import Prescription_Management.Entity.Client;
import Prescription_Management.Entity.Role;
import Prescription_Management.Entity.User;
import Prescription_Management.Repository.ClientRepository;
import Prescription_Management.Repository.RecordRepository;
import Prescription_Management.Repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Seed known admin for login tests
        userRepository.findByRegistrationNumber("IT_ADMIN").orElseGet(() -> {
            User u = new User();
            u.setName("IT Admin");
            u.setRegistrationNumber("IT_ADMIN");
            u.setPhoneNumber("+1111000000");
            u.setPassword(passwordEncoder.encode("Admin@12345"));
            u.setRole(Role.ADMIN);
            return userRepository.save(u);
        });
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("should return 201 and JWT on valid registration")
        void shouldRegisterSuccessfully() throws Exception {
            String uniqueReg = "REG_" + System.currentTimeMillis();
            RegisterRequest req = new RegisterRequest("Test User", uniqueReg, "+1234567890", "Password@123", Role.DOCTOR);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").isString())
                    .andExpect(jsonPath("$.registrationNumber").value(uniqueReg))
                    .andExpect(jsonPath("$.role").value("DOCTOR"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("should return 400 on duplicate registration number")
        void shouldRejectDuplicateRegistrationNumber() throws Exception {
            RegisterRequest req = new RegisterRequest("Dup User", "IT_ADMIN", "+1234567890", "Password@123", Role.DOCTOR);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("should return 400 when password is too short")
        void shouldRejectShortPassword() throws Exception {
            RegisterRequest req = new RegisterRequest("Short Pass", "SHORT001", "+1234567890", "short", Role.DOCTOR);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("should return 400 when registration number is blank")
        void shouldRejectBlankRegistrationNumber() throws Exception {
            RegisterRequest req = new RegisterRequest("No Reg", "", "+1234567890", "Password@123", Role.DOCTOR);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should default role to DOCTOR when role is not provided")
        void shouldDefaultRoleToDoctorWhenOmitted() throws Exception {
            String uniqueReg = "NOROL_" + System.currentTimeMillis();
            String body = """
                    {"name":"No Role User","registrationNumber":"%s","phoneNumber":"+1234567890","password":"Password@123"}
                    """.formatted(uniqueReg);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("DOCTOR"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("should return 200 and JWT on valid credentials")
        void shouldLoginSuccessfully() throws Exception {
            LoginRequest req = new LoginRequest("IT_ADMIN", "Admin@12345");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isString())
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andExpect(jsonPath("$.registrationNumber").value("IT_ADMIN"));
        }

        @Test
        @DisplayName("should return 401 on incorrect password")
        void shouldReturn401OnWrongPassword() throws Exception {
            LoginRequest req = new LoginRequest("IT_ADMIN", "WrongPass!");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("should return 401 on non-existent registration number")
        void shouldReturn401OnNonExistentUser() throws Exception {
            LoginRequest req = new LoginRequest("GHOST999", "SomePassword@1");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 400 when login body is missing fields")
        void shouldReturn400OnMissingBody() throws Exception {
            String body = """
                    {"registrationNumber":""}
                    """;

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class Me {

        @Test
        @DisplayName("should return 200 with profile for authenticated user")
        void shouldReturnProfileForAuthenticatedUser() throws Exception {
            // Login to get token
            LoginRequest loginReq = new LoginRequest("IT_ADMIN", "Admin@12345");
            String response = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andReturn().getResponse().getContentAsString();
            String token = objectMapper.readTree(response).get("token").asText();

            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.registrationNumber").value("IT_ADMIN"))
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        @DisplayName("should return 401 when no token is provided")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 401 when token is malformed")
        void shouldReturn401ForMalformedToken() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
