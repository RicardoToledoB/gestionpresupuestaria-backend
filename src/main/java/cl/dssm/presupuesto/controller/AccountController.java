package cl.dssm.presupuesto.controller;

import cl.dssm.presupuesto.dto.ChangePasswordRequestDto;
import cl.dssm.presupuesto.entity.AppUser;
import cl.dssm.presupuesto.repository.AppUserRepository;
import cl.dssm.presupuesto.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody ChangePasswordRequestDto request) {
        String username = currentUsername();
        AppUser user = userRepository.findByUsernameIgnoreCase(username).orElseThrow();

        if (request.currentPassword() == null || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            auditLogService.register("SEGURIDAD", "CHANGE_PASSWORD_FAILED", "AppUser", user.getId(), user.getUsername(), null, null, "Contraseña actual incorrecta");
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }

        validateNewPassword(request.newPassword(), request.confirmPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setLastPasswordChangeAt(LocalDateTime.now());
        userRepository.save(user);

        auditLogService.register("SEGURIDAD", "CHANGE_PASSWORD", "AppUser", user.getId(), user.getUsername(), null, "PASSWORD_CHANGED", "Cambio de contraseña realizado por el propio usuario");
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente."));
    }

    private void validateNewPassword(String password, String confirmPassword) {
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Debe indicar una nueva contraseña.");
        if (password.length() < 8) throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        if (!password.equals(confirmPassword)) throw new IllegalArgumentException("La confirmación de contraseña no coincide.");
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            throw new IllegalStateException("No existe un usuario autenticado.");
        }
        return auth.getName();
    }
}
