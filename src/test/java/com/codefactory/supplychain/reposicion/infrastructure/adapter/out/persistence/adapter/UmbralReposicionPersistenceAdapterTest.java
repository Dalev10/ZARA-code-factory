package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.reposicion.domain.model.UmbralReposicion;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.mapper.UmbralReposicionPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Módulo: reposicion — Umbrales de reposición interna (FEAT-09). Verifica el mapeo contra
 * la tabla real y los CHECK / UNIQUE de V21 (D-51, D-53, D-54).
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UmbralReposicionPersistenceMapper.class, UmbralReposicionPersistenceAdapter.class})
class UmbralReposicionPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UmbralReposicionPersistenceAdapter repositorio;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID crearCategoria() {
        return jdbc.queryForObject("INSERT INTO categoria (nombre) VALUES (?) RETURNING id", UUID.class,
                "Categoria-" + UUID.randomUUID());
    }

    private UUID crearVariante() {
        UUID categoria = crearCategoria();
        UUID template = jdbc.queryForObject(
                "INSERT INTO template (categoria_id, nombre) VALUES (?, ?) RETURNING id", UUID.class,
                categoria, "Template-" + UUID.randomUUID());
        return jdbc.queryForObject(
                "INSERT INTO variante (template_id, sku) VALUES (?, ?) RETURNING id", UUID.class,
                template, "SKU-" + UUID.randomUUID());
    }

    @Test
    void guardaYRecuperaUnUmbralPorVariante() {
        UUID variante = crearVariante();
        UmbralReposicion guardado = repositorio.guardar(UmbralReposicion.crear(variante, null, 3, 6));
        entityManager.flush();
        entityManager.clear();

        UmbralReposicion recuperado = repositorio.buscarPorId(guardado.getId()).orElseThrow();

        assertThat(recuperado.getVarianteId()).isEqualTo(variante);
        assertThat(recuperado.getCategoriaId()).isNull();
        assertThat(recuperado.getCantidadMinima()).isEqualTo(3);
        assertThat(recuperado.getCantidadObjetivo()).isEqualTo(6);
    }

    @Test
    void guardaYRecuperaUnUmbralPorCategoria() {
        UUID categoria = crearCategoria();
        repositorio.guardar(UmbralReposicion.crear(null, categoria, 2, 5));
        entityManager.flush();
        entityManager.clear();

        assertThat(repositorio.buscarPorCategoria(categoria)).isPresent()
                .get().extracting(UmbralReposicion::getCantidadObjetivo).isEqualTo(5);
        assertThat(repositorio.existePorCategoria(categoria)).isTrue();
        assertThat(repositorio.existePorVariante(categoria)).isFalse();
    }

    @Test
    void buscarPorVarianteEncuentraSoloElUmbralDeEsaVariante() {
        UUID variante = crearVariante();
        repositorio.guardar(UmbralReposicion.crear(variante, null, 3, 6));
        entityManager.flush();
        entityManager.clear();

        assertThat(repositorio.buscarPorVariante(variante)).isPresent();
        assertThat(repositorio.buscarPorVariante(UUID.randomUUID())).isEmpty();
    }

    @Test
    void modificarCantidadesPersisteElCambioSinCrearOtraFila() {
        UUID variante = crearVariante();
        UmbralReposicion guardado = repositorio.guardar(UmbralReposicion.crear(variante, null, 3, 6));
        entityManager.flush();
        entityManager.clear();

        repositorio.guardar(guardado.cambiarCantidades(8, 12));
        entityManager.flush();
        entityManager.clear();

        UmbralReposicion recuperado = repositorio.buscarPorId(guardado.getId()).orElseThrow();
        assertThat(recuperado.getCantidadMinima()).isEqualTo(8);
        assertThat(recuperado.getCantidadObjetivo()).isEqualTo(12);
        assertThat(repositorio.existePorVariante(variante)).isTrue();
    }

    @Test
    void eliminarBorraElUmbral() {
        UUID variante = crearVariante();
        UmbralReposicion guardado = repositorio.guardar(UmbralReposicion.crear(variante, null, 3, 6));
        entityManager.flush();

        repositorio.eliminarPorId(guardado.getId());
        entityManager.flush();

        assertThat(repositorio.existePorId(guardado.getId())).isFalse();
    }

    @Test
    void listarIncluyeLosUmbralesGuardados() {
        UmbralReposicion umbral = repositorio.guardar(UmbralReposicion.crear(crearVariante(), null, 3, 6));
        entityManager.flush();

        assertThat(repositorio.listar(Pageable.unpaged())).extracting(UmbralReposicion::getId)
                .contains(umbral.getId());
    }

    @Test
    void laBaseRechazaUnUmbralConVarianteYCategoriaALaVez() {
        UUID variante = crearVariante();
        UUID categoria = crearCategoria();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO umbral_reposicion_interna (variante_id, categoria_id, cantidad_minima, cantidad_objetivo)"
                        + " VALUES (?, ?, 3, 6)", variante, categoria))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseRechazaUnUmbralSinVarianteNiCategoria() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO umbral_reposicion_interna (cantidad_minima, cantidad_objetivo) VALUES (3, 6)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseRechazaMinimoNoPositivo() {
        UUID variante = crearVariante();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO umbral_reposicion_interna (variante_id, cantidad_minima, cantidad_objetivo)"
                        + " VALUES (?, 0, 6)", variante))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseRechazaObjetivoNoMayorAlMinimo() {
        UUID variante = crearVariante();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO umbral_reposicion_interna (variante_id, cantidad_minima, cantidad_objetivo)"
                        + " VALUES (?, 3, 3)", variante))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseRechazaDosUmbralesParaLaMismaVariante() {
        UUID variante = crearVariante();
        jdbc.update("INSERT INTO umbral_reposicion_interna (variante_id, cantidad_minima, cantidad_objetivo)"
                + " VALUES (?, 3, 6)", variante);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO umbral_reposicion_interna (variante_id, cantidad_minima, cantidad_objetivo)"
                        + " VALUES (?, 4, 8)", variante))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseRechazaDosUmbralesParaLaMismaCategoria() {
        UUID categoria = crearCategoria();
        jdbc.update("INSERT INTO umbral_reposicion_interna (categoria_id, cantidad_minima, cantidad_objetivo)"
                + " VALUES (?, 3, 6)", categoria);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO umbral_reposicion_interna (categoria_id, cantidad_minima, cantidad_objetivo)"
                        + " VALUES (?, 4, 8)", categoria))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
