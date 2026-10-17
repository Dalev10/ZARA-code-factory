package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.out.InventarioRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.VarianteNoCargadaException;
import com.codefactory.supplychain.inventario.domain.model.Inventario;
import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;
import com.codefactory.supplychain.inventario.domain.model.TipoMovimientoInventario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventarioServiceTest {

    private InventarioRepositoryPort repository;
    private InventarioService servicio;

    @BeforeEach
    void setUp() {
        repository = mock(InventarioRepositoryPort.class);
        servicio = new InventarioService(repository);
    }

    @Test
    void rechazaOperarUnaVarianteNoCargada() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.aplicarVenta(nodoId, varianteId, 1, false))
                .isInstanceOf(VarianteNoCargadaException.class);
    }

    @Test
    void buscaLaFilaAntesDeAplicarLaVentaYGuardaElAgregado() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(5);
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.of(inventario));

        MovimientoInventario movimiento = servicio.aplicarVenta(nodoId, varianteId, 2, false);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.VENTA);
        assertThat(inventario.getALaMano()).isEqualTo(3);
        verify(repository).guardar(same(inventario));
    }

    @Test
    void aplicaReposicionSoloSobreUnaFilaExistente() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        when(repository.buscarPorNodoYVariante(nodoId, varianteId)).thenReturn(Optional.of(inventario));

        MovimientoInventario movimiento = servicio.reponer(nodoId, varianteId, 4);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.REPOSICION);
        verify(repository).guardar(same(inventario));
    }
}
