package cl.dssm.presupuesto.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardChartsDto(
        List<ProgramExecutionDto> programs,
        List<StatusCountDto> cdpByStatus,
        List<TopCdpDto> topPendingCdps,
        List<KpiSliceDto> executionSlices
) {
    public record ProgramExecutionDto(String program, BigDecimal budget, BigDecimal issued, BigDecimal available, BigDecimal committedPercent) {}
    public record StatusCountDto(String status, long count) {}
    public record TopCdpDto(String cdpNumber, String description, BigDecimal pendingBalance, String alertStatus) {}
    public record KpiSliceDto(String label, BigDecimal value) {}
}
