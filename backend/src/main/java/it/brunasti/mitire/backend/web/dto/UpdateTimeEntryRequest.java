package it.brunasti.mitire.backend.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateTimeEntryRequest(
        @NotNull @DecimalMin("0.25") @DecimalMax("24.0") BigDecimal hours,
        String description
) {
}
