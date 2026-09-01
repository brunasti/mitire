package it.brunasti.mitire.backend.web.dto;

import it.brunasti.mitire.backend.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
        @NotBlank String fullName,
        @NotBlank String email,
        @NotNull Role role,
        Long groupId,
        boolean enabled
) {
}
