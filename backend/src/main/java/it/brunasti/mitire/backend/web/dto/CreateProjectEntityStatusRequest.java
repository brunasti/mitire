package it.brunasti.mitire.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectEntityStatusRequest(@NotBlank String name, String description) {
}
