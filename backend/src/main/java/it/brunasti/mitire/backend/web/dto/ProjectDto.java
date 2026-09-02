package it.brunasti.mitire.backend.web.dto;

import java.time.LocalDate;

public record ProjectDto(Long id, String code, String name, boolean active, LocalDate startDate, LocalDate endDate,
                          Long approverId, String approverFullName) {
}
