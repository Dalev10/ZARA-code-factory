package com.codefactory.supplychain.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de Spring Security usada como infraestructura (PasswordEncoder,
 * filter chain, resolución de "quién sos" vía JWT) — la lógica de negocio de
 * autenticación, MFA y tokens vive en identity/application/service, no acoplada
 * a este framework.
 *
 * Desde HU-07: los endpoints de auth (login/refresh/logout), el registro (HU-02,
 * deliberadamente sin proteger todavía) y Swagger quedan públicos; CUALQUIER OTRO
 * endpoint exige un JWT válido (autenticación). Esto es distinto de autorización
 * por scope — "qué puede hacer" un usuario ya autenticado sigue siendo HU-11, que
 * debe reemplazar el bloque de reglas de abajo por chequeos reales de scope.
 *
 * Componentes transversales de tipo 'security', compartidos por todos los módulos.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // TODO(HU-11): sigue abierto porque el guard de autorización por
                        // scope todavía no existe — ver decisión registrada en HU-02.
                        .requestMatchers(HttpMethod.POST, "/api/v1/usuarios").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/login/mfa",
                                "/api/v1/auth/refresh", "/api/v1/auth/logout")
                        .permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
