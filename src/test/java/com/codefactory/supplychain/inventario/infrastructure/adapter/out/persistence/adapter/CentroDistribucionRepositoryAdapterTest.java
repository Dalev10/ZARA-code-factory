package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.CentroDistribucionMapper;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CentroDistribucionMapper.class, CentroDistribucionRepositoryAdapter.class})
class CentroDistribucionRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CentroDistribucionRepositoryAdapter centroDistribucionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void guardaYRecuperaUnCdPorId() {
        CentroDistribucion guardado = centroDistribucionRepository.guardar(
                CentroDistribucion.crear("CD Principal", "Bogotá"));
        entityManager.flush();
        entityManager.clear();

        assertThat(centroDistribucionRepository.buscarPorId(guardado.getId())).isPresent();
    }

    @Test
    void buscaPorNombre() {
        centroDistribucionRepository.guardar(CentroDistribucion.crear("CD Único", "Cali"));
        entityManager.flush();
        entityManager.clear();

        assertThat(centroDistribucionRepository.buscarPorNombre("CD Único")).isPresent();
    }

    @Test
    void buscarPorNombreParcialUsandoElFiltro() {
        centroDistribucionRepository.guardar(CentroDistribucion.crear("CD Norte Grande", "Bogotá"));
        entityManager.flush();
        entityManager.clear();

        assertThat(centroDistribucionRepository.buscar(null, "norte", null))
                .extracting(CentroDistribucion::getNombre).contains("CD Norte Grande");
    }

    @Test
    void eliminaUnCdSinNodoAsociado() {
        CentroDistribucion creado = centroDistribucionRepository.guardar(
                CentroDistribucion.crear("CD A Eliminar", null));
        entityManager.flush();
        entityManager.clear();

        centroDistribucionRepository.eliminar(creado.getId());
        entityManager.flush();

        assertThat(centroDistribucionRepository.buscarPorId(creado.getId())).isEmpty();
    }

    @Test
    void buscarPorIdInexistenteDevuelveVacio() {
        assertThat(centroDistribucionRepository.buscarPorId(UUID.randomUUID())).isEmpty();
    }
}
