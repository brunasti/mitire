package it.brunasti.mitire.backend.repository;

import it.brunasti.mitire.backend.domain.ProjectEntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectEntityStatusRepository extends JpaRepository<ProjectEntityStatus, Long> {

    List<ProjectEntityStatus> findByProjectIdOrderBySequence(Long projectId);

    Optional<ProjectEntityStatus> findByProjectIdAndStartingStatusTrue(Long projectId);

    boolean existsByProjectIdAndName(Long projectId, String name);
}
