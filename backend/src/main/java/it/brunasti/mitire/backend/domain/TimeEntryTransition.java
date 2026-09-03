package it.brunasti.mitire.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "time_entry_transition")
@Getter
@Setter
@NoArgsConstructor
public class TimeEntryTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "time_entry_id", nullable = false)
    private TimeEntry timeEntry;

    @ManyToOne(optional = false)
    @JoinColumn(name = "old_status_id", nullable = false)
    private ProjectEntryStatus oldStatus;

    @ManyToOne(optional = false)
    @JoinColumn(name = "new_status_id", nullable = false)
    private ProjectEntryStatus newStatus;

    @ManyToOne(optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
