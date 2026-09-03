package it.brunasti.mitire.backend.web.dto;

import java.time.Instant;

public record TimeEntryNoteDto(
        Long id,
        Long authorId,
        String authorFullName,
        String text,
        Instant createdAt
) {
}
