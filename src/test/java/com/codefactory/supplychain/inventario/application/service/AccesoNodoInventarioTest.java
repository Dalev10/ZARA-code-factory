package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.AdministracionInventarioNoPermitidaException;
import com.codefactory.supplychain.inventario.domain.exception.NodoNoEncontradoException;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccesoNodoInventarioTest {

    private NodoRepositoryPort nodoRepository;
    private AccesoNodoInventario acceso;

    private final UUID tiendaId = UUID.randomUUID();
    private final Nodo almacen = Nodo.reconstruir(UUID.randomUUID(), TipoNodo.ALMACEN, null, tiendaId);
    private final Nodo cd = Nodo.reconstruir(UUID.randomUUID(), TipoNodo.CD, UUID.randomUUID(), null);

    @BeforeEach
    void setUp() {
        nodoRepository = mock(NodoRepositoryPort.class);
        when(nodoRepository.buscarPorId(almacen.getId())).thenReturn(Optional.of(almacen));
        when(nodoRepository.buscarPorId(cd.getId())).thenReturn(Optional.of(cd));
        acceso = new AccesoNodoInventario(nodoRepository);
    }

    private static UsuarioActual usuario(Set<String> scopes, UUID... tiendas) {
        return new UsuarioActual(UUID.randomUUID(), scopes, Set.of(tiendas));
    }

    @Test
    void unNodoInexistenteRespondeNoEncontrado() {
        UUID nodoId = UUID.randomUUID();
        when(nodoRepository.buscarPorId(nodoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acceso.exigirAdministracion(
                nodoId, usuario(Set.of("inventario:administrar"), tiendaId)))
                .isInstanceOf(NodoNoEncontradoException.class);
    }

    @Test
    void unCdExigeElScopeDeInventarioCd() {
        assertThatThrownBy(() -> acceso.exigirAdministracion(
                cd.getId(), usuario(Set.of("inventario:administrar", "tiendas:operar-todas"))))
                .isInstanceOf(AdministracionInventarioNoPermitidaException.class);

        assertThat(acceso.exigirAdministracion(cd.getId(), usuario(Set.of("inventario-cd:administrar"))))
                .isEqualTo(cd);
    }

    @Test
    void unNodoDeTiendaFueraDeAlcanceRespondeNoEncontradoAunqueTengaElScope() {
        assertThatThrownBy(() -> acceso.exigirAdministracion(
                almacen.getId(), usuario(Set.of("inventario:administrar"), UUID.randomUUID())))
                .isInstanceOf(NodoNoEncontradoException.class);
    }

    @Test
    void unNodoDeTiendaEnAlcanceExigeElScopeDeInventario() {
        assertThatThrownBy(() -> acceso.exigirAdministracion(
                almacen.getId(), usuario(Set.of("inventario-cd:administrar"), tiendaId)))
                .isInstanceOf(AdministracionInventarioNoPermitidaException.class);

        assertThat(acceso.exigirAdministracion(
                almacen.getId(), usuario(Set.of("inventario:administrar"), tiendaId)))
                .isEqualTo(almacen);
    }

    @Test
    void elAccesoGlobalPermiteOperarCualquierTienda() {
        assertThat(acceso.exigirAdministracion(
                almacen.getId(), usuario(Set.of("inventario:administrar", "tiendas:operar-todas"))))
                .isEqualTo(almacen);
    }
}
