package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.ActualizarScopeComando;
import com.codefactory.supplychain.identity.application.port.in.CrearScopeComando;
import com.codefactory.supplychain.identity.application.port.out.RolScopeRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.ScopeRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.ScopeEnUsoException;
import com.codefactory.supplychain.identity.domain.exception.ScopeNoEncontradoException;
import com.codefactory.supplychain.identity.domain.exception.ScopeYaExistenteException;
import com.codefactory.supplychain.identity.domain.model.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScopeServiceTest {

    private ScopeRepositoryPort scopeRepositoryPort;
    private RolScopeRepositoryPort rolScopeRepositoryPort;
    private ScopeService servicio;

    @BeforeEach
    void setUp() {
        scopeRepositoryPort = mock(ScopeRepositoryPort.class);
        rolScopeRepositoryPort = mock(RolScopeRepositoryPort.class);
        servicio = new ScopeService(scopeRepositoryPort, rolScopeRepositoryPort);
    }

    @Test
    void creaUnScopeNuevo() {
        when(scopeRepositoryPort.existePorCodigo("usuarios:leer")).thenReturn(false);
        when(scopeRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Scope scope = servicio.crear(new CrearScopeComando("usuarios:leer", "desc", false));

        assertThat(scope.getCodigo()).isEqualTo("usuarios:leer");
    }

    @Test
    void rechazaCrearUnScopeConCodigoDuplicado() {
        when(scopeRepositoryPort.existePorCodigo("usuarios:leer")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(new CrearScopeComando("usuarios:leer", "desc", false)))
                .isInstanceOf(ScopeYaExistenteException.class);

        verify(scopeRepositoryPort, never()).guardar(any());
    }

    @Test
    void actualizaUnScopeExistente() {
        UUID scopeId = UUID.randomUUID();
        Scope existente = Scope.reconstruir(scopeId, "usuarios:leer", "vieja", false, Instant.now());
        when(scopeRepositoryPort.buscarPorId(scopeId)).thenReturn(Optional.of(existente));
        when(scopeRepositoryPort.existePorCodigo("usuarios:leer:v2")).thenReturn(false);
        when(scopeRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Scope actualizado = servicio.actualizar(
                new ActualizarScopeComando(scopeId, "usuarios:leer:v2", "nueva", true));

        assertThat(actualizado.getCodigo()).isEqualTo("usuarios:leer:v2");
        assertThat(actualizado.isSensible()).isTrue();
    }

    @Test
    void actualizarRechazaUnCodigoYaUsadoPorOtroScope() {
        UUID scopeId = UUID.randomUUID();
        Scope existente = Scope.reconstruir(scopeId, "usuarios:leer", "desc", false, Instant.now());
        when(scopeRepositoryPort.buscarPorId(scopeId)).thenReturn(Optional.of(existente));
        when(scopeRepositoryPort.existePorCodigo("usuarios:escribir")).thenReturn(true);

        assertThatThrownBy(() -> servicio.actualizar(
                new ActualizarScopeComando(scopeId, "usuarios:escribir", "desc", false)))
                .isInstanceOf(ScopeYaExistenteException.class);
    }

    @Test
    void actualizarRechazaUnScopeInexistente() {
        UUID scopeId = UUID.randomUUID();
        when(scopeRepositoryPort.buscarPorId(scopeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(new ActualizarScopeComando(scopeId, "x", "y", false)))
                .isInstanceOf(ScopeNoEncontradoException.class);
    }

    @Test
    void eliminaUnScopeSinRolesAsignados() {
        UUID scopeId = UUID.randomUUID();
        when(scopeRepositoryPort.buscarPorId(scopeId))
                .thenReturn(Optional.of(Scope.reconstruir(scopeId, "x", null, false, Instant.now())));
        when(rolScopeRepositoryPort.tieneRolesAsignados(scopeId)).thenReturn(false);

        servicio.eliminar(scopeId);

        verify(scopeRepositoryPort).eliminar(scopeId);
    }

    @Test
    void rechazaEliminarUnScopeAsignadoAAlgunRol() {
        UUID scopeId = UUID.randomUUID();
        when(scopeRepositoryPort.buscarPorId(scopeId))
                .thenReturn(Optional.of(Scope.reconstruir(scopeId, "x", null, false, Instant.now())));
        when(rolScopeRepositoryPort.tieneRolesAsignados(scopeId)).thenReturn(true);

        assertThatThrownBy(() -> servicio.eliminar(scopeId)).isInstanceOf(ScopeEnUsoException.class);

        verify(scopeRepositoryPort, never()).eliminar(any());
    }

    @Test
    void rechazaEliminarUnScopeInexistente() {
        UUID scopeId = UUID.randomUUID();
        when(scopeRepositoryPort.buscarPorId(scopeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(scopeId)).isInstanceOf(ScopeNoEncontradoException.class);
    }
}
