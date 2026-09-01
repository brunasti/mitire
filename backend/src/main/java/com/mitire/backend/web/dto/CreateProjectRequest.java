package com.mitire.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(@NotBlank String code, @NotBlank String name) {
}
