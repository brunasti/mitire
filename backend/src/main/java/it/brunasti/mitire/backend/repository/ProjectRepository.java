package it.brunasti.mitire.backend.repository;

import it.brunasti.mitire.backend.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByCode(String code);

    boolean existsByCode(String code);

    List<Project> findByApproverId(Long approverId);

    List<Project> findByOwnerId(Long ownerId);
}
