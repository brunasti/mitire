package it.brunasti.mitire.backend.repository;

import it.brunasti.mitire.backend.domain.ProjectEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectEntryStatusRepository extends JpaRepository<ProjectEntryStatus, Long> {

    List<ProjectEntryStatus> findByProjectIdOrderBySequence(Long projectId);

    Optional<ProjectEntryStatus> findByProjectIdAndStartingStatusTrue(Long projectId);

    boolean existsByProjectIdAndName(Long projectId, String name);
}
