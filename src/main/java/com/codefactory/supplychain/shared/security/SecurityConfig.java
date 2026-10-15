package com.codefactory.supplychain.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

/**
 * Configuración de Spring Security usada como infraestructura (PasswordEncoder,
 * filter chain, resolución de "quién sos" vía JWT) — la lógica de negocio de
 * autenticación, MFA y tokens vive en identity/application/service, no acoplada
 * a este framework.
 *
 * Los endpoints de auth (login/refresh/logout), Swagger y el health check de
 * Actuator quedan públicos; CUALQUIER OTRO endpoint exige un JWT válido
 * (autenticación). "Qué puede hacer"
 * un usuario ya autenticado (autorización) es responsabilidad de cada controller
 * vía @PreAuthorize("hasAuthority('scope:codigo')") — ver JwtAuthenticationFilter,
 * que resuelve esas autoridades desde los scopes reales del usuario (HU-11).
 *
 * Componentes transversales de tipo 'security', compartidos por todos los módulos.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String CUERPO_NO_AUTENTICADO = "{\"mensaje\":\"Se requiere autenticación\"}";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(errores -> errores.authenticationEntryPoint(respuestaNoAutenticado()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/login/mfa",
                                "/api/v1/auth/refresh", "/api/v1/auth/logout")
                        .permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    /**
     * 401 para todo request protegido sin un usuario autenticado: sin token, token inválido
     * o vencido, o usuario que ya no está ACTIVO. Es la misma respuesta para los cuatro
     * motivos a propósito, para no revelar si un usuario existe ni en qué estado está. Sin
     * esto Spring responde 403, que el cliente no puede distinguir de "sin permiso" (403 con
     * cuerpo, ver GlobalExceptionHandler) y no sabría cuándo refrescar el token.
     */
    private static AuthenticationEntryPoint respuestaNoAutenticado() {
        return (request, response, excepcion) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(CUERPO_NO_AUTENTICADO);
        };
    }
}
