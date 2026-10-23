package Prescription_Management.Entity;

import jakarta.persistence.*;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "Record")
@Table(
        name = "records",
        indexes = {
                @Index(name = "idx_records_user_id", columnList = "user_id"),
                @Index(name = "idx_records_client_id", columnList = "client_id"),
                @Index(name = "idx_records_record_date", columnList = "record_date"),
                @Index(name = "idx_records_idempotency_key", columnList = "idempotency_key", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", length = 120, unique = true)
    private String idempotencyKey;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Client is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @NotNull(message = "Record date is required")
    @PastOrPresent(message = "Record date cannot be in the future")
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @NotBlank(message = "Diagnosis is required")
    @Size(max = 1000, message = "Diagnosis must not exceed 1000 characters")
    @Column(nullable = false, length = 1000)
    private String diagnosis;

    @NotNull(message = "Record status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordStatus status = RecordStatus.DRAFT;

    @Valid
    @NotEmpty(message = "A record must contain at least one entry")
    @OneToMany(
            mappedBy = "record",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<RecordEntry> entries = new ArrayList<>();

    public void addEntry(RecordEntry entry) {
        entries.add(entry);
        entry.setRecord(this);
    }

    public void removeEntry(RecordEntry entry) {
        entries.remove(entry);
        entry.setRecord(null);
    }

}
