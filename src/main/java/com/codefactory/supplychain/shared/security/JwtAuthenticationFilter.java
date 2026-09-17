package com.codefactory.supplychain.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Resuelve el usuario autenticado a partir del JWT del header "Authorization: Bearer".
 * Esto es AUTENTICACIÓN (quién sos), no autorización por scope (qué podés hacer) —
 * eso último sigue siendo responsabilidad de HU-11. Si el token falta, es inválido o
 * expiró, la petición sigue como anónima; será rechazada más adelante por
 * authorizeHttpRequests únicamente si el endpoint exige estar autenticado.
 *
 * El principal que queda en el SecurityContext es el UUID del usuario (el "sub" del
 * JWT), no un objeto de dominio completo — evita que este filtro transversal dependa
 * del módulo identity para resolver un Usuario.
 *
 * Componentes transversales de tipo 'security', compartidos por todos los módulos.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey clave;

    public JwtAuthenticationFilter(@Value("${app.security.jwt.secret}") String secreto) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            try {
                Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
                UUID usuarioId = UUID.fromString(claims.getSubject());
                var authentication = new UsernamePasswordAuthenticationToken(usuarioId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException excepcionTokenInvalido) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
