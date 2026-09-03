package it.brunasti.mitire.backend.repository;

import it.brunasti.mitire.backend.domain.TimeEntryTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeEntryTransitionRepository extends JpaRepository<TimeEntryTransition, Long> {

    List<TimeEntryTransition> findByTimeEntryIdOrderByCreatedAtDesc(Long timeEntryId);
}
