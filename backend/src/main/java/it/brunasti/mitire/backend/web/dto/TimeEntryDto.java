package it.brunasti.mitire.backend.web.dto;

import it.brunasti.mitire.backend.domain.TimeEntryStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimeEntryDto(
        Long id,
        Long userId,
        String username,
        String userFullName,
        Long projectId,
        String projectCode,
        LocalDate workDate,
        BigDecimal hours,
        String description,
        TimeEntryStatus status
) {
}
