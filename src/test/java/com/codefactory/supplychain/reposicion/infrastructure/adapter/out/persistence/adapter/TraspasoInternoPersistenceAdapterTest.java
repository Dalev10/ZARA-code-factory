package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.reposicion.domain.model.OrigenTraspaso;
import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.mapper.TraspasoInternoPersistenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * traspaso_interno tiene FK a variante, tienda y usuario (registrado_por, V22): cada test
 * crea esas filas antes de guardar, para que la base no rechace el INSERT.
 */
@DisplayName("TraspasoInternoPersistenceAdapter")
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TraspasoInternoPersistenceMapper.class, TraspasoInternoPersistenceAdapter.class})
class TraspasoInternoPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TraspasoInternoPersistenceAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID tiendaId;
    private UUID varianteId;
    private UUID usuarioId;

    @BeforeEach
    void crearDatosRelacionados() {
        tiendaId = jdbc.queryForObject(
                "INSERT INTO tienda (nombre, ubicacion, estado) VALUES (?, 'Loc', 'ACTIVA') RETURNING id",
                UUID.class, "Tienda-" + UUID.randomUUID());
        UUID categoriaId = jdbc.queryForObject(
                "INSERT INTO categoria (nombre) VALUES (?) RETURNING id",
                UUID.class, "Cat-" + UUID.randomUUID());
        UUID templateId = jdbc.queryForObject(
                "INSERT INTO template (categoria_id, nombre) VALUES (?, ?) RETURNING id",
                UUID.class, categoriaId, "Tpl-" + UUID.randomUUID());
        varianteId = jdbc.queryForObject(
                "INSERT INTO variante (template_id, talla, color, sku) VALUES (?, 'M', 'Negro', ?) RETURNING id",
                UUID.class, templateId, "SKU-" + UUID.randomUUID());
        usuarioId = jdbc.queryForObject(
                "INSERT INTO usuario (email, nombre_completo, password_hash) VALUES (?, 'Usuario Prueba', 'hash') RETURNING id",
                UUID.class, "u-" + UUID.randomUUID() + "@test.com");
    }

    private TraspasoInterno automatico(int cantidad, Instant cuando) {
        return TraspasoInterno.reconstruir(UUID.randomUUID(), varianteId, tiendaId, cantidad,
                OrigenTraspaso.AUTOMATICO, null, cuando);
    }

    private TraspasoInterno manual(int cantidad, Instant cuando) {
        return TraspasoInterno.reconstruir(UUID.randomUUID(), varianteId, tiendaId, cantidad,
                OrigenTraspaso.MANUAL, usuarioId, cuando);
    }

    @Nested
    @DisplayName("guardar y recuperar por id")
    class GuardarYRecuperar {

        @Test
        @DisplayName("persiste un traspaso AUTOMATICO sin registrador")
        void guardaYRecuperaAutomatico() {
            TraspasoInterno guardado = adapter.guardar(automatico(5, Instant.now()));
            entityManager.flush();
            entityManager.clear();

            Optional<TraspasoInterno> recuperado = adapter.buscarPorId(guardado.getId());

            assertThat(recuperado).isPresent();
            assertThat(recuperado.get().getOrigen()).isEqualTo(OrigenTraspaso.AUTOMATICO);
            assertThat(recuperado.get().getRegistradoPor()).isNull();
            assertThat(recuperado.get().getCantidad()).isEqualTo(5);
        }

        @Test
        @DisplayName("persiste un traspaso MANUAL con su registrador")
        void guardaYRecuperaManual() {
            TraspasoInterno guardado = adapter.guardar(manual(3, Instant.now()));
            entityManager.flush();
            entityManager.clear();

            Optional<TraspasoInterno> recuperado = adapter.buscarPorId(guardado.getId());

            assertThat(recuperado).isPresent();
            assertThat(recuperado.get().getOrigen()).isEqualTo(OrigenTraspaso.MANUAL);
            assertThat(recuperado.get().getRegistradoPor()).isEqualTo(usuarioId);
            assertThat(recuperado.get().getTiendaId()).isEqualTo(tiendaId);
            assertThat(recuperado.get().getVarianteId()).isEqualTo(varianteId);
        }

        @Test
        @DisplayName("devuelve vacío para un id inexistente")
        void idInexistenteDevuelveVacio() {
            assertThat(adapter.buscarPorId(UUID.randomUUID())).isEmpty();
        }
    }

    @Nested
    @DisplayName("listarPorTienda")
    class ListarPorTienda {

        @Test
        @DisplayName("devuelve los traspasos de la tienda del más reciente al más antiguo")
        void listaOrdenadoDescendente() {
            Instant ahora = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            TraspasoInterno antiguo = adapter.guardar(automatico(2, ahora.minus(2, ChronoUnit.HOURS)));
            TraspasoInterno reciente = adapter.guardar(manual(4, ahora));
            entityManager.flush();
            entityManager.clear();

            List<TraspasoInterno> lista = adapter.listarPorTienda(tiendaId);

            assertThat(lista).extracting(TraspasoInterno::getId)
                    .containsExactly(reciente.getId(), antiguo.getId());
        }

        @Test
        @DisplayName("devuelve lista vacía para una tienda sin traspasos")
        void tiendaSinTraspasos() {
            assertThat(adapter.listarPorTienda(UUID.randomUUID())).isEmpty();
        }
    }
}
