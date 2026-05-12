package cl.dssm.presupuesto.service;

import cl.dssm.presupuesto.dto.DashboardChartsDto;
import cl.dssm.presupuesto.dto.DashboardSummaryDto;
import cl.dssm.presupuesto.dto.ProgramSummaryDto;
import cl.dssm.presupuesto.entity.BudgetProgram;
import cl.dssm.presupuesto.entity.Cdp;
import cl.dssm.presupuesto.enums.AlertStatus;
import cl.dssm.presupuesto.repository.BudgetProgramRepository;
import cl.dssm.presupuesto.repository.CdpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final BudgetProgramRepository programRepository;
    private final CdpRepository cdpRepository;

    public DashboardSummaryDto summary() {
        BigDecimal totalBudget = programRepository.sumCurrentBudget();
        BigDecimal totalCdp = cdpRepository.sumRealCdpAmount();
        BigDecimal totalExecuted = cdpRepository.sumExecutedAmount();
        BigDecimal available = totalBudget.subtract(totalCdp);

        return new DashboardSummaryDto(
                totalBudget,
                totalCdp,
                totalExecuted,
                available,
                cdpRepository.sumPendingBalance(),
                cdpRepository.sumPossibleReleaseAmount(),
                cdpRepository.countByAlertStatus(AlertStatus.NORMAL),
                cdpRepository.countByAlertStatus(AlertStatus.SEGUIMIENTO),
                cdpRepository.countByAlertStatus(AlertStatus.ALERTA),
                cdpRepository.countByAlertStatus(AlertStatus.REVISAR_REBAJA_CDP),
                cdpRepository.countByAlertStatus(AlertStatus.CERRADO)
        );
    }

    public List<ProgramSummaryDto> programs() {
        return programRepository.findAll().stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(p -> {
                    BigDecimal currentBudget = zero(p.getCurrentBudget());
                    BigDecimal issued = zero(cdpRepository.sumRealCdpAmountByProgramId(p.getId()));
                    BigDecimal pendingBalance = zero(cdpRepository.sumPendingBalanceByProgramIdExcludingDirectOc(p.getId()));
                    BigDecimal available = currentBudget.subtract(issued);
                    BigDecimal possibleRelease = zero(cdpRepository.sumPossibleReleaseAmountByProgramId(p.getId()));
                    long followUp = cdpRepository.countByProgramIdAndAlertStatus(p.getId(), AlertStatus.SEGUIMIENTO);
                    long alert = cdpRepository.countByProgramIdAndAlertStatusIn(p.getId(), List.of(AlertStatus.ALERTA, AlertStatus.REVISAR_REBAJA_CDP));
                    BigDecimal pct = percent(issued, currentBudget);
                    return new ProgramSummaryDto(
                            p.getId(),
                            p.getName(),
                            p.getSubtitle(),
                            currentBudget,
                            issued,
                            pendingBalance,
                            available,
                            followUp,
                            alert,
                            possibleRelease,
                            pct
                    );
                })
                .toList();
    }

    public DashboardChartsDto charts() {
        List<DashboardChartsDto.ProgramExecutionDto> programs = programRepository.findAll().stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(p -> {
                    BigDecimal budget = zero(p.getCurrentBudget());
                    BigDecimal issued = zero(cdpRepository.sumRealCdpAmountByProgramId(p.getId()));
                    BigDecimal available = budget.subtract(issued);
                    return new DashboardChartsDto.ProgramExecutionDto(p.getName(), budget, issued, available, percent(issued, budget));
                })
                .sorted((a, b) -> b.budget().compareTo(a.budget()))
                .limit(10)
                .toList();

        List<DashboardChartsDto.StatusCountDto> statuses = Arrays.stream(AlertStatus.values())
                .map(status -> new DashboardChartsDto.StatusCountDto(status.name(), cdpRepository.countByAlertStatus(status)))
                .toList();

        List<DashboardChartsDto.TopCdpDto> topPending = cdpRepository.findTopPending(PageRequest.of(0, 10)).stream()
                .map(c -> new DashboardChartsDto.TopCdpDto(c.getCdpNumber(), c.getDescription(), zero(c.getPendingBalance()), c.getAlertStatus().name()))
                .toList();

        DashboardSummaryDto s = summary();
        List<DashboardChartsDto.KpiSliceDto> slices = List.of(
                new DashboardChartsDto.KpiSliceDto("CDP emitidos", s.totalCdpIssued()),
                new DashboardChartsDto.KpiSliceDto("Saldo disponible", s.totalAvailableBalance()),
                new DashboardChartsDto.KpiSliceDto("Ejecutado por OC", s.totalExecutedByOc()),
                new DashboardChartsDto.KpiSliceDto("Saldo pendiente CDP", s.totalPendingCdpBalance())
        );
        return new DashboardChartsDto(programs, statuses, topPending, slices);
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return zero(numerator).multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
