package it.brunasti.mitire.backend.web.dto;

import it.brunasti.mitire.backend.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateGroupRequest(@NotBlank String name, @NotNull Role role, List<Long> projectIds) {
}
