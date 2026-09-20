package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.domain.model.EstadoTienda;
import com.codefactory.supplychain.inventario.domain.model.Tienda;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.TiendaMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TiendaMapper.class, TiendaRepositoryAdapter.class})
class TiendaRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TiendaRepositoryAdapter tiendaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void guardaYRecuperaUnaTiendaPorId() {
        Tienda guardada = tiendaRepository.guardar(Tienda.crear("Tienda Centro", "Bogotá"));
        entityManager.flush();
        entityManager.clear();

        Tienda recuperada = tiendaRepository.buscarPorId(guardada.getId()).orElseThrow();
        assertThat(recuperada.getNombre()).isEqualTo("Tienda Centro");
        assertThat(recuperada.getEstado()).isEqualTo(EstadoTienda.ACTIVA);
        assertThat(recuperada.getCreadoEn()).isNotNull();
    }

    @Test
    void listarTodasIncluyeLasTiendasGuardadas() {
        tiendaRepository.guardar(Tienda.crear("Tienda A", null));
        tiendaRepository.guardar(Tienda.crear("Tienda B", null));
        entityManager.flush();

        assertThat(tiendaRepository.listarTodas()).extracting(Tienda::getNombre).contains("Tienda A", "Tienda B");
    }

    @Test
    void existePorNombreDevuelveFalseSiNoExiste() {
        assertThat(tiendaRepository.existePorNombre("No Existe")).isFalse();
    }

    @Test
    void laBaseDeDatosRechazaNombresDuplicados() {
        tiendaRepository.guardar(Tienda.crear("Tienda Duplicada", null));
        entityManager.flush();
        entityManager.clear();

        tiendaRepository.guardar(Tienda.crear("Tienda Duplicada", null));

        assertThatThrownBy(entityManager::flush).isInstanceOf(RuntimeException.class);
    }

    @Test
    void guardarUnaTiendaYaExistenteLaActualiza() {
        Tienda creada = tiendaRepository.guardar(Tienda.crear("Tienda Centro", "Bogotá"));
        entityManager.flush();
        entityManager.clear();

        Tienda desactivada = creada.desactivar();
        tiendaRepository.guardar(desactivada);
        entityManager.flush();
        entityManager.clear();

        assertThat(tiendaRepository.buscarPorId(creada.getId())).get()
                .extracting(Tienda::getEstado).isEqualTo(EstadoTienda.INACTIVA);
    }
}
