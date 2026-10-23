package Prescription_Management.Repository;

import Prescription_Management.Entity.Record;
import Prescription_Management.Entity.RecordEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordEntryRepository extends JpaRepository<RecordEntry, Long> {
    Page<RecordEntry> findByRecordId(long recordId, Pageable pageable);
}
