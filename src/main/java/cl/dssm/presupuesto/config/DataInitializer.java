package cl.dssm.presupuesto.config;

import cl.dssm.presupuesto.entity.AppRole;
import cl.dssm.presupuesto.entity.AppUser;
import cl.dssm.presupuesto.repository.AppRoleRepository;
import cl.dssm.presupuesto.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final AppRoleRepository roleRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        List<String> roles = List.of("ADMIN", "DIRECCION", "PRESUPUESTO", "ABASTECIMIENTO", "LECTURA", "AUDITOR");
        for (String role : roles) {
            roleRepository.findByNameIgnoreCase(role).orElseGet(() -> roleRepository.save(AppRole.builder()
                    .name(role)
                    .description("Rol funcional " + role)
                    .active(true)
                    .build()));
        }

        userRepository.findByUsernameIgnoreCase("admin").orElseGet(() -> userRepository.save(AppUser.builder()
                .username("admin")
                .fullName("Administrador Plataforma")
                .email("admin@dssm.cl")
                .passwordHash(passwordEncoder.encode("admin123"))
                .rolesText("ADMIN,DIRECCION,PRESUPUESTO,ABASTECIMIENTO,LECTURA")
                .active(true)
                .build()));
    }
}
