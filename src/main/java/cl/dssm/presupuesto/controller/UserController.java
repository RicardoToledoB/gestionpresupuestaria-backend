package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.PageResponseDto;
import cl.dssm.presupuesto.dto.ResetPasswordRequestDto;
import cl.dssm.presupuesto.entity.AppRole;
import cl.dssm.presupuesto.entity.AppUser;
import cl.dssm.presupuesto.repository.AppRoleRepository;
import cl.dssm.presupuesto.repository.AppUserRepository;
import cl.dssm.presupuesto.service.AuditLogService;
import cl.dssm.presupuesto.util.CsvExportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
  private final AuditLogService auditLogService;

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
    validateNewPassword(raw == null || raw.isBlank() ? "Cambiar123" : raw, raw == null || raw.isBlank() ? "Cambiar123" : raw);
    incoming.setPasswordHash(passwordEncoder.encode(raw == null || raw.isBlank() ? "Cambiar123" : raw));
    incoming.setRolesText(resolveRolesText(incoming));
    incoming.setLastPasswordChangeAt(LocalDateTime.now());
    incoming.setLockedAt(null);
    AppUser saved = repository.save(incoming);
    auditLogService.register("USUARIOS", "CREATE_USER", "AppUser", saved.getId(), saved.getUsername(), null, safeUserSnapshot(saved), "Usuario creado");
    hydrateRoleFields(saved);
    return saved;
  }

  @PutMapping("/{id}")
  public AppUser update(@PathVariable Long id, @RequestBody AppUser incoming) {
    AppUser user = repository.findById(id).orElseThrow();
    String previousRoles = user.getRolesText();
    String previousSnapshot = safeUserSnapshot(user);
    String newRoles = resolveRolesText(incoming);

    Boolean newActive = incoming.getActive() == null ? user.getActive() : incoming.getActive();
    assertAtLeastOneAdminAfterChange(user, newRoles, newActive, user.getDeletedAt(), user.getLockedAt(), "No es posible dejar el sistema sin un usuario ADMIN activo.");

    user.setUsername(incoming.getUsername());
    user.setFullName(incoming.getFullName());
    user.setEmail(incoming.getEmail());
    user.setActive(newActive);
    user.setRolesText(newRoles);
    if (incoming.getPasswordHash() != null && !incoming.getPasswordHash().isBlank()) {
      validateNewPassword(incoming.getPasswordHash(), incoming.getPasswordHash());
      user.setPasswordHash(passwordEncoder.encode(incoming.getPasswordHash()));
      user.setLastPasswordChangeAt(LocalDateTime.now());
      auditLogService.register("USUARIOS", "RESET_PASSWORD_INLINE", "AppUser", user.getId(), user.getUsername(), null, "PASSWORD_RESET", "Contraseña reseteada desde edición de usuario");
    }
    AppUser saved = repository.save(user);
    if (!Objects.equals(normalizeRoles(previousRoles), normalizeRoles(newRoles))) {
      auditLogService.register("USUARIOS", "ROLE_CHANGE", "AppUser", saved.getId(), saved.getUsername(), previousRoles, newRoles, "Cambio de roles del usuario");
    }
    auditLogService.register("USUARIOS", "UPDATE_USER", "AppUser", saved.getId(), saved.getUsername(), previousSnapshot, safeUserSnapshot(saved), "Usuario actualizado");
    hydrateRoleFields(saved);
    return saved;
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    AppUser user = repository.findById(id).orElseThrow();
    if (isCurrentUser(user)) {
      throw new IllegalArgumentException("No puede eliminar su propio usuario administrador.");
    }
    assertAtLeastOneAdminAfterChange(user, user.getRolesText(), false, LocalDateTime.now(), user.getLockedAt(), "No es posible eliminar o desactivar el último usuario ADMIN activo.");
    String previous = safeUserSnapshot(user);
    user.setDeletedAt(LocalDateTime.now());
    user.setActive(false);
    repository.save(user);
    auditLogService.register("USUARIOS", "SOFT_DELETE_USER", "AppUser", user.getId(), user.getUsername(), previous, safeUserSnapshot(user), "Usuario eliminado lógicamente");
  }

  @PostMapping("/{id}/restore")
  public AppUser restore(@PathVariable Long id) {
    AppUser user = repository.findById(id).orElseThrow();
    String previous = safeUserSnapshot(user);
    user.setDeletedAt(null);
    user.setActive(true);
    AppUser saved = repository.save(user);
    auditLogService.register("USUARIOS", "RESTORE_USER", "AppUser", saved.getId(), saved.getUsername(), previous, safeUserSnapshot(saved), "Usuario recuperado");
    hydrateRoleFields(saved);
    return saved;
  }

  @PostMapping("/{id}/reset-password")
  public ResponseEntity<Map<String, Object>> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordRequestDto request) {
    AppUser user = repository.findById(id).orElseThrow();
    validateNewPassword(request.newPassword(), request.confirmPassword());
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    user.setLastPasswordChangeAt(LocalDateTime.now());
    repository.save(user);
    auditLogService.register("USUARIOS", "RESET_PASSWORD", "AppUser", user.getId(), user.getUsername(), null, "PASSWORD_RESET", "Contraseña reseteada por administrador");
    return ResponseEntity.ok(Map.of("message", "Contraseña reseteada correctamente."));
  }

  @PostMapping("/{id}/lock")
  public AppUser lock(@PathVariable Long id) {
    AppUser user = repository.findById(id).orElseThrow();
    if (isCurrentUser(user)) {
      throw new IllegalArgumentException("No puede bloquear su propio usuario.");
    }
    assertAtLeastOneAdminAfterChange(user, user.getRolesText(), user.getActive(), user.getDeletedAt(), LocalDateTime.now(), "No es posible bloquear el último usuario ADMIN activo.");
    String previous = safeUserSnapshot(user);
    user.setLockedAt(LocalDateTime.now());
    AppUser saved = repository.save(user);
    auditLogService.register("USUARIOS", "LOCK_USER", "AppUser", saved.getId(), saved.getUsername(), previous, safeUserSnapshot(saved), "Usuario bloqueado por administrador");
    hydrateRoleFields(saved);
    return saved;
  }

  @PostMapping("/{id}/unlock")
  public AppUser unlock(@PathVariable Long id) {
    AppUser user = repository.findById(id).orElseThrow();
    String previous = safeUserSnapshot(user);
    user.setLockedAt(null);
    AppUser saved = repository.save(user);
    auditLogService.register("USUARIOS", "UNLOCK_USER", "AppUser", saved.getId(), saved.getUsername(), previous, safeUserSnapshot(saved), "Usuario desbloqueado por administrador");
    hydrateRoleFields(saved);
    return saved;
  }

  @GetMapping("/export")
  public ResponseEntity<String> export(
      @RequestParam(defaultValue="") String search,
      @RequestParam(defaultValue="false") boolean includeDeleted) {
    String header = "ID;Usuario;NombreCompleto;Correo;Roles;Activo;Bloqueado;UltimoAcceso;UltimoCambioPassword;Eliminado\n";
    String rows = repository.exportRows(search.trim(), includeDeleted).stream()
        .map(e -> String.join(";",
            CsvExportUtils.cell(e.getId()),
            CsvExportUtils.cell(e.getUsername()),
            CsvExportUtils.cell(e.getFullName()),
            CsvExportUtils.cell(e.getEmail()),
            CsvExportUtils.cell(e.getRolesText()),
            CsvExportUtils.cell(e.getActive()),
            CsvExportUtils.cell(e.getLockedAt() != null),
            CsvExportUtils.cell(e.getLastLoginAt()),
            CsvExportUtils.cell(e.getLastPasswordChangeAt()),
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

  private boolean isCurrentUser(AppUser user) {
    return user != null && user.getUsername() != null && user.getUsername().equalsIgnoreCase(currentUsername());
  }

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) return "system";
    return auth.getName();
  }

  private void assertAtLeastOneAdminAfterChange(AppUser target, String targetRoles, Boolean targetActive, LocalDateTime targetDeletedAt, LocalDateTime targetLockedAt, String message) {
    long activeAdmins = repository.findAll().stream()
        .filter(user -> {
          String roles = Objects.equals(user.getId(), target.getId()) ? targetRoles : user.getRolesText();
          Boolean active = Objects.equals(user.getId(), target.getId()) ? targetActive : user.getActive();
          LocalDateTime deletedAt = Objects.equals(user.getId(), target.getId()) ? targetDeletedAt : user.getDeletedAt();
          LocalDateTime lockedAt = Objects.equals(user.getId(), target.getId()) ? targetLockedAt : user.getLockedAt();
          return Boolean.TRUE.equals(active) && deletedAt == null && lockedAt == null && hasAdminRole(roles);
        })
        .count();
    if (activeAdmins <= 0) throw new IllegalArgumentException(message);
  }

  private boolean hasAdminRole(String rolesText) {
    return parseRoleNames(rolesText).stream().anyMatch(role -> role.equalsIgnoreCase("ADMIN"));
  }

  private String normalizeRoles(String rolesText) {
    return parseRoleNames(rolesText).stream().map(String::toUpperCase).sorted().collect(Collectors.joining(","));
  }

  private void validateNewPassword(String password, String confirmPassword) {
    if (password == null || password.isBlank()) throw new IllegalArgumentException("Debe indicar una contraseña.");
    if (password.length() < 8) throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
    if (!password.equals(confirmPassword)) throw new IllegalArgumentException("La confirmación de contraseña no coincide.");
  }

  private String safeUserSnapshot(AppUser user) {
    if (user == null) return null;
    return "username=" + user.getUsername()
        + "; fullName=" + user.getFullName()
        + "; email=" + user.getEmail()
        + "; roles=" + user.getRolesText()
        + "; active=" + user.getActive()
        + "; lockedAt=" + user.getLockedAt()
        + "; deletedAt=" + user.getDeletedAt()
        + "; lastLoginAt=" + user.getLastLoginAt();
  }
}
