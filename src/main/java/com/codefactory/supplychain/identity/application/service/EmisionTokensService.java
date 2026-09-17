package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.LoginResultado;
import com.codefactory.supplychain.identity.application.port.out.AccessTokenGeneratorPort;
import com.codefactory.supplychain.identity.application.port.out.RefreshTokenRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.RefreshToken;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Emite el access token + refresh token para un usuario cuyas credenciales (y,
 * si aplica, su segundo factor) ya fueron validadas por completo. Extraído acá
 * porque tanto LoginService (login sin MFA) como CompletarLoginMfaService
 * (segundo paso del login con MFA) necesitan exactamente esta misma lógica.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Service
class EmisionTokensService {

    private final AccessTokenGeneratorPort accessTokenGeneratorPort;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final long refreshTokenTtlDias;

    EmisionTokensService(AccessTokenGeneratorPort accessTokenGeneratorPort,
                          RefreshTokenRepositoryPort refreshTokenRepositoryPort,
                          @Value("${app.security.jwt.refresh-token-ttl-dias}") long refreshTokenTtlDias) {
        this.accessTokenGeneratorPort = accessTokenGeneratorPort;
        this.refreshTokenRepositoryPort = refreshTokenRepositoryPort;
        this.refreshTokenTtlDias = refreshTokenTtlDias;
    }

    LoginResultado.Completado emitirParaUsuarioAutenticado(Usuario usuario) {
        String accessToken = accessTokenGeneratorPort.generar(usuario);

        String refreshTokenValor = HashingSupport.generarValorAleatorio();
        Instant ahora = Instant.now();
        Instant expiraEn = ahora.plus(Duration.ofDays(refreshTokenTtlDias));
        RefreshToken refreshToken = RefreshToken.crearNuevaFamilia(usuario.getId(),
                HashingSupport.sha256Hex(refreshTokenValor), ahora, expiraEn);
        refreshTokenRepositoryPort.guardar(refreshToken);

        return new LoginResultado.Completado(accessToken, refreshTokenValor, expiraEn, usuario);
    }
}
