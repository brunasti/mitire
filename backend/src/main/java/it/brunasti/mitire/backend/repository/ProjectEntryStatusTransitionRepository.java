package it.brunasti.mitire.backend.repository;

import it.brunasti.mitire.backend.domain.ProjectEntryStatusTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectEntryStatusTransitionRepository extends JpaRepository<ProjectEntryStatusTransition, Long> {

    List<ProjectEntryStatusTransition> findByParentStatusId(Long parentStatusId);

    List<ProjectEntryStatusTransition> findByChildStatusId(Long childStatusId);

    Optional<ProjectEntryStatusTransition> findByParentStatusIdAndChildStatusId(Long parentStatusId, Long childStatusId);
}
