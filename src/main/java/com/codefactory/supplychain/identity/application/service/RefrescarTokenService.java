package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.RefrescarTokenComando;
import com.codefactory.supplychain.identity.application.port.in.RefrescarTokenResultado;
import com.codefactory.supplychain.identity.application.port.in.RefrescarTokenUseCase;
import com.codefactory.supplychain.identity.application.port.out.AccessTokenGeneratorPort;
import com.codefactory.supplychain.identity.application.port.out.RefreshTokenRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.TokenInvalidoException;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.RefreshToken;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Implementa rotación con detección de reuse: cada refresh consume el token
 * presentado (lo revoca) y emite uno nuevo en la misma familia. Si el token
 * presentado YA estaba revocado (es decir, ya se había usado antes), se asume
 * que fue robado y se revoca la familia completa — tanto el token del atacante
 * como cualquiera que el usuario legítimo tuviera vigente quedan invalidados,
 * forzando un nuevo login.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Service
public class RefrescarTokenService implements RefrescarTokenUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final AccessTokenGeneratorPort accessTokenGeneratorPort;
    private final long refreshTokenTtlDias;

    public RefrescarTokenService(RefreshTokenRepositoryPort refreshTokenRepositoryPort,
                                  UsuarioRepositoryPort usuarioRepositoryPort,
                                  AccessTokenGeneratorPort accessTokenGeneratorPort,
                                  @Value("${app.security.jwt.refresh-token-ttl-dias}") long refreshTokenTtlDias) {
        this.refreshTokenRepositoryPort = refreshTokenRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.accessTokenGeneratorPort = accessTokenGeneratorPort;
        this.refreshTokenTtlDias = refreshTokenTtlDias;
    }

    @Override
    public RefrescarTokenResultado refrescar(RefrescarTokenComando comando) {
        Instant ahora = Instant.now();
        String hashPresentado = HashingSupport.sha256Hex(comando.refreshTokenValor());

        RefreshToken tokenPresentado = refreshTokenRepositoryPort.buscarPorTokenHash(hashPresentado)
                .orElseThrow(TokenInvalidoException::new);

        if (tokenPresentado.estaRevocado()) {
            refreshTokenRepositoryPort.revocarFamilia(tokenPresentado.getFamiliaId(), ahora);
            throw new TokenInvalidoException();
        }

        if (tokenPresentado.estaExpirado(ahora)) {
            throw new TokenInvalidoException();
        }

        Usuario usuario = usuarioRepositoryPort.buscarPorId(tokenPresentado.getUsuarioId())
                .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO)
                .orElseThrow(TokenInvalidoException::new);

        // Rotación: se revoca el token presentado y se emite uno nuevo en la misma familia.
        refreshTokenRepositoryPort.guardar(tokenPresentado.revocar(ahora));

        String nuevoValor = HashingSupport.generarValorAleatorio();
        Instant nuevaExpiracion = ahora.plus(Duration.ofDays(refreshTokenTtlDias));
        RefreshToken nuevoToken = RefreshToken.crearRotado(usuario.getId(), tokenPresentado.getFamiliaId(),
                HashingSupport.sha256Hex(nuevoValor), ahora, nuevaExpiracion);
        refreshTokenRepositoryPort.guardar(nuevoToken);

        String accessToken = accessTokenGeneratorPort.generar(usuario);

        return new RefrescarTokenResultado(accessToken, nuevoValor, nuevaExpiracion);
    }
}
