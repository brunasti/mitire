package it.brunasti.mitire.backend.web.dto;

import java.time.Instant;

public record TimeEntryTransitionDto(
        Long id,
        String oldStatusName,
        String newStatusName,
        Long changedByUserId,
        String changedByFullName,
        Instant createdAt
) {
}
