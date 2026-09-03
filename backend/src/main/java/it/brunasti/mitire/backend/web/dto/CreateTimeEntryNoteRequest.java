package it.brunasti.mitire.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTimeEntryNoteRequest(@NotBlank String text) {
}
