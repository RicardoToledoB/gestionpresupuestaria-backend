package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.PageResponseDto;
import cl.dssm.presupuesto.entity.AppUser;
import cl.dssm.presupuesto.repository.AppUserRepository;
import cl.dssm.presupuesto.util.CsvExportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController @RequestMapping("/users") @RequiredArgsConstructor
public class UserController {
  private final AppUserRepository repository;
  @GetMapping public PageResponseDto<AppUser> findAll(@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="false") boolean includeDeleted,@PageableDefault(size=20,sort="fullName") Pageable pageable){return PageResponseDto.from(repository.search(search.trim(),includeDeleted,pageable));}
  @PostMapping public AppUser create(@RequestBody AppUser e){e.setId(null); return repository.save(e);} 
  @PutMapping("/{id}") public AppUser update(@PathVariable Long id,@RequestBody AppUser i){AppUser e=repository.findById(id).orElseThrow(); e.setUsername(i.getUsername()); e.setFullName(i.getFullName()); e.setEmail(i.getEmail()); e.setRolesText(i.getRolesText()); e.setActive(i.getActive()); return repository.save(e);} 
  @DeleteMapping("/{id}") public void delete(@PathVariable Long id){AppUser e=repository.findById(id).orElseThrow(); e.setDeletedAt(LocalDateTime.now()); e.setActive(false); repository.save(e);} 
  @PostMapping("/{id}/restore") public AppUser restore(@PathVariable Long id){AppUser e=repository.findById(id).orElseThrow(); e.setDeletedAt(null); e.setActive(true); return repository.save(e);} 
  @GetMapping("/export") public ResponseEntity<String> export(@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="false") boolean includeDeleted){String h="ID;Usuario;NombreCompleto;Correo;Roles;Activo;Eliminado\n"; String rows=repository.exportRows(search.trim(),includeDeleted).stream().map(e->String.join(";", CsvExportUtils.cell(e.getId()),CsvExportUtils.cell(e.getUsername()),CsvExportUtils.cell(e.getFullName()),CsvExportUtils.cell(e.getEmail()),CsvExportUtils.cell(e.getRolesText()),CsvExportUtils.cell(e.getActive()),CsvExportUtils.cell(e.getDeletedAt()!=null))).collect(Collectors.joining("\n")); return CsvExportUtils.response("usuarios.csv", h+rows);} 
}
