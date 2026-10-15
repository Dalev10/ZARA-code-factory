package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

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
 * Usuarios y tiendas se insertan con SQL: identity no tiene acceso al modelo de tiendas
 * (pertenecen a inventario), solo a la tabla de asignaciones.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UsuarioTiendaRepositoryAdapter.class)
class UsuarioTiendaRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UsuarioTiendaRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void asignaListaYQuitaLasTiendasDeUnUsuario() {
        UUID usuarioId = crearUsuario();
        UUID tiendaA = crearTienda();
        UUID tiendaB = crearTienda();

        adapter.asignar(usuarioId, tiendaA);
        adapter.asignar(usuarioId, tiendaB);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.listarTiendasDeUsuario(usuarioId)).containsExactlyInAnyOrder(tiendaA, tiendaB);
        assertThat(adapter.existeAsignacion(usuarioId, tiendaA)).isTrue();

        adapter.quitar(usuarioId, tiendaA);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.listarTiendasDeUsuario(usuarioId)).containsExactly(tiendaB);
        assertThat(adapter.existeAsignacion(usuarioId, tiendaA)).isFalse();
    }

    @Test
    void quitarUnaAsignacionInexistenteNoFalla() {
        adapter.quitar(UUID.randomUUID(), UUID.randomUUID());
        entityManager.flush();
    }

    private UUID crearUsuario() {
        UUID id = UUID.randomUUID();
        ejecutar("INSERT INTO usuario (id, email, nombre_completo, password_hash) VALUES (?1, ?2, 'Vendedor', 'hash')",
                id, id + "@tiendas.test");
        return id;
    }

    private UUID crearTienda() {
        UUID id = UUID.randomUUID();
        ejecutar("INSERT INTO tienda (id, nombre) VALUES (?1, ?2)", id, "tienda-" + id);
        return id;
    }

    private void ejecutar(String sql, Object... parametros) {
        var query = entityManager.getEntityManager().createNativeQuery(sql);
        for (int i = 0; i < parametros.length; i++) {
            query.setParameter(i + 1, parametros[i]);
        }
        query.executeUpdate();
    }
}
