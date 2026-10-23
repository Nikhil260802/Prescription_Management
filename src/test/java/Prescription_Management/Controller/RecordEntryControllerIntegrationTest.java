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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DisplayName("RecordEntryController Integration Tests")
class RecordEntryControllerIntegrationTest {

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
    private Long draftRecordId;
    private Long issuedRecordId;
    private Long entryId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Seed admin
        User admin = userRepository.findByRegistrationNumber("REC_ADMIN").orElseGet(() -> {
            User u = new User();
            u.setName("REC Admin"); u.setRegistrationNumber("REC_ADMIN");
            u.setPhoneNumber("+4100000000"); u.setPassword(passwordEncoder.encode("Admin@12345"));
            u.setRole(Role.ADMIN);
            return userRepository.save(u);
        });

        // Seed doctor
        User doctor = userRepository.findByRegistrationNumber("REC_DOC").orElseGet(() -> {
            User u = new User();
            u.setName("REC Doctor"); u.setRegistrationNumber("REC_DOC");
            u.setPhoneNumber("+4200000000"); u.setPassword(passwordEncoder.encode("Doctor@123"));
            u.setRole(Role.DOCTOR);
            return userRepository.save(u);
        });

        // Seed client
        Client client = new Client();
        client.setName("REC Patient");
        client.setPhoneNumber("+4300000000");
        Client savedClient = clientRepository.save(client);

        adminToken = jwtService.generateToken(new CustomUserDetails(admin), admin.getId(), admin.getRole().name(), admin.getName());
        doctorToken = jwtService.generateToken(new CustomUserDetails(doctor), doctor.getId(), doctor.getRole().name(), doctor.getName());

        // Create a draft record with one entry
        draftRecordId = createRecord(doctor.getId(), savedClient.getId(), RecordStatus.DRAFT);
        issuedRecordId = createRecord(doctor.getId(), savedClient.getId(), RecordStatus.ISSUED);

        // Grab the entry id from the draft record
        String response = mockMvc.perform(get("/api/records/" + draftRecordId + "/entries")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getContentAsString();
        entryId = objectMapper.readTree(response).get("content").get(0).get("id").asLong();
    }

    private String obtainToken(String regNum, String password) throws Exception {
        LoginRequest login = new LoginRequest(regNum, password);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private Long createRecord(Long userId, Long clientId, RecordStatus status) throws Exception {
        String body = """
                {
                  "userId": %d, "clientId": %d,
                  "recordDate": "%s", "diagnosis": "Test Diagnosis",
                  "status": "%s",
                  "entries": [{"entryName":"Paracetamol","quantity":2,"frequency":"Twice daily","duration":5}]
                }
                """.formatted(userId, clientId, LocalDate.now(), status.name());

        String response = mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Nested
    @DisplayName("POST /api/records/{recordId}/entries")
    class CreateEntry {

        @Test
        @DisplayName("Should add entry to DRAFT record (201)")
        void shouldAddEntryToDraftRecord() throws Exception {
            String body = """
                    {"entryName":"Ibuprofen","quantity":1,"frequency":"Once daily","duration":3}
                    """;

            mockMvc.perform(post("/api/records/" + draftRecordId + "/entries")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.entryName").value("Ibuprofen"));
        }

        @Test
        @DisplayName("Should return 409 when adding entry to ISSUED record")
        void shouldReturn409ForIssuedRecord() throws Exception {
            String body = """
                    {"entryName":"Aspirin","quantity":1,"frequency":"Once daily","duration":3}
                    """;

            mockMvc.perform(post("/api/records/" + issuedRecordId + "/entries")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Missing required fields should return 400")
        void missingFieldsReturn400() throws Exception {
            String body = """
                    {"entryName":"Only Name"}
                    """;

            mockMvc.perform(post("/api/records/" + draftRecordId + "/entries")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Unauthenticated request returns 401")
        void unauthenticatedReturns401() throws Exception {
            String body = """
                    {"entryName":"Med","quantity":1,"frequency":"Daily","duration":1}
                    """;

            mockMvc.perform(post("/api/records/" + draftRecordId + "/entries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/records/{recordId}/entries")
    class GetEntries {

        @Test
        @DisplayName("Should return paginated entries list (200)")
        void shouldReturnEntriesList() throws Exception {
            mockMvc.perform(get("/api/records/" + draftRecordId + "/entries")
                            .header("Authorization", "Bearer " + doctorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("GET /entries/{entryId} returns entry (200)")
        void shouldReturnEntryById() throws Exception {
            mockMvc.perform(get("/api/records/" + draftRecordId + "/entries/" + entryId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(entryId));
        }

        @Test
        @DisplayName("GET /entries/{entryId} returns 404 for non-existent entry")
        void shouldReturn404ForNonExistentEntry() throws Exception {
            mockMvc.perform(get("/api/records/" + draftRecordId + "/entries/999999")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/records/{recordId}/entries/{entryId}")
    class UpdateEntry {

        @Test
        @DisplayName("Should update entry on DRAFT record (200)")
        void shouldUpdateEntrySuccessfully() throws Exception {
            String body = """
                    {"entryName":"Updated Paracetamol","quantity":3,"frequency":"Three times daily","duration":10}
                    """;

            mockMvc.perform(put("/api/records/" + draftRecordId + "/entries/" + entryId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.entryName").value("Updated Paracetamol"))
                    .andExpect(jsonPath("$.quantity").value(3));
        }

        @Test
        @DisplayName("Should return 409 when updating entry on ISSUED record")
        void shouldReturn409ForIssuedRecord() throws Exception {
            // Get entry from issued record
            String response = mockMvc.perform(get("/api/records/" + issuedRecordId + "/entries")
                            .header("Authorization", "Bearer " + adminToken))
                    .andReturn().getResponse().getContentAsString();
            Long issuedEntryId = objectMapper.readTree(response).get("content").get(0).get("id").asLong();

            String body = """
                    {"entryName":"Blocked","quantity":1,"frequency":"Daily","duration":1}
                    """;

            mockMvc.perform(put("/api/records/" + issuedRecordId + "/entries/" + issuedEntryId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/records/{recordId}/entries/{entryId}")
    class DeleteEntry {

        @Test
        @DisplayName("Should return 409 when deleting the only entry")
        void shouldReturn409WhenDeletingLastEntry() throws Exception {
            mockMvc.perform(delete("/api/records/" + draftRecordId + "/entries/" + entryId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should delete entry when record has more than one (204)")
        void shouldDeleteEntryWhenMultipleExist() throws Exception {
            // Add a second entry first
            String addBody = """
                    {"entryName":"Extra Med","quantity":1,"frequency":"Daily","duration":2}
                    """;
            mockMvc.perform(post("/api/records/" + draftRecordId + "/entries")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addBody))
                    .andExpect(status().isCreated());

            // Now delete the first entry
            mockMvc.perform(delete("/api/records/" + draftRecordId + "/entries/" + entryId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 409 when deleting entry on ISSUED record")
        void shouldReturn409ForIssuedRecord() throws Exception {
            String response = mockMvc.perform(get("/api/records/" + issuedRecordId + "/entries")
                            .header("Authorization", "Bearer " + adminToken))
                    .andReturn().getResponse().getContentAsString();
            Long issuedEntryId = objectMapper.readTree(response).get("content").get(0).get("id").asLong();

            mockMvc.perform(delete("/api/records/" + issuedRecordId + "/entries/" + issuedEntryId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isConflict());
        }
    }
}
