package it.brunasti.mitire.backend.repository;

import it.brunasti.mitire.backend.domain.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectStatusRepository extends JpaRepository<ProjectStatus, Long> {

    List<ProjectStatus> findByProjectIdOrderBySequence(Long projectId);

    Optional<ProjectStatus> findFirstByProjectIdOrderBySequence(Long projectId);

    boolean existsByProjectIdAndName(Long projectId, String name);
}
