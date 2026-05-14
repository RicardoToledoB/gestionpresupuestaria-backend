package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.PageResponseDto;
import cl.dssm.presupuesto.entity.Cdp;
import cl.dssm.presupuesto.entity.CdpType;
import cl.dssm.presupuesto.repository.CdpRepository;
import cl.dssm.presupuesto.repository.CdpTypeRepository;
import cl.dssm.presupuesto.util.CsvExportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController @RequestMapping("/cdp-types") @RequiredArgsConstructor
public class CdpTypeController {
  private final CdpTypeRepository repository;
  private final CdpRepository cdpRepository;
  @GetMapping public PageResponseDto<CdpType> findAll(@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="false") boolean includeDeleted,@PageableDefault(size=20,sort="name") Pageable pageable){syncDetectedCdpTypes(); return PageResponseDto.from(repository.search(search.trim(),includeDeleted,pageable));}
  @PostMapping public CdpType create(@RequestBody CdpType e){e.setId(null); return repository.save(e);} 
  @PutMapping("/{id}") public CdpType update(@PathVariable Long id,@RequestBody CdpType i){CdpType e=repository.findById(id).orElseThrow(); e.setCode(i.getCode()); e.setName(i.getName()); e.setDescription(i.getDescription()); e.setActive(i.getActive()); return repository.save(e);} 
  @DeleteMapping("/{id}") public void delete(@PathVariable Long id){CdpType e=repository.findById(id).orElseThrow(); e.setDeletedAt(LocalDateTime.now()); e.setActive(false); repository.save(e);} 
  @PostMapping("/{id}/restore") public CdpType restore(@PathVariable Long id){CdpType e=repository.findById(id).orElseThrow(); e.setDeletedAt(null); e.setActive(true); return repository.save(e);} 
  @GetMapping("/export") public ResponseEntity<String> export(@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="false") boolean includeDeleted){syncDetectedCdpTypes(); String h="ID;Codigo;Nombre;Descripcion;Activo;Eliminado\n"; String rows=repository.exportRows(search.trim(),includeDeleted).stream().map(e->String.join(";", CsvExportUtils.cell(e.getId()),CsvExportUtils.cell(e.getCode()),CsvExportUtils.cell(e.getName()),CsvExportUtils.cell(e.getDescription()),CsvExportUtils.cell(e.getActive()),CsvExportUtils.cell(e.getDeletedAt()!=null))).collect(Collectors.joining("\n")); return CsvExportUtils.response("tipos_cdp.csv", h+rows);} 


  private void syncDetectedCdpTypes() {
    cdpRepository.findAll().stream()
        .map(Cdp::getCdpType)
        .filter(v -> v != null && !v.isBlank())
        .map(String::trim)
        .distinct()
        .forEach(name -> repository.findByNameIgnoreCase(name).orElseGet(() -> repository.save(CdpType.builder()
            .name(name)
            .code(normalizeCode(name))
            .description("Detectado automáticamente desde CDP importados")
            .active(true)
            .build())));
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
}