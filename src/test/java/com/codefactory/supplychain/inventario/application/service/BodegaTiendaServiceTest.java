package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.api.StockResumen;
import com.codefactory.supplychain.inventario.application.dto.BodegaTiendaConsulta;
import com.codefactory.supplychain.inventario.application.port.out.ExistenciasRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.TiendaRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.VariantesCatalogoPort;
import com.codefactory.supplychain.inventario.domain.exception.BodegaTiendaNoEncontradaException;
import com.codefactory.supplychain.inventario.domain.model.EstadoTienda;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import com.codefactory.supplychain.inventario.domain.model.Tienda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BodegaTiendaServiceTest {

    private NodoRepositoryPort nodoRepositoryPort;
    private TiendaRepositoryPort tiendaRepositoryPort;
    private ExistenciasRepositoryPort existencias;
    private VariantesCatalogoPort variantesCatalogo;
    private BodegaTiendaService servicio;

    @BeforeEach
    void setUp() {
        nodoRepositoryPort = mock(NodoRepositoryPort.class);
        tiendaRepositoryPort = mock(TiendaRepositoryPort.class);
        existencias = mock(ExistenciasRepositoryPort.class);
        variantesCatalogo = mock(VariantesCatalogoPort.class);
        servicio = new BodegaTiendaService(nodoRepositoryPort, tiendaRepositoryPort, existencias, variantesCatalogo);
    }

    private static Tienda tiendaActiva(UUID id) {
        Instant ahora = Instant.now();
        return Tienda.reconstruir(id, "Tienda Centro", "Bogotá", EstadoTienda.ACTIVA, ahora, ahora);
    }

    @Test
    void consultarPorIdRechazaUnNodoInexistente() {
        UUID id = UUID.randomUUID();
        when(nodoRepositoryPort.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultarPorId(id)).isInstanceOf(BodegaTiendaNoEncontradaException.class);
    }

    @Test
    void consultarPorIdRechazaUnNodoQueNoEsBodegaTienda() {
        UUID id = UUID.randomUUID();
        when(nodoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(Nodo.crearParaCd(UUID.randomUUID())));

        assertThatThrownBy(() -> servicio.consultarPorId(id)).isInstanceOf(BodegaTiendaNoEncontradaException.class);
    }

    @Test
    void consultarPorIdEnriqueceConTiendaEInventario() {
        UUID tiendaId = UUID.randomUUID();
        Tienda tienda = tiendaActiva(tiendaId);
        Nodo nodo = Nodo.crearParaTienda(tiendaId, TipoNodo.BODEGA_TIENDA);
        StockResumen resumen = new StockResumen(UUID.randomUUID(), UUID.randomUUID(), 10, 10, 0, 0, 10);

        when(nodoRepositoryPort.buscarPorId(nodo.getId())).thenReturn(Optional.of(nodo));
        when(tiendaRepositoryPort.buscarPorId(tiendaId)).thenReturn(Optional.of(tienda));
        when(existencias.listarPorNodo(nodo.getId())).thenReturn(List.of(resumen));
        when(variantesCatalogo.skusPorId(Set.of(resumen.varianteId()))).thenReturn(Map.of(resumen.varianteId(), "SKU-1"));

        BodegaTiendaConsulta consulta = servicio.consultarPorId(nodo.getId());

        assertThat(consulta.nodo()).isEqualTo(nodo);
        assertThat(consulta.tienda()).isEqualTo(tienda);
        assertThat(consulta.inventario()).containsExactly(resumen);
        assertThat(consulta.skus()).containsExactly(Map.entry(resumen.varianteId(), "SKU-1"));
    }

    @Test
    void listarTodasTraeLaTiendaDeCadaBodegaEnUnaSolaConsulta() {
        UUID tiendaId = UUID.randomUUID();
        Tienda tienda = tiendaActiva(tiendaId);
        Nodo nodo = Nodo.crearParaTienda(tiendaId, TipoNodo.BODEGA_TIENDA);
        Pageable pageable = Pageable.unpaged();
        when(nodoRepositoryPort.listarPorTipo(TipoNodo.BODEGA_TIENDA, pageable))
                .thenReturn(new PageImpl<>(List.of(nodo)));
        when(tiendaRepositoryPort.buscarPorIds(Set.of(tiendaId))).thenReturn(List.of(tienda));

        assertThat(servicio.listarTodas(pageable))
                .containsExactly(new BodegaTiendaConsulta(nodo, tienda, List.of(), Map.of()));
    }

}
