package cl.dssm.presupuesto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CdpRequestDto(
        @NotBlank String cdpNumber,
        LocalDate cdpDate,
        @NotNull Long programId,
        Long providerId,
        Long budgetItemId,
        String cdpType,
        String tenderOrContract,
        String description,
        LocalDate coverageStart,
        LocalDate coverageEnd,
        BigDecimal coverageMonths,
        BigDecimal cdpAmount,
        BigDecimal cdpAdjustment,
        BigDecimal realCdpAmount,
        BigDecimal expectedPercent,
        String observation,
        Boolean active
) {}
