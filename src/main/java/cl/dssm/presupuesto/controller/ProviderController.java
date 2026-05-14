package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.PageResponseDto;
import cl.dssm.presupuesto.entity.Provider;
import cl.dssm.presupuesto.repository.ProviderRepository;
import cl.dssm.presupuesto.util.CsvExportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/providers")
@RequiredArgsConstructor
public class ProviderController {
    private final ProviderRepository repository;

    @GetMapping
    public PageResponseDto<Provider> findAll(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "false") boolean includeDeleted, @PageableDefault(size = 20, sort = "businessName") Pageable pageable) {
        return PageResponseDto.from(repository.search(search.trim(), includeDeleted, pageable));
    }

    @PostMapping
    public Provider create(@RequestBody Provider provider) { provider.setId(null); return repository.save(provider); }

    @PutMapping("/{id}")
    public Provider update(@PathVariable Long id, @RequestBody Provider input) {
        Provider p = repository.findById(id).orElseThrow();
        p.setRut(input.getRut()); p.setBusinessName(input.getBusinessName()); p.setFantasyName(input.getFantasyName()); p.setLineOfBusiness(input.getLineOfBusiness());
        p.setEmail(input.getEmail()); p.setPhone(input.getPhone()); p.setActive(input.getActive());
        return repository.save(p);
    }

    @DeleteMapping("/{id}")
    public void softDelete(@PathVariable Long id) { Provider p = repository.findById(id).orElseThrow(); p.setDeletedAt(LocalDateTime.now()); p.setActive(false); repository.save(p); }

    @PostMapping("/{id}/restore")
    public Provider restore(@PathVariable Long id) { Provider p = repository.findById(id).orElseThrow(); p.setDeletedAt(null); p.setActive(true); return repository.save(p); }

    @GetMapping("/export")
    public ResponseEntity<String> export(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "false") boolean includeDeleted) {
        String header = "ID;RUT;RazonSocial;NombreFantasia;Giro;Correo;Telefono;Activo;Eliminado\n";
        String rows = repository.exportRows(search.trim(), includeDeleted).stream()
                .map(p -> String.join(";", CsvExportUtils.cell(p.getId()), CsvExportUtils.cell(p.getRut()), CsvExportUtils.cell(p.getBusinessName()), CsvExportUtils.cell(p.getFantasyName()), CsvExportUtils.cell(p.getLineOfBusiness()), CsvExportUtils.cell(p.getEmail()), CsvExportUtils.cell(p.getPhone()), CsvExportUtils.cell(p.getActive()), CsvExportUtils.cell(p.getDeletedAt() != null)))
                .collect(Collectors.joining("\n"));
        return CsvExportUtils.response("proveedores.csv", header + rows);
    }
}
