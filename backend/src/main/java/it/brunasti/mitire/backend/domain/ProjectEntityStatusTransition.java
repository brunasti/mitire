package it.brunasti.mitire.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project_entity_status_transition")
@Getter
@Setter
@NoArgsConstructor
public class ProjectEntityStatusTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "parent_status_id", nullable = false)
    private ProjectEntityStatus parentStatus;

    @ManyToOne(optional = false)
    @JoinColumn(name = "child_status_id", nullable = false)
    private ProjectEntityStatus childStatus;
}
