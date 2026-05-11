package cl.dssm.presupuesto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Importante para Railway/frontends externos: permitir preflight CORS.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/docs/**",
                                "/swagger-ui/**",
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/auth/**",
                                "/h2-console/**",
                                "/imports/**",
                                "/dashboard/**",
                                "/programs/**",
                                "/providers/**",
                                "/budget-items/**",
                                "/cdp-types/**",
                                "/purchase-order-states/**",
                                "/budget-subtitles/**",
                                "/cdps/**",
                                "/purchase-orders/**",
                                "/master-data/**",
                                "/audit-logs/**",
                                "/users/**",
                                "/roles/**"
                        ).permitAll()
                        // MVP: abierto mientras validamos Railway/H2. Luego se cambia a authenticated().
                        .anyRequest().permitAll()
                )
                .build();
    }
}
