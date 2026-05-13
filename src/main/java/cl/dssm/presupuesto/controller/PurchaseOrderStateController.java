package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.PageResponseDto;
import cl.dssm.presupuesto.entity.PurchaseOrder;
import cl.dssm.presupuesto.entity.PurchaseOrderState;
import cl.dssm.presupuesto.enums.PurchaseOrderStatus;
import cl.dssm.presupuesto.repository.PurchaseOrderRepository;
import cl.dssm.presupuesto.repository.PurchaseOrderStateRepository;
import cl.dssm.presupuesto.util.CsvExportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/purchase-order-states")
@RequiredArgsConstructor
public class PurchaseOrderStateController {
    private final PurchaseOrderStateRepository repository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    @GetMapping
    public PageResponseDto<PurchaseOrderState> findAll(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        syncDetectedStates();
        return PageResponseDto.from(repository.search(search.trim(), includeDeleted, pageable));
    }

    @PostMapping
    public PurchaseOrderState create(@RequestBody PurchaseOrderState e) {
        e.setId(null);
        if (e.getCode() == null || e.getCode().isBlank()) e.setCode(normalizeCode(e.getName()));
        return repository.save(e);
    }

    @PutMapping("/{id}")
    public PurchaseOrderState update(@PathVariable Long id, @RequestBody PurchaseOrderState i) {
        PurchaseOrderState e = repository.findById(id).orElseThrow();
        e.setCode(i.getCode() == null || i.getCode().isBlank() ? normalizeCode(i.getName()) : i.getCode());
        e.setName(i.getName());
        e.setDescription(i.getDescription());
        e.setActive(i.getActive());
        return repository.save(e);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        PurchaseOrderState e = repository.findById(id).orElseThrow();
        e.setDeletedAt(LocalDateTime.now());
        e.setActive(false);
        repository.save(e);
    }

    @PostMapping("/{id}/restore")
    public PurchaseOrderState restore(@PathVariable Long id) {
        PurchaseOrderState e = repository.findById(id).orElseThrow();
        e.setDeletedAt(null);
        e.setActive(true);
        return repository.save(e);
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        syncDetectedStates();
        String h = "ID;Codigo;Nombre;Descripcion;Activo;Eliminado\n";
        String rows = repository.exportRows(search.trim(), includeDeleted).stream()
                .map(e -> String.join(";", CsvExportUtils.cell(e.getId()), CsvExportUtils.cell(e.getCode()), CsvExportUtils.cell(e.getName()), CsvExportUtils.cell(e.getDescription()), CsvExportUtils.cell(e.getActive()), CsvExportUtils.cell(e.getDeletedAt() != null)))
                .collect(Collectors.joining("\n"));
        return CsvExportUtils.response("estados_oc.csv", h + rows);
    }

    private void syncDetectedStates() {
        purchaseOrderRepository.findAll().stream()
                .map(PurchaseOrder::getStatus)
                .filter(Objects::nonNull)
                .forEach(status -> repository.findByCodeIgnoreCase(status.name())
                        .orElseGet(() -> repository.save(PurchaseOrderState.builder()
                                .code(status.name())
                                .name(humanizeEnum(status.name()))
                                .description("Detectado automáticamente desde órdenes de compra")
                                .active(true)
                                .build())));

        repository.findByCodeIgnoreCase("SIN_ESTADO")
                .orElseGet(() -> repository.save(PurchaseOrderState.builder()
                        .code("SIN_ESTADO")
                        .name("Sin Estado")
                        .description("Estado por defecto para OC sin estado en planilla")
                        .active(true)
                        .build()));
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace("Ñ", "N")
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized.length() > 50 ? normalized.substring(0, 50) : normalized;
    }

    private String humanizeEnum(String value) {
        if (value == null || value.isBlank()) return "Sin Estado";
        String lower = value.toLowerCase().replace('_', ' ');
        return Arrays.stream(lower.split(" "))
                .filter(p -> !p.isBlank())
                .map(p -> p.substring(0, 1).toUpperCase() + p.substring(1))
                .collect(Collectors.joining(" "));
    }
}
