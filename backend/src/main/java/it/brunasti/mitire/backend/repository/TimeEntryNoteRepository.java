package it.brunasti.mitire.backend.repository;

import it.brunasti.mitire.backend.domain.TimeEntryNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeEntryNoteRepository extends JpaRepository<TimeEntryNote, Long> {

    List<TimeEntryNote> findByTimeEntryIdOrderByCreatedAtDesc(Long timeEntryId);
}
