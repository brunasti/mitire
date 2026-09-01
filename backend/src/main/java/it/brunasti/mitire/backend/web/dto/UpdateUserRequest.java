package it.brunasti.mitire.backend.web.dto;

import it.brunasti.mitire.backend.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateUserRequest(
        @NotBlank String fullName,
        @NotBlank String email,
        @NotNull Role role,
        List<Long> groupIds,
        boolean enabled
) {
}
