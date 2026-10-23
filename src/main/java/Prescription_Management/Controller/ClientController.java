package Prescription_Management.Controller;

import Prescription_Management.DTO.RequestDto.ClientRequest;
import Prescription_Management.DTO.ResponseDto.ClientResponse;
import Prescription_Management.Service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
@Tag(name = "3. Client Management", description = "Endpoints for managing patients/clients (Doctor and Admin)")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @PostMapping
    @Operation(summary = "Create client", description = "Registers a new patient/client in the system.")
    public ResponseEntity<ClientResponse> createClient(
            @Valid @RequestBody ClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientService.createClient(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get client by ID", description = "Retrieves client details by their database ID.")
    public ResponseEntity<ClientResponse> getClientById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @GetMapping
    @Operation(summary = "List all clients", description = "Returns a paginated list of all clients.")
    public ResponseEntity<Page<ClientResponse>> getAllClients(
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "id",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {
        return ResponseEntity.ok(clientService.getAllClients(pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update client", description = "Updates patient/client personal and contact details.")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequest request) {
        return ResponseEntity.ok(clientService.updateClient(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete client", description = "Deletes a client if they have no associated prescription records.")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
