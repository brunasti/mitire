package it.brunasti.mitire.backend.repository;

import it.brunasti.mitire.backend.domain.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long>, JpaSpecificationExecutor<TimeEntry> {

    List<TimeEntry> findByUserIdAndWorkDate(Long userId, LocalDate workDate);
}
