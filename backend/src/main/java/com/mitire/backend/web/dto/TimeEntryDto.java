package com.mitire.backend.web.dto;

import com.mitire.backend.domain.TimeEntryStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimeEntryDto(
        Long id,
        Long userId,
        String username,
        Long projectId,
        String projectCode,
        LocalDate workDate,
        BigDecimal hours,
        String description,
        TimeEntryStatus status
) {
}
