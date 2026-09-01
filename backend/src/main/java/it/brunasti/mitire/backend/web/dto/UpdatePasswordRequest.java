package it.brunasti.mitire.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordRequest(@NotBlank String password) {
}
