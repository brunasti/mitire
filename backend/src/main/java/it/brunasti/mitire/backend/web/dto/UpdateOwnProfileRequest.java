package it.brunasti.mitire.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateOwnProfileRequest(@NotBlank String fullName, @NotBlank String email) {
}
