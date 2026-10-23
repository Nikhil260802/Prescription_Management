package Prescription_Management.Controller;

import Prescription_Management.DTO.RequestDto.LoginRequest;
import Prescription_Management.Entity.*;
import Prescription_Management.Repository.ClientRepository;
import Prescription_Management.Repository.RecordRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DisplayName("RecordController Integration Tests")
class RecordControllerIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private RecordRepository recordRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private MockMvc mockMvc;
    private String adminToken;
    private String doctorToken;
    private Long doctorUserId;
    private Long clientId;
    private Long draftRecordId;
    private Long issuedRecordId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Seed admin
        User admin = userRepository.findByRegistrationNumber("RC_ADMIN").orElseGet(() -> {
            User u = new User();
            u.setName("RC Admin"); u.setRegistrationNumber("RC_ADMIN");
            u.setPhoneNumber("+3100000000"); u.setPassword(passwordEncoder.encode("Admin@12345"));
            u.setRole(Role.ADMIN);
            return userRepository.save(u);
        });

        // Seed doctor
        User doctor = userRepository.findByRegistrationNumber("RC_DOC").orElseGet(() -> {
            User u = new User();
            u.setName("RC Doctor"); u.setRegistrationNumber("RC_DOC");
            u.setPhoneNumber("+3200000000"); u.setPassword(passwordEncoder.encode("Doctor@123"));
            u.setRole(Role.DOCTOR);
            return userRepository.save(u);
        });
        doctorUserId = doctor.getId();

        // Seed client
        Client client = new Client();
        client.setName("RC Test Patient");
        client.setPhoneNumber("+3300000000");
        client.setDateOfBirth(LocalDate.of(1985, 3, 10));
        Client savedClient = clientRepository.save(client);
        clientId = savedClient.getId();

        // Generate tokens
        adminToken = jwtService.generateToken(new CustomUserDetails(admin));
        doctorToken = jwtService.generateToken(new CustomUserDetails(doctor));

        // Create initial draft record
        draftRecordId = createRecordViaApi(RecordStatus.DRAFT);
        // Create initial issued record
        issuedRecordId = createRecordViaApi(RecordStatus.ISSUED);
    }

    private Long createRecordViaApi(RecordStatus status) throws Exception {
        String body = """
                {
                  "userId": %d,
                  "clientId": %d,
                  "recordDate": "%s",
                  "diagnosis": "Test Diagnosis",
                  "status": "%s",
                  "entries": [
                    {
                      "entryName": "Amoxicillin",
                      "quantity": 2,
                      "frequency": "Three times daily",
                      "duration": 5,
                      "instructions": "After meals"
                    }
                  ]
                }
                """.formatted(doctorUserId, clientId, LocalDate.now(), status.name());

        String response = mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Nested
    @DisplayName("POST /api/records")
    class CreateRecord {

        @Test
        @DisplayName("Doctor can create a record (201)")
        void doctorCanCreateRecord() throws Exception {
            String body = """
                    {
                      "userId": %d,
                      "clientId": %d,
                      "recordDate": "%s",
                      "diagnosis": "Flu Diagnosis",
                      "entries": [{"entryName":"Aspirin","quantity":1,"frequency":"Once daily","duration":3}]
                    }
                    """.formatted(doctorUserId, clientId, LocalDate.now());

            mockMvc.perform(post("/api/records")
                            .header("Authorization", "Bearer " + doctorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.diagnosis").value("Flu Diagnosis"))
                    .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @DisplayName("Duplicate request with same Idempotency-Key returns existing record without creating duplicate")
        void duplicateSubmissionWithIdempotencyKeyReturnsExistingRecord() throws Exception {
            String idempotencyKey = "TEST-IDEMP-" + System.currentTimeMillis();
            String body = """
                    {
                      "userId": %d,
                      "clientId": %d,
                      "recordDate": "%s",
                      "diagnosis": "Idempotent Diagnosis",
                      "entries": [{"entryName":"Paracetamol","quantity":10,"frequency":"Twice daily","duration":5}]
                    }
                    """.formatted(doctorUserId, clientId, LocalDate.now());

            // First submission
            String firstResponse = mockMvc.perform(post("/api/records")
                            .header("Authorization", "Bearer " + doctorToken)
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andReturn().getResponse().getContentAsString();

            Long firstId = objectMapper.readTree(firstResponse).get("id").asLong();

            // Second submission with identical Idempotency-Key
            String secondResponse = mockMvc.perform(post("/api/records")
                            .header("Authorization", "Bearer " + doctorToken)
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(firstId))
                    .andReturn().getResponse().getContentAsString();

            Long secondId = objectMapper.readTree(secondResponse).get("id").asLong();
            assertThat(firstId).isEqualTo(secondId);
        }

        @Test
        @DisplayName("Unauthenticated request returns 401")
        void unauthenticatedReturns401() throws Exception {
            String body = """
                    {
                      "userId": %d, "clientId": %d,
                      "recordDate": "%s", "diagnosis": "Test",
                      "entries": [{"entryName":"Med","quantity":1,"frequency":"Daily","duration":1}]
                    }
                    """.formatted(doctorUserId, clientId, LocalDate.now());

            mockMvc.perform(post("/api/records")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Missing entries should return 400")
        void missingEntriesReturns400() throws Exception {
            String body = """
                    {"userId": %d, "clientId": %d, "recordDate": "%s", "diagnosis": "Bad Record", "entries": []}
                    """.formatted(doctorUserId, clientId, LocalDate.now());

            mockMvc.perform(post("/api/records")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Non-existent userId should return 404")
        void nonExistentUserIdReturns404() throws Exception {
            String body = """
                    {
                      "userId": 999999, "clientId": %d,
                      "recordDate": "%s", "diagnosis": "Bad User",
                      "entries": [{"entryName":"Med","quantity":1,"frequency":"Daily","duration":1}]
                    }
                    """.formatted(clientId, LocalDate.now());

            mockMvc.perform(post("/api/records")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Future recordDate should return 400")
        void futureRecordDateReturns400() throws Exception {
            String body = """
                    {
                      "userId": %d, "clientId": %d,
                      "recordDate": "2099-01-01", "diagnosis": "Future",
                      "entries": [{"entryName":"Med","quantity":1,"frequency":"Daily","duration":1}]
                    }
                    """.formatted(doctorUserId, clientId);

            mockMvc.perform(post("/api/records")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/records")
    class GetRecords {

        @Test
        @DisplayName("Admin gets all records (200)")
        void adminGetsAllRecords() throws Exception {
            mockMvc.perform(get("/api/records")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("GET /api/records/{id} returns record (200)")
        void getRecordById() throws Exception {
            mockMvc.perform(get("/api/records/" + draftRecordId)
                            .header("Authorization", "Bearer " + doctorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(draftRecordId));
        }

        @Test
        @DisplayName("GET /api/records/{id} returns 404 for non-existent record")
        void getRecordByIdReturns404() throws Exception {
            mockMvc.perform(get("/api/records/999999")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/records/getRecordsByUser/{userId} returns records (200)")
        void getRecordsByUser() throws Exception {
            mockMvc.perform(get("/api/records/getRecordsByUser/" + doctorUserId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("GET /api/records/getRecordsByClient/{clientId} returns records (200)")
        void getRecordsByClient() throws Exception {
            mockMvc.perform(get("/api/records/getRecordsByClient/" + clientId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("PUT /api/records/{id}")
    class UpdateRecord {

        @Test
        @DisplayName("Admin can update a DRAFT record (200)")
        void adminCanUpdateDraftRecord() throws Exception {
            String body = """
                    {
                      "userId": %d, "clientId": %d,
                      "recordDate": "%s", "diagnosis": "Updated Diagnosis",
                      "status": "DRAFT",
                      "entries": [{"entryName":"Ibuprofen","quantity":1,"frequency":"Once daily","duration":7}]
                    }
                    """.formatted(doctorUserId, clientId, LocalDate.now());

            mockMvc.perform(put("/api/records/" + draftRecordId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.diagnosis").value("Updated Diagnosis"));
        }

        @Test
        @DisplayName("Updating an ISSUED record should return 409")
        void updatingIssuedRecordReturns409() throws Exception {
            String body = """
                    {
                      "userId": %d, "clientId": %d,
                      "recordDate": "%s", "diagnosis": "Attempt Edit",
                      "status": "ISSUED",
                      "entries": [{"entryName":"Med","quantity":1,"frequency":"Daily","duration":1}]
                    }
                    """.formatted(doctorUserId, clientId, LocalDate.now());

            mockMvc.perform(put("/api/records/" + issuedRecordId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/records/{id}")
    class DeleteRecord {

        @Test
        @DisplayName("Admin can delete a DRAFT record (204)")
        void adminCanDeleteDraftRecord() throws Exception {
            Long tempRecordId = createRecordViaApi(RecordStatus.DRAFT);

            mockMvc.perform(delete("/api/records/" + tempRecordId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Deleting an ISSUED record returns 409")
        void deletingIssuedRecordReturns409() throws Exception {
            mockMvc.perform(delete("/api/records/" + issuedRecordId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Deleting non-existent record returns 404")
        void deletingNonExistentRecordReturns404() throws Exception {
            mockMvc.perform(delete("/api/records/999999")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }
    }
}
