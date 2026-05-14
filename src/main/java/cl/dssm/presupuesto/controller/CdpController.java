package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.AssociatedPurchaseOrderDto;
import cl.dssm.presupuesto.dto.CdpRequestDto;
import cl.dssm.presupuesto.dto.CdpResponseDto;
import cl.dssm.presupuesto.dto.PageResponseDto;
import cl.dssm.presupuesto.enums.AlertStatus;
import cl.dssm.presupuesto.repository.CdpRepository;
import cl.dssm.presupuesto.repository.PurchaseOrderRepository;
import cl.dssm.presupuesto.service.CdpManagementService;
import cl.dssm.presupuesto.service.CdpRecalculationService;
import cl.dssm.presupuesto.util.CsvExportUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cdps")
@RequiredArgsConstructor
public class CdpController {
    private final CdpRepository repository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final CdpManagementService service;
    private final CdpRecalculationService recalculationService;

    @GetMapping
    @Transactional(readOnly = true)
    public PageResponseDto<CdpResponseDto> findAll(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @PageableDefault(size = 20, sort = "cdpNumber") Pageable pageable
    ) {
        return PageResponseDto.from(repository.search(search.trim(), includeDeleted, pageable).map(CdpResponseDto::fromEntity));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CdpResponseDto findById(@PathVariable Long id) {
        return repository.findById(id).map(CdpResponseDto::fromEntity).orElseThrow();
    }

    @PostMapping
    public CdpResponseDto create(@RequestBody @Valid CdpRequestDto input) { return CdpResponseDto.fromEntity(service.create(input)); }

    @PutMapping("/{id}")
    public CdpResponseDto update(@PathVariable Long id, @RequestBody @Valid CdpRequestDto input) { return CdpResponseDto.fromEntity(service.update(id, input)); }

    @DeleteMapping("/{id}")
    public void softDelete(@PathVariable Long id) { service.softDelete(id); }

    @PostMapping("/{id}/restore")
    public CdpResponseDto restore(@PathVariable Long id) { return CdpResponseDto.fromEntity(service.restore(id)); }

    @PostMapping("/{id}/recalculate")
    public CdpResponseDto recalculate(@PathVariable Long id) { return CdpResponseDto.fromEntity(recalculationService.recalculate(id)); }

    @PostMapping("/recalculate-all")
    public ResponseEntity<?> recalculateAll() {
        recalculationService.recalculateAll();
        return ResponseEntity.ok(java.util.Map.of("status", "OK", "message", "CDP recalculados correctamente"));
    }


    @GetMapping("/{id}/purchase-orders")
    @Transactional(readOnly = true)
    public List<AssociatedPurchaseOrderDto> purchaseOrdersByCdp(@PathVariable Long id) {
        return purchaseOrderRepository.findByCdpIdAndDeletedAtIsNullOrderByOrderDateDescOrderNumberAsc(id)
                .stream()
                .map(AssociatedPurchaseOrderDto::fromEntity)
                .toList();
    }

    @GetMapping("/alerts")
    @Transactional(readOnly = true)
    public List<CdpResponseDto> alerts() {
        return repository.findTop20ByAlertStatusInOrderByPossibleReleaseAmountDesc(
                        List.of(AlertStatus.ALERTA, AlertStatus.SEGUIMIENTO, AlertStatus.REVISAR_REBAJA_CDP)
                ).stream()
                .filter(c -> c.getDeletedAt() == null)
                .map(CdpResponseDto::fromEntity)
                .toList();
    }

    @GetMapping("/export")
    @Transactional(readOnly = true)
    public ResponseEntity<String> export(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "false") boolean includeDeleted) {
        String header = "ID;CDP;Fecha;Programa;Proveedor;Item;Descripcion;MontoReal;Ejecutado;Saldo;%Ejecutado;Estado;AccionSugerida;Eliminado\n";
        String rows = repository.exportRows(search.trim(), includeDeleted).stream()
                .map(c -> String.join(";",
                        CsvExportUtils.cell(c.getId()), CsvExportUtils.cell(c.getCdpNumber()), CsvExportUtils.cell(c.getCdpDate()),
                        CsvExportUtils.cell(c.getProgram() != null ? c.getProgram().getName() : null),
                        CsvExportUtils.cell(c.getProvider() != null ? c.getProvider().getBusinessName() : null),
                        CsvExportUtils.cell(c.getBudgetItem() != null ? c.getBudgetItem().getCode() : null),
                        CsvExportUtils.cell(c.getDescription()), CsvExportUtils.cell(c.getRealCdpAmount()), CsvExportUtils.cell(c.getExecutedAmount()),
                        CsvExportUtils.cell(c.getPendingBalance()), CsvExportUtils.cell(c.getExecutedPercent()), CsvExportUtils.cell(c.getAlertStatus()),
                        CsvExportUtils.cell(c.getSuggestedAction()), CsvExportUtils.cell(c.getDeletedAt() != null)))
                .collect(Collectors.joining("\n"));
        return CsvExportUtils.response("cdp_filtrado.csv", header + rows);
    }
}
