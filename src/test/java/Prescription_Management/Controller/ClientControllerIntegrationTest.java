package Prescription_Management.Controller;

import Prescription_Management.DTO.RequestDto.LoginRequest;
import Prescription_Management.DTO.RequestDto.RegisterRequest;
import Prescription_Management.Entity.Client;
import Prescription_Management.Entity.Role;
import Prescription_Management.Entity.User;
import Prescription_Management.Repository.ClientRepository;
import Prescription_Management.Repository.UserRepository;
import Prescription_Management.Security.CustomUserDetails;
import Prescription_Management.Security.JwtService;
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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DisplayName("ClientController Integration Tests")
class ClientControllerIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private MockMvc mockMvc;
    private String adminToken;
    private String doctorToken;
    private Long seededClientId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Seed admin
        User admin = userRepository.findByRegistrationNumber("CC_ADMIN").orElseGet(() -> {
            User u = new User();
            u.setName("CC Admin"); u.setRegistrationNumber("CC_ADMIN");
            u.setPhoneNumber("+1100000000"); u.setPassword(passwordEncoder.encode("Admin@12345"));
            u.setRole(Role.ADMIN);
            return userRepository.save(u);
        });

        // Seed doctor
        User doctor = userRepository.findByRegistrationNumber("CC_DOC").orElseGet(() -> {
            User u = new User();
            u.setName("CC Doctor"); u.setRegistrationNumber("CC_DOC");
            u.setPhoneNumber("+1200000000"); u.setPassword(passwordEncoder.encode("Doctor@123"));
            u.setRole(Role.DOCTOR);
            return userRepository.save(u);
        });

        // Seed a client for read/update/delete tests
        Client seededClient = clientRepository.save(buildClient("CC Seeded Patient", "+1300000000"));
        seededClientId = seededClient.getId();

        adminToken = jwtService.generateToken(new CustomUserDetails(admin), admin.getId(), admin.getRole().name(), admin.getName());
        doctorToken = jwtService.generateToken(new CustomUserDetails(doctor), doctor.getId(), doctor.getRole().name(), doctor.getName());
    }

    private String obtainToken(String regNum, String password) throws Exception {
        LoginRequest login = new LoginRequest(regNum, password);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private Client buildClient(String name, String phone) {
        Client c = new Client();
        c.setName(name);
        c.setPhoneNumber(phone);
        c.setDateOfBirth(LocalDate.of(1990, 1, 1));
        return c;
    }

    @Nested
    @DisplayName("POST /api/clients")
    class CreateClient {

        @Test
        @DisplayName("Admin should create a client (201)")
        void adminShouldCreateClient() throws Exception {
            String body = """
                    {"name":"New Patient","phoneNumber":"+1999888777","dateOfBirth":"1992-06-15"}
                    """;
            mockMvc.perform(post("/api/clients")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("New Patient"));
        }

        @Test
        @DisplayName("Doctor should also create a client (201)")
        void doctorShouldCreateClient() throws Exception {
            String body = """
                    {"name":"Another Patient","phoneNumber":"+1888777666"}
                    """;
            mockMvc.perform(post("/api/clients")
                            .header("Authorization", "Bearer " + doctorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Unauthenticated request should return 401")
        void unauthenticatedShouldReturn401() throws Exception {
            String body = """
                    {"name":"Anon Patient","phoneNumber":"+1777666555"}
                    """;
            mockMvc.perform(post("/api/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Missing required fields should return 400")
        void missingNameShouldReturn400() throws Exception {
            String body = """
                    {"phoneNumber":"+1777666555"}
                    """;
            mockMvc.perform(post("/api/clients")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/clients")
    class GetAllClients {

        @Test
        @DisplayName("Admin should get paginated clients list (200)")
        void adminShouldGetClients() throws Exception {
            mockMvc.perform(get("/api/clients")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("Doctor should get clients list (200)")
        void doctorShouldGetClients() throws Exception {
            mockMvc.perform(get("/api/clients")
                            .header("Authorization", "Bearer " + doctorToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Unauthenticated should return 401")
        void unauthenticatedShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/clients"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/clients/{id}")
    class GetClientById {

        @Test
        @DisplayName("Should return client details for valid ID (200)")
        void shouldReturnClientById() throws Exception {
            mockMvc.perform(get("/api/clients/" + seededClientId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(seededClientId));
        }

        @Test
        @DisplayName("Should return 404 for non-existent client")
        void shouldReturn404ForNonExistentClient() throws Exception {
            mockMvc.perform(get("/api/clients/999999")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/clients/{id}")
    class UpdateClient {

        @Test
        @DisplayName("Admin should update a client (200)")
        void adminShouldUpdateClient() throws Exception {
            String body = """
                    {"name":"Updated Name","phoneNumber":"+1300000000"}
                    """;
            mockMvc.perform(put("/api/clients/" + seededClientId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Name"));
        }

        @Test
        @DisplayName("Doctor should update a client (200)")
        void doctorShouldUpdateClient() throws Exception {
            String body = """
                    {"name":"Doctor Updated","phoneNumber":"+1300000000"}
                    """;
            mockMvc.perform(put("/api/clients/" + seededClientId)
                            .header("Authorization", "Bearer " + doctorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 404 for non-existent client")
        void shouldReturn404ForNonExistentId() throws Exception {
            String body = """
                    {"name":"Ghost","phoneNumber":"+1234567890"}
                    """;
            mockMvc.perform(put("/api/clients/999999")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/clients/{id}")
    class DeleteClient {

        @Test
        @DisplayName("Admin should delete a client with no records (204)")
        void adminShouldDeleteClient() throws Exception {
            Client toDelete = clientRepository.save(buildClient("To Delete Patient", "+1400000001"));

            mockMvc.perform(delete("/api/clients/" + toDelete.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 for non-existent client")
        void shouldReturn404WhenClientNotFound() throws Exception {
            mockMvc.perform(delete("/api/clients/999999")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }
    }
}
