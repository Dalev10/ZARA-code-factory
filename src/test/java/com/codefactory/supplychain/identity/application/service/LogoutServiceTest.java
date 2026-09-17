package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.LogoutComando;
import com.codefactory.supplychain.identity.application.port.out.RefreshTokenRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogoutServiceTest {

    private RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private LogoutService servicio;

    @BeforeEach
    void setUp() {
        refreshTokenRepositoryPort = mock(RefreshTokenRepositoryPort.class);
        servicio = new LogoutService(refreshTokenRepositoryPort);
    }

    @Test
    void revocaElTokenActivoPresentado() {
        RefreshToken tokenActivo = RefreshToken.crearNuevaFamilia(UUID.randomUUID(), "hash-cualquiera",
                Instant.now(), Instant.now().plusSeconds(3600));
        when(refreshTokenRepositoryPort.buscarPorTokenHash(any())).thenReturn(Optional.of(tokenActivo));

        servicio.logout(new LogoutComando("valor-de-prueba"));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepositoryPort).guardar(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(tokenActivo.getId());
        assertThat(captor.getValue().estaRevocado()).isTrue();
    }

    @Test
    void noHaceNadaSiElTokenYaEstabaRevocado() {
        Instant ahora = Instant.now();
        RefreshToken tokenYaRevocado = RefreshToken.crearNuevaFamilia(UUID.randomUUID(), "hash-cualquiera",
                ahora, ahora.plusSeconds(3600)).revocar(ahora);
        when(refreshTokenRepositoryPort.buscarPorTokenHash(any())).thenReturn(Optional.of(tokenYaRevocado));

        servicio.logout(new LogoutComando("valor-de-prueba"));

        verify(refreshTokenRepositoryPort, never()).guardar(any());
    }

    @Test
    void noFallaSiElTokenNoExiste() {
        when(refreshTokenRepositoryPort.buscarPorTokenHash(any())).thenReturn(Optional.empty());

        servicio.logout(new LogoutComando("no-existe"));

        verify(refreshTokenRepositoryPort, never()).guardar(any());
    }

    @Test
    void noRevocaLaFamiliaCompletaSoloElTokenPresentado() {
        RefreshToken tokenActivo = RefreshToken.crearNuevaFamilia(UUID.randomUUID(), "hash-cualquiera",
                Instant.now(), Instant.now().plusSeconds(3600));
        when(refreshTokenRepositoryPort.buscarPorTokenHash(any())).thenReturn(Optional.of(tokenActivo));

        servicio.logout(new LogoutComando("valor-de-prueba"));

        verify(refreshTokenRepositoryPort, never()).revocarFamilia(any(), any());
    }
}
