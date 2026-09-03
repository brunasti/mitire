package it.brunasti.mitire.backend.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
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
        Long statusId,
        String statusName,
        Instant createdAt
) {
}
