package it.brunasti.mitire.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project_entry_status")
@Getter
@Setter
@NoArgsConstructor
public class ProjectEntryStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "starting_status", nullable = false)
    private boolean startingStatus = false;

    @Column(columnDefinition = "text")
    private String description;
}
