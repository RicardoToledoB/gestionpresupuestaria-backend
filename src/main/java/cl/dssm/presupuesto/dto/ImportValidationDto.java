package cl.dssm.presupuesto.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ImportValidationDto(
        Long lastImportId,
        String lastImportFilename,
        String lastImportStatus,
        LocalDateTime lastImportDate,
        Integer programsDetectedInImport,
        Integer movementsDetectedInImport,
        Integer cdpDetectedInImport,
        Integer purchaseOrdersDetectedInImport,
        Integer errorsDetectedInImport,
        long programsInSystem,
        long providersInSystem,
        long budgetItemsInSystem,
        long cdpInSystem,
        long purchaseOrdersInSystem,
        long cdpWithoutProgram,
        long cdpWithoutProvider,
        long purchaseOrdersWithoutCdp,
        BigDecimal totalCurrentBudget,
        BigDecimal totalCdpIssued,
        BigDecimal totalExecutedByPurchaseOrders,
        BigDecimal totalPendingCdpBalance,
        BigDecimal totalPossibleRelease,
        String status,
        String recommendation
) {}
