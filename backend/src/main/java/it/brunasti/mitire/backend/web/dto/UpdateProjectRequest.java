package it.brunasti.mitire.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UpdateProjectRequest(@NotBlank String name, boolean active, LocalDate startDate, LocalDate endDate,
                                    Long approverId, Long ownerId) {
}
