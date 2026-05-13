package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.PageResponseDto;
import cl.dssm.presupuesto.entity.BudgetProgram;
import cl.dssm.presupuesto.repository.BudgetProgramRepository;
import cl.dssm.presupuesto.util.CsvExportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/programs")
@RequiredArgsConstructor
public class ProgramController {
    private final BudgetProgramRepository repository;

    @GetMapping
    public PageResponseDto<BudgetProgram> findAll(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "false") boolean includeDeleted, @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return PageResponseDto.from(repository.search(search.trim(), includeDeleted, pageable));
    }

    @PostMapping
    public BudgetProgram create(@RequestBody BudgetProgram program) { program.setId(null); return repository.save(program); }

    @PutMapping("/{id}")
    public BudgetProgram update(@PathVariable Long id, @RequestBody BudgetProgram input) {
        BudgetProgram p = repository.findById(id).orElseThrow();
        p.setName(input.getName()); p.setCode(input.getCode()); p.setSubtitle(input.getSubtitle()); p.setDescription(input.getDescription());
        p.setInitialBudget(input.getInitialBudget()); p.setCurrentBudget(input.getCurrentBudget()); p.setActive(input.getActive());
        return repository.save(p);
    }

    @DeleteMapping("/{id}")
    public void softDelete(@PathVariable Long id) { BudgetProgram p = repository.findById(id).orElseThrow(); p.setDeletedAt(LocalDateTime.now()); p.setActive(false); repository.save(p); }

    @PostMapping("/{id}/restore")
    public BudgetProgram restore(@PathVariable Long id) { BudgetProgram p = repository.findById(id).orElseThrow(); p.setDeletedAt(null); p.setActive(true); return repository.save(p); }

    @GetMapping("/export")
    public ResponseEntity<String> export(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "false") boolean includeDeleted) {
        String header = "ID;Programa;Codigo;Subtitulo;PresupuestoInicial;PresupuestoVigente;Activo;Eliminado\n";
        String rows = repository.exportRows(search.trim(), includeDeleted).stream()
                .map(p -> String.join(";", CsvExportUtils.cell(p.getId()), CsvExportUtils.cell(p.getName()), CsvExportUtils.cell(p.getCode()), CsvExportUtils.cell(p.getSubtitle()), CsvExportUtils.cell(p.getInitialBudget()), CsvExportUtils.cell(p.getCurrentBudget()), CsvExportUtils.cell(p.getActive()), CsvExportUtils.cell(p.getDeletedAt() != null)))
                .collect(Collectors.joining("\n"));
        return CsvExportUtils.response("programas_presupuestarios.csv", header + rows);
    }
}
