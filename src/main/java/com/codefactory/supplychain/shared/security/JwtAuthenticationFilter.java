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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Resuelve el usuario autenticado a partir del JWT del header "Authorization: Bearer",
 * y sus autoridades (los scopes que le otorgan sus roles, ver ContextoAutorizacionPort) para
 * que el guard genérico de HU-11 (@PreAuthorize("hasAuthority('...')")) funcione en
 * cualquier endpoint. Los scopes se resuelven contra la base en cada request — nunca
 * se cachean en el JWT — para que quitarle un scope a un rol aplique de inmediato, sin
 * esperar a que expire el access token.
 *
 * El principal que queda en el SecurityContext es un UsuarioActual (id + scopes), no un
 * objeto de dominio del módulo identity. Si el usuario no existe o no está ACTIVO, el
 * request sigue sin autenticar y SecurityConfig responde 401.
 *
 * Componentes transversales de tipo 'security', compartidos por todos los módulos.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey clave;
    private final ContextoAutorizacionPort contextoAutorizacionPort;

    public JwtAuthenticationFilter(@Value("${app.security.jwt.secret}") String secreto,
                                    ContextoAutorizacionPort contextoAutorizacionPort) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.contextoAutorizacionPort = contextoAutorizacionPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            try {
                Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
                // Un token de otro tipo (ej. el desafío de MFA de vida corta) NUNCA debe
                // servir como access token, aunque esté firmado correctamente.
                if (!JwtClaimTypes.TIPO_ACCESO.equals(claims.get(JwtClaimTypes.CLAIM_TIPO, String.class))) {
                    SecurityContextHolder.clearContext();
                } else {
                    UUID usuarioId = UUID.fromString(claims.getSubject());
                    contextoAutorizacionPort.resolver(usuarioId).ifPresentOrElse(
                            this::autenticar, SecurityContextHolder::clearContext);
                }
            } catch (JwtException | IllegalArgumentException excepcionTokenInvalido) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    private void autenticar(UsuarioActual usuario) {
        List<GrantedAuthority> autoridades = usuario.scopes().stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, autoridades));
    }
}
