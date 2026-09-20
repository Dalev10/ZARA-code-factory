package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.domain.model.EstadoTienda;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.CentroDistribucionEntity;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.TiendaEntity;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.NodoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 * <p>
 * {@code nodo.cd_id} y {@code nodo.tienda_id} tienen FK reales hacia
 * {@code cd}/{@code tienda}, por lo que cada prueba persiste primero el
 * agregado padre correspondiente en lugar de usar un UUID aleatorio.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({NodoMapper.class, NodoRepositoryAdapter.class})
class NodoRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private NodoRepositoryAdapter nodoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID persistirCd(String nombre) {
        CentroDistribucionEntity cd = CentroDistribucionEntity.builder()
                .id(UUID.randomUUID()).nombre(nombre).ubicacion("Bogotá").build();
        return entityManager.persistAndFlush(cd).getId();
    }

    private UUID persistirTienda(String nombre) {
        TiendaEntity tienda = TiendaEntity.builder()
                .id(UUID.randomUUID()).nombre(nombre).ubicacion("Bogotá")
                .estado(EstadoTienda.ACTIVA).creadoEn(Instant.now()).actualizadoEn(Instant.now()).build();
        return entityManager.persistAndFlush(tienda).getId();
    }

    @Test
    void guardaYRecuperaUnNodoDeCdPorId() {
        UUID cdId = persistirCd("CD Nodo Test 1");
        Nodo guardado = nodoRepository.guardar(Nodo.crearParaCd(cdId));
        entityManager.flush();
        entityManager.clear();

        assertThat(nodoRepository.buscarPorId(guardado.getId())).isPresent();
    }

    @Test
    void buscaPorCdId() {
        UUID cdId = persistirCd("CD Nodo Test 2");
        nodoRepository.guardar(Nodo.crearParaCd(cdId));
        entityManager.flush();
        entityManager.clear();

        assertThat(nodoRepository.buscarPorCdId(cdId)).isPresent()
                .get().extracting(Nodo::getTipo).isEqualTo(TipoNodo.CD);
    }

    @Test
    void buscaPorTiendaYTipo() {
        UUID tiendaId = persistirTienda("Tienda Nodo Test 1");
        nodoRepository.guardar(Nodo.crearParaTienda(tiendaId, TipoNodo.ALMACEN));
        entityManager.flush();
        entityManager.clear();

        assertThat(nodoRepository.buscarPorTiendaYTipo(tiendaId, TipoNodo.ALMACEN)).isPresent();
        assertThat(nodoRepository.buscarPorTiendaYTipo(tiendaId, TipoNodo.BODEGA_TIENDA)).isEmpty();
    }

    @Test
    void listarPorTipoFiltraCorrectamente() {
        UUID tiendaBodega = persistirTienda("Tienda Nodo Test 2");
        UUID tiendaAlmacen = persistirTienda("Tienda Nodo Test 3");
        nodoRepository.guardar(Nodo.crearParaTienda(tiendaBodega, TipoNodo.BODEGA_TIENDA));
        nodoRepository.guardar(Nodo.crearParaTienda(tiendaAlmacen, TipoNodo.ALMACEN));
        entityManager.flush();
        entityManager.clear();

        assertThat(nodoRepository.listarPorTipo(TipoNodo.BODEGA_TIENDA, Pageable.unpaged()))
                .allMatch(n -> n.getTipo() == TipoNodo.BODEGA_TIENDA);
    }

    @Test
    void existePorTiendaYTipoDevuelveFalseSiNoExiste() {
        assertThat(nodoRepository.existePorTiendaYTipo(UUID.randomUUID(), TipoNodo.ALMACEN)).isFalse();
    }

    @Test
    void laBaseDeDatosRechazaDosBodegasParaLaMismaTienda() {
        UUID tiendaId = persistirTienda("Tienda Nodo Test 5");
        nodoRepository.guardar(Nodo.crearParaTienda(tiendaId, TipoNodo.BODEGA_TIENDA));
        entityManager.flush();
        entityManager.clear();

        nodoRepository.guardar(Nodo.crearParaTienda(tiendaId, TipoNodo.BODEGA_TIENDA));

        assertThatThrownBy(entityManager::flush).isInstanceOf(RuntimeException.class);
    }

    @Test
    void eliminaUnNodoPorId() {
        UUID tiendaId = persistirTienda("Tienda Nodo Test 4");
        Nodo creado = nodoRepository.guardar(Nodo.crearParaTienda(tiendaId, TipoNodo.BODEGA_TIENDA));
        entityManager.flush();
        entityManager.clear();

        nodoRepository.eliminar(creado.getId());
        entityManager.flush();

        assertThat(nodoRepository.buscarPorId(creado.getId())).isEmpty();
    }
}
