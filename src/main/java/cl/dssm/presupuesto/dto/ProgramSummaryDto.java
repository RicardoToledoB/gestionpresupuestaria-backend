package cl.dssm.presupuesto.dto;

import java.math.BigDecimal;

public record ProgramSummaryDto(
        Long programId,
        String programName,
        BigDecimal currentBudget,
        BigDecimal cdpIssued,
        BigDecimal availableBalance,
        BigDecimal commitmentPercent
) {}
