package it.brunasti.mitire.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateGroupRequest(@NotBlank String name, List<Long> projectIds) {
}
