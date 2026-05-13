package cl.dssm.presupuesto.config;

import cl.dssm.presupuesto.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String[] READ_ROLES = {"ADMIN", "DIRECCION", "PRESUPUESTO", "ABASTECIMIENTO", "LECTURA", "AUDITOR"};
    private static final String[] REPORT_ROLES = {"ADMIN", "DIRECCION", "PRESUPUESTO", "AUDITOR"};
    private static final String[] AUDIT_ROLES = {"ADMIN", "AUDITOR"};
    private static final String[] BUDGET_WRITE_ROLES = {"ADMIN", "PRESUPUESTO"};
    private static final String[] SUPPLY_WRITE_ROLES = {"ADMIN", "ABASTECIMIENTO"};
    private static final String[] MASTER_WRITE_ROLES = {"ADMIN", "PRESUPUESTO"};
    private static final String[] SUBDIRECCION_ROLES = {"ADMIN", "DIRECCION", "PRESUPUESTO", "ABASTECIMIENTO"};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/docs/**",
                                "/swagger-ui/**",
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/auth/**",
                                "/h2-console/**"
                        ).permitAll()

                        // Mercado Público: proxy público de solo lectura.
                        // Permite probar directamente la URL del backend y evita errores por sesión expirada
                        // al abrir el endpoint en navegador. No expone el ticket al frontend.
                        .requestMatchers(HttpMethod.GET, "/mercado-publico/**").permitAll()

                        // Exportaciones filtradas: permitidas solo a perfiles operativos/directivos/auditoría, no a LECTURA simple.
                        .requestMatchers(HttpMethod.GET,
                                "/programs/export", "/providers/export", "/budget-items/export", "/cdp-types/export",
                                "/purchase-order-states/export", "/budget-subtitles/export", "/cdps/export", "/purchase-orders/export")
                        .hasAnyRole("ADMIN", "DIRECCION", "PRESUPUESTO", "ABASTECIMIENTO", "AUDITOR")

                        // Gestión Subdirección / CeroPapel: consulta de solicitudes de compra originadas en CeroPapel.
                        .requestMatchers(HttpMethod.GET, "/ceropapel/**").hasAnyRole(SUBDIRECCION_ROLES)

                        // Consulta general permitida a perfiles de lectura.
                        .requestMatchers(HttpMethod.GET, "/dashboard/**", "/master-data/**", "/quadrature/**").hasAnyRole(READ_ROLES)
                        .requestMatchers(HttpMethod.GET,
                                "/programs/**", "/providers/**", "/budget-items/**", "/cdp-types/**",
                                "/purchase-order-states/**", "/budget-subtitles/**", "/cdps/**", "/purchase-orders/**")
                        .hasAnyRole(READ_ROLES)

                        // Reportes y auditoría no quedan disponibles para LECTURA simple.
                        .requestMatchers(HttpMethod.GET, "/reports/**").hasAnyRole(REPORT_ROLES)
                        .requestMatchers(HttpMethod.GET, "/audit-logs/**").hasAnyRole(AUDIT_ROLES)

                        // Importación y limpieza de base: solo administración presupuestaria.
                        .requestMatchers(HttpMethod.GET, "/imports/**").hasAnyRole("ADMIN", "PRESUPUESTO", "AUDITOR")
                        .requestMatchers(HttpMethod.POST, "/imports/**").hasAnyRole("ADMIN", "PRESUPUESTO")
                        .requestMatchers(HttpMethod.DELETE, "/imports/**").hasRole("ADMIN")

                        // Mantenedores presupuestarios.
                        .requestMatchers(HttpMethod.POST, "/programs/**", "/budget-items/**", "/cdp-types/**", "/budget-subtitles/**").hasAnyRole(MASTER_WRITE_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/programs/**", "/budget-items/**", "/cdp-types/**", "/budget-subtitles/**").hasAnyRole(MASTER_WRITE_ROLES)
                        .requestMatchers(HttpMethod.DELETE, "/programs/**", "/budget-items/**", "/cdp-types/**", "/budget-subtitles/**").hasAnyRole(MASTER_WRITE_ROLES)

                        // Proveedores y estados de OC.
                        .requestMatchers(HttpMethod.POST, "/providers/**", "/purchase-order-states/**").hasAnyRole(SUPPLY_WRITE_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/providers/**", "/purchase-order-states/**").hasAnyRole(SUPPLY_WRITE_ROLES)
                        .requestMatchers(HttpMethod.DELETE, "/providers/**", "/purchase-order-states/**").hasAnyRole(SUPPLY_WRITE_ROLES)

                        // Gestión CDP y OC.
                        .requestMatchers(HttpMethod.POST, "/cdps/**").hasAnyRole(BUDGET_WRITE_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/cdps/**").hasAnyRole(BUDGET_WRITE_ROLES)
                        .requestMatchers(HttpMethod.DELETE, "/cdps/**").hasAnyRole(BUDGET_WRITE_ROLES)
                        .requestMatchers(HttpMethod.POST, "/purchase-orders/**").hasAnyRole(SUPPLY_WRITE_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/purchase-orders/**").hasAnyRole(SUPPLY_WRITE_ROLES)
                        .requestMatchers(HttpMethod.DELETE, "/purchase-orders/**").hasAnyRole(SUPPLY_WRITE_ROLES)

                        // Seguridad: usuarios y roles solo ADMIN.
                        .requestMatchers("/users/**", "/roles/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Sesión expirada o no autenticada. Ingrese nuevamente.\"}");
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"No cuenta con permisos para realizar esta acción.\"}");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
