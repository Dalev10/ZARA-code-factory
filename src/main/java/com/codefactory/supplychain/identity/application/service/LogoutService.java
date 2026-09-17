package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.LogoutComando;
import com.codefactory.supplychain.identity.application.port.in.LogoutUseCase;
import com.codefactory.supplychain.identity.application.port.out.RefreshTokenRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Revoca únicamente el refresh token presentado — nunca toda su familia (eso es
 * exclusivo de la detección de reuse en HU-05, que responde a un robo, no a un
 * cierre de sesión legítimo). Idempotente: no falla ni informa nada distinto si
 * el token ya no existe o ya estaba revocado, para no filtrar información sobre
 * el estado de una sesión ajena.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;

    @Override
    public void logout(LogoutComando comando) {
        String hash = RefreshTokenSupport.sha256Hex(comando.refreshTokenValor());
        refreshTokenRepositoryPort.buscarPorTokenHash(hash)
                .filter(token -> !token.estaRevocado())
                .ifPresent(token -> refreshTokenRepositoryPort.guardar(token.revocar(Instant.now())));
    }
}
