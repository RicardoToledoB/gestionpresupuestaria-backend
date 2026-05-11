package cl.dssm.presupuesto.dto;

import cl.dssm.presupuesto.enums.PurchaseOrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseOrderRequestDto(
        @NotBlank String orderNumber,
        LocalDate orderDate,
        String sigfeFolio,
        @NotNull Long programId,
        Long providerId,
        Long budgetItemId,
        Long cdpId,
        BigDecimal committedAmount,
        BigDecimal adjustmentAmount,
        BigDecimal realAmount,
        PurchaseOrderStatus status,
        String observation,
        String subtitle,
        Boolean active
) {}
