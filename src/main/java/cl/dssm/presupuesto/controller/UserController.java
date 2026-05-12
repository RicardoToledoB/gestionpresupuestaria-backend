package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.PageResponseDto;
import cl.dssm.presupuesto.entity.AppRole;
import cl.dssm.presupuesto.entity.AppUser;
import cl.dssm.presupuesto.repository.AppRoleRepository;
import cl.dssm.presupuesto.repository.AppUserRepository;
import cl.dssm.presupuesto.util.CsvExportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
  private final AppUserRepository repository;
  private final AppRoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  @GetMapping
  public PageResponseDto<AppUser> findAll(
      @RequestParam(defaultValue="") String search,
      @RequestParam(defaultValue="false") boolean includeDeleted,
      @PageableDefault(size=20, sort="fullName") Pageable pageable) {
    Page<AppUser> page = repository.search(search.trim(), includeDeleted, pageable);
    page.getContent().forEach(this::hydrateRoleFields);
    return PageResponseDto.from(page);
  }

  @PostMapping
  public AppUser create(@RequestBody AppUser incoming) {
    incoming.setId(null);
    String raw = incoming.getPasswordHash();
    incoming.setPasswordHash(passwordEncoder.encode(raw == null || raw.isBlank() ? "Cambiar123" : raw));
    incoming.setRolesText(resolveRolesText(incoming));
    AppUser saved = repository.save(incoming);
    hydrateRoleFields(saved);
    return saved;
  }

  @PutMapping("/{id}")
  public AppUser update(@PathVariable Long id, @RequestBody AppUser incoming) {
    AppUser user = repository.findById(id).orElseThrow();
    user.setUsername(incoming.getUsername());
    user.setFullName(incoming.getFullName());
    user.setEmail(incoming.getEmail());
    user.setActive(incoming.getActive());
    user.setRolesText(resolveRolesText(incoming));
    if (incoming.getPasswordHash() != null && !incoming.getPasswordHash().isBlank()) {
      user.setPasswordHash(passwordEncoder.encode(incoming.getPasswordHash()));
    }
    AppUser saved = repository.save(user);
    hydrateRoleFields(saved);
    return saved;
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    AppUser user = repository.findById(id).orElseThrow();
    user.setDeletedAt(LocalDateTime.now());
    user.setActive(false);
    repository.save(user);
  }

  @PostMapping("/{id}/restore")
  public AppUser restore(@PathVariable Long id) {
    AppUser user = repository.findById(id).orElseThrow();
    user.setDeletedAt(null);
    user.setActive(true);
    AppUser saved = repository.save(user);
    hydrateRoleFields(saved);
    return saved;
  }

  @GetMapping("/export")
  public ResponseEntity<String> export(
      @RequestParam(defaultValue="") String search,
      @RequestParam(defaultValue="false") boolean includeDeleted) {
    String header = "ID;Usuario;NombreCompleto;Correo;Roles;Activo;Eliminado\n";
    String rows = repository.exportRows(search.trim(), includeDeleted).stream()
        .map(e -> String.join(";",
            CsvExportUtils.cell(e.getId()),
            CsvExportUtils.cell(e.getUsername()),
            CsvExportUtils.cell(e.getFullName()),
            CsvExportUtils.cell(e.getEmail()),
            CsvExportUtils.cell(e.getRolesText()),
            CsvExportUtils.cell(e.getActive()),
            CsvExportUtils.cell(e.getDeletedAt() != null)))
        .collect(Collectors.joining("\n"));
    return CsvExportUtils.response("usuarios.csv", header + rows);
  }

  private String resolveRolesText(AppUser incoming) {
    List<Long> roleIds = incoming.getRoleIds();
    if (roleIds != null && !roleIds.isEmpty()) {
      LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(roleIds);
      List<AppRole> roles = roleRepository.findAllById(uniqueIds);
      Map<Long, AppRole> byId = roles.stream().collect(Collectors.toMap(AppRole::getId, r -> r));

      List<Long> missing = uniqueIds.stream().filter(id -> !byId.containsKey(id)).toList();
      if (!missing.isEmpty()) {
        throw new IllegalArgumentException("Existen roles no registrados: " + missing);
      }

      return uniqueIds.stream()
          .map(byId::get)
          .filter(r -> Boolean.TRUE.equals(r.getActive()) && r.getDeletedAt() == null)
          .map(AppRole::getName)
          .map(String::trim)
          .filter(s -> !s.isBlank())
          .distinct()
          .collect(Collectors.joining(","));
    }

    String rolesText = incoming.getRolesText();
    if (rolesText == null || rolesText.isBlank()) {
      return "LECTURA";
    }

    List<String> requestedNames = Arrays.stream(rolesText.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .map(s -> s.replace("ROLE_", ""))
        .distinct()
        .toList();

    List<String> normalizedNames = new ArrayList<>();
    for (String name : requestedNames) {
      AppRole role = roleRepository.findByNameIgnoreCase(name)
          .orElseThrow(() -> new IllegalArgumentException("El rol no existe en el mantenedor: " + name));
      if (Boolean.TRUE.equals(role.getActive()) && role.getDeletedAt() == null) {
        normalizedNames.add(role.getName());
      }
    }
    return normalizedNames.isEmpty() ? "LECTURA" : String.join(",", normalizedNames);
  }

  private void hydrateRoleFields(AppUser user) {
    if (user == null) return;
    List<String> roleNames = parseRoleNames(user.getRolesText());
    user.setRoleNames(roleNames);
    List<Long> ids = roleNames.stream()
        .map(roleRepository::findByNameIgnoreCase)
        .flatMap(Optional::stream)
        .map(AppRole::getId)
        .toList();
    user.setRoleIds(ids);
  }

  private List<String> parseRoleNames(String rolesText) {
    if (rolesText == null || rolesText.isBlank()) return List.of();
    return Arrays.stream(rolesText.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .map(s -> s.replace("ROLE_", ""))
        .distinct()
        .toList();
  }
}
