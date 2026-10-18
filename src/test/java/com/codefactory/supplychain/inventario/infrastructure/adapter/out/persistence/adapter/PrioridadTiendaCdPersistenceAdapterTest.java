package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.domain.model.PrioridadTiendaCd;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.PrioridadTiendaCdMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PrioridadTiendaCdMapper.class, PrioridadTiendaCdPersistenceAdapter.class})
class PrioridadTiendaCdPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PrioridadTiendaCdPersistenceAdapter repositorio;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID crearTienda() {
        return jdbc.queryForObject(
                "INSERT INTO tienda (nombre, ubicacion, estado) VALUES (?, 'Loc', 'ACTIVA') RETURNING id",
                UUID.class, "Tienda-" + UUID.randomUUID());
    }

    private UUID crearCd() {
        return jdbc.queryForObject(
                "INSERT INTO cd (nombre, ubicacion) VALUES (?, 'Loc') RETURNING id",
                UUID.class, "CD-" + UUID.randomUUID());
    }

    @Test
    void guardarYListarPrioridadesEnOrden() {
        UUID tienda = crearTienda();
        UUID cd1 = crearCd();
        UUID cd2 = crearCd();

        repositorio.guardarTodas(List.of(
                PrioridadTiendaCd.crear(tienda, cd1, 1),
                PrioridadTiendaCd.crear(tienda, cd2, 2)));
        entityManager.flush();
        entityManager.clear();

        List<PrioridadTiendaCd> lista = repositorio.listarPorTienda(tienda);
        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).getCdId()).isEqualTo(cd1);
        assertThat(lista.get(0).getOrdenPrioridad()).isEqualTo(1);
        assertThat(lista.get(1).getOrdenPrioridad()).isEqualTo(2);
    }

    @Test
    void eliminarPorTiendaBorraSoloSusPrioridades() {
        UUID tiendaA = crearTienda();
        UUID tiendaB = crearTienda();
        UUID cd = crearCd();
        repositorio.guardarTodas(List.of(PrioridadTiendaCd.crear(tiendaA, cd, 1)));
        repositorio.guardarTodas(List.of(PrioridadTiendaCd.crear(tiendaB, cd, 1)));
        entityManager.flush();

        repositorio.eliminarPorTienda(tiendaA);
        entityManager.flush();
        entityManager.clear();

        assertThat(repositorio.listarPorTienda(tiendaA)).isEmpty();
        assertThat(repositorio.listarPorTienda(tiendaB)).hasSize(1);
    }

    @Test
    void existePorCdRetornaVerdaderoCuandoHayPrioridad() {
        UUID tienda = crearTienda();
        UUID cd = crearCd();
        repositorio.guardarTodas(List.of(PrioridadTiendaCd.crear(tienda, cd, 1)));
        entityManager.flush();

        assertThat(repositorio.existePorCd(cd)).isTrue();
        assertThat(repositorio.existePorCd(UUID.randomUUID())).isFalse();
    }

    @Test
    void laBaseRechazaOrdenCeroONegativo() {
        UUID tienda = crearTienda();
        UUID cd = crearCd();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO tienda_cd_prioridad (id, tienda_id, cd_id, orden_prioridad) VALUES (?,?,?,0)",
                UUID.randomUUID(), tienda, cd))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseRechazaMismaTiendaYCdConOrdenDiferente() {
        UUID tienda = crearTienda();
        UUID cd = crearCd();
        jdbc.update("INSERT INTO tienda_cd_prioridad (id, tienda_id, cd_id, orden_prioridad) VALUES (?,?,?,1)",
                UUID.randomUUID(), tienda, cd);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO tienda_cd_prioridad (id, tienda_id, cd_id, orden_prioridad) VALUES (?,?,?,2)",
                UUID.randomUUID(), tienda, cd))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
