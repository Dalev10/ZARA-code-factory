package com.codefactory.supplychain.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de Spring Security usada únicamente como infraestructura
 * (PasswordEncoder, filter chain) — la lógica de negocio de autenticación,
 * MFA y tokens vive en identity/application/service, no acoplada a este framework.
 *
 * TODO(HU-11): hoy todos los endpoints quedan abiertos (permitAll) porque ni
 * la autenticación (HU-03) ni el guard de autorización por scope (HU-11)
 * existen todavía. HU-11 debe reemplazar authorizeHttpRequests por reglas
 * reales basadas en scopes.
 *
 * Componentes transversales de tipo 'security', compartidos por todos los módulos.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
