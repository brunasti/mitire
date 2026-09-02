package it.brunasti.mitire.backend.repository;

import it.brunasti.mitire.backend.domain.ProjectEntityStatusTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectEntityStatusTransitionRepository extends JpaRepository<ProjectEntityStatusTransition, Long> {

    List<ProjectEntityStatusTransition> findByParentStatusId(Long parentStatusId);

    List<ProjectEntityStatusTransition> findByChildStatusId(Long childStatusId);

    Optional<ProjectEntityStatusTransition> findByParentStatusIdAndChildStatusId(Long parentStatusId, Long childStatusId);
}
