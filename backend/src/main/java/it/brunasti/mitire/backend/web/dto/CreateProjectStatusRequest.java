package it.brunasti.mitire.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectStatusRequest(@NotBlank String name) {
}
