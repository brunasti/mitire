package it.brunasti.mitire.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProjectEntityStatusRequest(@NotBlank String name, String description, boolean active) {
}
