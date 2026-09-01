package it.brunasti.mitire.backend.web.dto;

import it.brunasti.mitire.backend.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank String fullName,
        @NotBlank String email,
        @NotBlank String password,
        @NotNull Role role,
        Long groupId
) {
}
