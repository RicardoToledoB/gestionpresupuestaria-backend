package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.PageResponseDto;
import cl.dssm.presupuesto.entity.PurchaseOrderState;
import cl.dssm.presupuesto.repository.PurchaseOrderStateRepository;
import cl.dssm.presupuesto.util.CsvExportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController @RequestMapping("/purchase-order-states") @RequiredArgsConstructor
public class PurchaseOrderStateController {
  private final PurchaseOrderStateRepository repository;
  @GetMapping public PageResponseDto<PurchaseOrderState> findAll(@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="false") boolean includeDeleted,@PageableDefault(size=20,sort="name") Pageable pageable){return PageResponseDto.from(repository.search(search.trim(),includeDeleted,pageable));}
  @PostMapping public PurchaseOrderState create(@RequestBody PurchaseOrderState e){e.setId(null); return repository.save(e);} 
  @PutMapping("/{id}") public PurchaseOrderState update(@PathVariable Long id,@RequestBody PurchaseOrderState i){PurchaseOrderState e=repository.findById(id).orElseThrow(); e.setCode(i.getCode()); e.setName(i.getName()); e.setDescription(i.getDescription()); e.setActive(i.getActive()); return repository.save(e);} 
  @DeleteMapping("/{id}") public void delete(@PathVariable Long id){PurchaseOrderState e=repository.findById(id).orElseThrow(); e.setDeletedAt(LocalDateTime.now()); e.setActive(false); repository.save(e);} 
  @PostMapping("/{id}/restore") public PurchaseOrderState restore(@PathVariable Long id){PurchaseOrderState e=repository.findById(id).orElseThrow(); e.setDeletedAt(null); e.setActive(true); return repository.save(e);} 
  @GetMapping("/export") public ResponseEntity<String> export(@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="false") boolean includeDeleted){String h="ID;Codigo;Nombre;Descripcion;Activo;Eliminado\n"; String rows=repository.exportRows(search.trim(),includeDeleted).stream().map(e->String.join(";", CsvExportUtils.cell(e.getId()),CsvExportUtils.cell(e.getCode()),CsvExportUtils.cell(e.getName()),CsvExportUtils.cell(e.getDescription()),CsvExportUtils.cell(e.getActive()),CsvExportUtils.cell(e.getDeletedAt()!=null))).collect(Collectors.joining("\n")); return CsvExportUtils.response("estados_oc.csv", h+rows);} 
}
