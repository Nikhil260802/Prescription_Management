package Prescription_Management.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "record_entries",
        indexes = {
                @Index(name = "idx_entries_record_id", columnList = "record_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RecordEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Parent record is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    private Record record;

    @NotBlank(message = "Entry name is required")
    @Size(max = 200, message = "Entry name must not exceed 200 characters")
    @Column(name = "entry_name", nullable = false, length = 200)
    private String entryName;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    @Column(nullable = false)
    private Integer quantity;

    @NotBlank(message = "Frequency is required")
    @Size(max = 100, message = "Frequency must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String frequency;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be greater than zero")
    @Column(nullable = false)
    private Integer duration;

    @Size(max = 1000, message = "Instructions must not exceed 1000 characters")
    @Column(length = 1000)
    private String instructions;
}