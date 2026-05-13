package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.PageResponseDto;
import cl.dssm.presupuesto.entity.BudgetSubtitle;
import cl.dssm.presupuesto.entity.PurchaseOrder;
import cl.dssm.presupuesto.repository.BudgetSubtitleRepository;
import cl.dssm.presupuesto.repository.PurchaseOrderRepository;
import cl.dssm.presupuesto.util.CsvExportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/budget-subtitles")
@RequiredArgsConstructor
public class BudgetSubtitleController {
    private final BudgetSubtitleRepository repository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    @GetMapping
    public PageResponseDto<BudgetSubtitle> findAll(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @PageableDefault(size = 20, sort = "code") Pageable pageable
    ) {
        syncDetectedSubtitles();
        return PageResponseDto.from(repository.search(search.trim(), includeDeleted, pageable));
    }

    @PostMapping
    public BudgetSubtitle create(@RequestBody BudgetSubtitle e) {
        e.setId(null);
        return repository.save(e);
    }

    @PutMapping("/{id}")
    public BudgetSubtitle update(@PathVariable Long id, @RequestBody BudgetSubtitle i) {
        BudgetSubtitle e = repository.findById(id).orElseThrow();
        e.setCode(i.getCode());
        e.setName(i.getName());
        e.setDescription(i.getDescription());
        e.setActive(i.getActive());
        return repository.save(e);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        BudgetSubtitle e = repository.findById(id).orElseThrow();
        e.setDeletedAt(LocalDateTime.now());
        e.setActive(false);
        repository.save(e);
    }

    @PostMapping("/{id}/restore")
    public BudgetSubtitle restore(@PathVariable Long id) {
        BudgetSubtitle e = repository.findById(id).orElseThrow();
        e.setDeletedAt(null);
        e.setActive(true);
        return repository.save(e);
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        syncDetectedSubtitles();
        String h = "ID;Codigo;Nombre;Descripcion;Activo;Eliminado\n";
        String rows = repository.exportRows(search.trim(), includeDeleted).stream()
                .map(e -> String.join(";", CsvExportUtils.cell(e.getId()), CsvExportUtils.cell(e.getCode()), CsvExportUtils.cell(e.getName()), CsvExportUtils.cell(e.getDescription()), CsvExportUtils.cell(e.getActive()), CsvExportUtils.cell(e.getDeletedAt() != null)))
                .collect(Collectors.joining("\n"));
        return CsvExportUtils.response("subtitulos_presupuestarios.csv", h + rows);
    }

    private void syncDetectedSubtitles() {
        purchaseOrderRepository.findAll().stream()
                .map(PurchaseOrder::getSubtitle)
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .forEach(code -> repository.findByCodeIgnoreCase(code)
                        .orElseGet(() -> repository.save(BudgetSubtitle.builder()
                                .code(code)
                                .name("Subtítulo " + code)
                                .description("Detectado automáticamente desde órdenes de compra")
                                .active(true)
                                .build())));

        repository.findByCodeIgnoreCase("SIN_SUBTITULO")
                .orElseGet(() -> repository.save(BudgetSubtitle.builder()
                        .code("SIN_SUBTITULO")
                        .name("Sin subtítulo")
                        .description("Opción por defecto para OC sin subtítulo")
                        .active(true)
                        .build()));
    }
}
