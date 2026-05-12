package cl.dssm.presupuesto.dto;

import cl.dssm.presupuesto.entity.Cdp;
import cl.dssm.presupuesto.enums.AlertStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CdpResponseDto(
        Long id,
        String cdpNumber,
        LocalDate cdpDate,
        Long programId,
        String programName,
        Long providerId,
        String providerRut,
        String providerName,
        Long budgetItemId,
        String budgetItemCode,
        String budgetItemName,
        String cdpType,
        String tenderOrContract,
        String description,
        LocalDate coverageStart,
        LocalDate coverageEnd,
        BigDecimal coverageMonths,
        BigDecimal cdpAmount,
        BigDecimal cdpAdjustment,
        BigDecimal realCdpAmount,
        BigDecimal executedAmount,
        BigDecimal pendingBalance,
        BigDecimal executedPercent,
        BigDecimal expectedPercent,
        BigDecimal deviation,
        AlertStatus alertStatus,
        String suggestedAction,
        BigDecimal possibleReleaseAmount,
        String observation,
        Boolean active,
        java.time.LocalDateTime deletedAt
) {
    public static CdpResponseDto fromEntity(Cdp cdp) {
        return new CdpResponseDto(
                cdp.getId(),
                cdp.getCdpNumber(),
                cdp.getCdpDate(),
                cdp.getProgram() != null ? cdp.getProgram().getId() : null,
                cdp.getProgram() != null ? cdp.getProgram().getName() : null,
                cdp.getProvider() != null ? cdp.getProvider().getId() : null,
                cdp.getProvider() != null ? cdp.getProvider().getRut() : null,
                cdp.getProvider() != null ? cdp.getProvider().getBusinessName() : null,
                cdp.getBudgetItem() != null ? cdp.getBudgetItem().getId() : null,
                cdp.getBudgetItem() != null ? cdp.getBudgetItem().getCode() : null,
                cdp.getBudgetItem() != null ? cdp.getBudgetItem().getName() : null,
                cdp.getCdpType(),
                cdp.getTenderOrContract(),
                cdp.getDescription(),
                cdp.getCoverageStart(),
                cdp.getCoverageEnd(),
                cdp.getCoverageMonths(),
                cdp.getCdpAmount(),
                cdp.getCdpAdjustment(),
                cdp.getRealCdpAmount(),
                cdp.getExecutedAmount(),
                cdp.getPendingBalance(),
                cdp.getExecutedPercent(),
                cdp.getExpectedPercent(),
                cdp.getDeviation(),
                cdp.getAlertStatus(),
                cdp.getSuggestedAction(),
                cdp.getPossibleReleaseAmount(),
                cdp.getObservation(),
                cdp.getDeletedAt() == null,
                cdp.getDeletedAt()
        );
    }
}
