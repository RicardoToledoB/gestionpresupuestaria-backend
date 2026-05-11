package cl.dssm.presupuesto.service;

import cl.dssm.presupuesto.dto.DashboardSummaryDto;
import cl.dssm.presupuesto.dto.ProgramSummaryDto;
import cl.dssm.presupuesto.entity.BudgetProgram;
import cl.dssm.presupuesto.enums.AlertStatus;
import cl.dssm.presupuesto.repository.BudgetProgramRepository;
import cl.dssm.presupuesto.repository.CdpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final BudgetProgramRepository programRepository;
    private final CdpRepository cdpRepository;

    public DashboardSummaryDto summary() {
        BigDecimal totalBudget = programRepository.findAll().stream()
                .map(BudgetProgram::getCurrentBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
                .map(p -> {
                    // MVP: se calcula en memoria para mantener el código simple. En producción conviene query agregada SQL.
                    BigDecimal issued = BigDecimal.ZERO;
                    BigDecimal available = p.getCurrentBudget().subtract(issued);
                    BigDecimal pct = p.getCurrentBudget().compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : issued.multiply(BigDecimal.valueOf(100)).divide(p.getCurrentBudget(), 2, RoundingMode.HALF_UP);
                    return new ProgramSummaryDto(p.getId(), p.getName(), p.getCurrentBudget(), issued, available, pct);
                })
                .toList();
    }
}
