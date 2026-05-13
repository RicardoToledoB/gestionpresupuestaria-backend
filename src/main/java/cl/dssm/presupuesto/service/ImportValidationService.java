package cl.dssm.presupuesto.service;

import cl.dssm.presupuesto.dto.ImportValidationDto;
import cl.dssm.presupuesto.entity.ExcelImport;
import cl.dssm.presupuesto.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class ImportValidationService {
    private final ExcelImportRepository importRepository;
    private final BudgetProgramRepository programRepository;
    private final ProviderRepository providerRepository;
    private final BudgetItemRepository itemRepository;
    private final CdpRepository cdpRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public ImportValidationDto validate() {
        ExcelImport last = importRepository.findAll().stream()
                .max(Comparator.comparing(ExcelImport::getCreatedAt))
                .orElse(null);
        long cdpWithoutProvider = cdpRepository.countWithoutProvider();
        long poWithoutCdp = purchaseOrderRepository.countWithoutCdp();
        long cdpInSystem = cdpRepository.countActive();
        long poInSystem = purchaseOrderRepository.countActive();
        int warnings = (int) ((last != null ? last.getErrorsDetected() : 0) + cdpWithoutProvider + poWithoutCdp);
        String status = warnings == 0 ? "OK" : "REVISAR";
        String recommendation = warnings == 0
                ? "La importación se encuentra consistente a nivel general. Puede continuar con la operación diaria."
                : "Revise advertencias de importación, CDP sin proveedor u OC sin CDP asociado antes de utilizar el dashboard como fuente oficial.";

        return new ImportValidationDto(
                last != null ? last.getId() : null,
                last != null ? last.getFilename() : null,
                last != null ? last.getStatus() : "SIN_IMPORTACION",
                last != null ? last.getCreatedAt() : null,
                last != null ? last.getProgramsDetected() : 0,
                last != null ? last.getMovementsDetected() : 0,
                last != null ? last.getCdpDetected() : 0,
                last != null ? last.getPurchaseOrdersDetected() : 0,
                last != null ? last.getErrorsDetected() : 0,
                programRepository.count(),
                providerRepository.count(),
                itemRepository.count(),
                cdpInSystem,
                poInSystem,
                cdpRepository.countWithoutProgram(),
                cdpWithoutProvider,
                poWithoutCdp,
                zero(programRepository.sumCurrentBudget()),
                zero(cdpRepository.sumRealCdpAmount()),
                zero(cdpRepository.sumExecutedAmount()),
                zero(cdpRepository.sumPendingBalance()),
                zero(cdpRepository.sumPossibleReleaseAmount()),
                status,
                recommendation
        );
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
