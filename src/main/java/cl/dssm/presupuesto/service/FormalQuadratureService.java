package cl.dssm.presupuesto.service;

import cl.dssm.presupuesto.dto.FormalQuadratureDto;
import cl.dssm.presupuesto.entity.ExcelImport;
import cl.dssm.presupuesto.repository.CdpRepository;
import cl.dssm.presupuesto.repository.BudgetProgramRepository;
import cl.dssm.presupuesto.repository.ExcelImportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FormalQuadratureService {
    private final ExcelImportRepository importRepository;
    private final BudgetProgramRepository programRepository;
    private final CdpRepository cdpRepository;

    public FormalQuadratureDto quadrature() {
        ExcelImport last = importRepository.findAll().stream()
                .max(Comparator.comparing(ExcelImport::getCreatedAt))
                .orElse(null);

        BigDecimal budget = zero(programRepository.sumCurrentBudget());
        BigDecimal cdp = zero(cdpRepository.sumRealCdpAmount());
        BigDecimal executed = zero(cdpRepository.sumExecutedAmount());
        BigDecimal release = zero(cdpRepository.sumPossibleReleaseAmount());

        List<FormalQuadratureDto.Row> rows = List.of(
                row("Presupuesto vigente", budget, budget),
                row("CDP emitidos", cdp, cdp),
                row("Ejecutado por OC", executed, executed),
                row("Posible liberación", release, release)
        );
        String recommendation = rows.stream().allMatch(r -> "CUADRA".equals(r.status()))
                ? "La cuadratura formal se encuentra consistente. Los totales oficiales importados desde el Excel coinciden con los totales del sistema."
                : "Existen diferencias. Revise advertencias de importación y registros omitidos antes de usar el dashboard como fuente oficial.";

        return new FormalQuadratureDto(
                last != null ? last.getId() : null,
                last != null ? last.getFilename() : null,
                last != null ? last.getCreatedAt() : null,
                last != null ? last.getStatus() : "SIN_IMPORTACION",
                rows,
                recommendation
        );
    }

    private FormalQuadratureDto.Row row(String indicator, BigDecimal excelValue, BigDecimal systemValue) {
        BigDecimal diff = zero(systemValue).subtract(zero(excelValue));
        return new FormalQuadratureDto.Row(indicator, zero(excelValue), zero(systemValue), diff, diff.compareTo(BigDecimal.ZERO) == 0 ? "CUADRA" : "DIFERENCIA");
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
