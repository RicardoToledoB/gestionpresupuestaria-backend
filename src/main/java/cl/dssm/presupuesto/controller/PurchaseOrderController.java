package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.PageResponseDto;
import cl.dssm.presupuesto.dto.PurchaseOrderRequestDto;
import cl.dssm.presupuesto.dto.PurchaseOrderResponseDto;
import cl.dssm.presupuesto.repository.PurchaseOrderRepository;
import cl.dssm.presupuesto.service.PurchaseOrderManagementService;
import cl.dssm.presupuesto.util.CsvExportUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {
    private final PurchaseOrderRepository repository;
    private final PurchaseOrderManagementService service;

    @GetMapping
    @Transactional(readOnly = true)
    public PageResponseDto<PurchaseOrderResponseDto> findAll(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @PageableDefault(size = 20, sort = "orderDate") Pageable pageable
    ) {
        return PageResponseDto.from(repository.search(search.trim(), includeDeleted, pageable).map(PurchaseOrderResponseDto::fromEntity));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public PurchaseOrderResponseDto findById(@PathVariable Long id) {
        return repository.findById(id).map(PurchaseOrderResponseDto::fromEntity).orElseThrow();
    }

    @PostMapping
    public PurchaseOrderResponseDto create(@RequestBody @Valid PurchaseOrderRequestDto input) { return PurchaseOrderResponseDto.fromEntity(service.create(input)); }

    @PutMapping("/{id}")
    public PurchaseOrderResponseDto update(@PathVariable Long id, @RequestBody @Valid PurchaseOrderRequestDto input) { return PurchaseOrderResponseDto.fromEntity(service.update(id, input)); }

    @DeleteMapping("/{id}")
    public void softDelete(@PathVariable Long id) { service.softDelete(id); }

    @PostMapping("/{id}/restore")
    public PurchaseOrderResponseDto restore(@PathVariable Long id) { return PurchaseOrderResponseDto.fromEntity(service.restore(id)); }

    @GetMapping("/export")
    @Transactional(readOnly = true)
    public ResponseEntity<String> export(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "false") boolean includeDeleted) {
        String header = "ID;OC;Fecha OC;ID Solicitud Compra;Fecha Recepcion Producto/Servicio;Programa;Proveedor;CDP;SIGFE;Comprometido;Ajuste;MontoReal;Estado;Observacion;Eliminado\n";
        String rows = repository.exportRows(search.trim(), includeDeleted).stream()
                .map(po -> String.join(";",
                        CsvExportUtils.cell(po.getId()), CsvExportUtils.cell(po.getOrderNumber()), CsvExportUtils.cell(po.getOrderDate()),
                        CsvExportUtils.cell(po.getPurchaseRequestId()), CsvExportUtils.cell(po.getProductServiceReceptionDate()),
                        CsvExportUtils.cell(po.getProgram() != null ? po.getProgram().getName() : null),
                        CsvExportUtils.cell(po.getProvider() != null ? po.getProvider().getBusinessName() : null),
                        CsvExportUtils.cell(po.getCdp() != null ? po.getCdp().getCdpNumber() : null),
                        CsvExportUtils.cell(po.getSigfeFolio()), CsvExportUtils.cell(po.getCommittedAmount()), CsvExportUtils.cell(po.getAdjustmentAmount()),
                        CsvExportUtils.cell(po.getRealAmount()), CsvExportUtils.cell(po.getStatus()), CsvExportUtils.cell(po.getObservation()), CsvExportUtils.cell(po.getDeletedAt() != null)))
                .collect(Collectors.joining("\n"));
        return CsvExportUtils.response("ordenes_compra_filtrado.csv", header + rows);
    }
}
