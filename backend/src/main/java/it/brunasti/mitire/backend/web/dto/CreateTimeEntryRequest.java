package it.brunasti.mitire.backend.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTimeEntryRequest(
        @NotNull Long userId,
        @NotNull Long projectId,
        @NotNull @PastOrPresent LocalDate workDate,
        @NotNull @DecimalMin("0.25") @DecimalMax("24.0") BigDecimal hours,
        String description
) {
}
