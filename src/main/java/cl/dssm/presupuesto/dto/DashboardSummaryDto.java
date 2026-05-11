package cl.dssm.presupuesto.dto;

import java.math.BigDecimal;

public record DashboardSummaryDto(
        BigDecimal totalCurrentBudget,
        BigDecimal totalCdpIssued,
        BigDecimal totalExecutedByOc,
        BigDecimal totalAvailableBalance,
        BigDecimal totalPendingCdpBalance,
        BigDecimal totalPossibleRelease,
        long cdpNormal,
        long cdpFollowUp,
        long cdpAlert,
        long cdpReviewRelease,
        long cdpClosed
) {}
