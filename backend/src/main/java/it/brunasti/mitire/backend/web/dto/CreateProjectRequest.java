package it.brunasti.mitire.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateProjectRequest(@NotBlank String code, @NotBlank String name,
                                    LocalDate startDate, LocalDate endDate) {
}
