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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
                        .requestMatchers("/dashboard/**").hasAnyRole("ADMIN", "DIRECCION", "PRESUPUESTO", "ABASTECIMIENTO", "LECTURA")
                        .requestMatchers("/imports/**", "/reports/**").hasAnyRole("ADMIN", "DIRECCION", "PRESUPUESTO")
                        .requestMatchers("/purchase-orders/**").hasAnyRole("ADMIN", "ABASTECIMIENTO", "PRESUPUESTO")
                        .requestMatchers("/cdps/**").hasAnyRole("ADMIN", "PRESUPUESTO", "ABASTECIMIENTO")
                        .requestMatchers("/users/**", "/roles/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
