package it.brunasti.mitire.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeOwnPasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
}
