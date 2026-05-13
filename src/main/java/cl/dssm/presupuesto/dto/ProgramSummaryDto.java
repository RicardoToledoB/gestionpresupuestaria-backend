package cl.dssm.presupuesto.dto;

import java.math.BigDecimal;

public record ProgramSummaryDto(
        Long programId,
        String programName,
        String subtitle,
        BigDecimal currentBudget,
        BigDecimal cdpIssued,
        BigDecimal cdpPendingBalance,
        BigDecimal availableBalance,
        long cdpFollowUp,
        long cdpAlert,
        BigDecimal possibleRelease,
        BigDecimal commitmentPercent
) {}
