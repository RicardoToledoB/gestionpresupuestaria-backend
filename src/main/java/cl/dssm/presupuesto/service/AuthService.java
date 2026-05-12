package cl.dssm.presupuesto.service;

import cl.dssm.presupuesto.dto.AuthRequestDto;
import cl.dssm.presupuesto.dto.AuthResponseDto;
import cl.dssm.presupuesto.entity.AppUser;
import cl.dssm.presupuesto.repository.AppUserRepository;
import cl.dssm.presupuesto.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    public AuthResponseDto login(AuthRequestDto request) {
        AppUser user = userRepository.findByUsernameIgnoreCase(request.username())
                .filter(u -> Boolean.TRUE.equals(u.getActive()) && u.getDeletedAt() == null)
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña inválidos."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditLogService.register("SEGURIDAD", "LOGIN_FALLIDO", "AppUser", user.getId(), user.getUsername(), null, null, "Credenciales inválidas");
            throw new BadCredentialsException("Usuario o contraseña inválidos.");
        }

        List<String> roles = parseRoles(user.getRolesText());
        String token = jwtService.generateToken(user.getUsername(), user.getFullName(), roles);
        auditLogService.register("SEGURIDAD", "LOGIN_EXITOSO", "AppUser", user.getId(), user.getUsername(), null, String.join(",", roles), "Inicio de sesión exitoso");
        return new AuthResponseDto(token, "Bearer", jwtService.expirationMs(), user.getUsername(), user.getFullName(), roles);
    }

    private List<String> parseRoles(String rolesText) {
        if (rolesText == null || rolesText.isBlank()) return List.of("LECTURA");
        return Arrays.stream(rolesText.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.replace("ROLE_", ""))
                .distinct()
                .toList();
    }
}
