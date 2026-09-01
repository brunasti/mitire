package it.brunasti.mitire.backend.repository;

import it.brunasti.mitire.backend.domain.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {

    boolean existsByName(String name);
}
