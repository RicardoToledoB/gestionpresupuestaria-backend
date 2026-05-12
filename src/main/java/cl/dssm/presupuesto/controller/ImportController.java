package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.ImportResultDto;
import cl.dssm.presupuesto.entity.ExcelImport;
import cl.dssm.presupuesto.entity.ExcelImportError;
import cl.dssm.presupuesto.repository.*;
import cl.dssm.presupuesto.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/imports")
@RequiredArgsConstructor
public class ImportController {
    private final ExcelImportService service;
    private final ExcelImportRepository importRepository;
    private final ExcelImportErrorRepository errorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final CdpRepository cdpRepository;
    private final BudgetMovementRepository budgetMovementRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final ProviderRepository providerRepository;
    private final BudgetProgramRepository budgetProgramRepository;
    private final AuditLogRepository auditLogRepository;

    @PostMapping("/excel")
    public ImportResultDto importExcel(@RequestParam("file") MultipartFile file) {
        return service.importWorkbook(file);
    }

    @GetMapping
    public List<ExcelImport> imports() { return importRepository.findAll(); }

    @GetMapping("/{id}/errors")
    public List<ExcelImportError> errors(@PathVariable Long id) { return errorRepository.findByExcelImportId(id); }

    @DeleteMapping("/database")
    @Transactional
    public Map<String, String> clearDatabase() {
        auditLogRepository.deleteAllInBatch();
        purchaseOrderRepository.deleteAllInBatch();
        cdpRepository.deleteAllInBatch();
        budgetMovementRepository.deleteAllInBatch();
        budgetItemRepository.deleteAllInBatch();
        providerRepository.deleteAllInBatch();
        budgetProgramRepository.deleteAllInBatch();
        errorRepository.deleteAllInBatch();
        importRepository.deleteAllInBatch();
        return Map.of("status", "OK", "message", "Base H2 limpiada correctamente");
    }
}
