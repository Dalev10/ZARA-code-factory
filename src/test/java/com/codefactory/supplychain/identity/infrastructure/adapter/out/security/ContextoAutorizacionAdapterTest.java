package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import com.codefactory.supplychain.shared.security.UsuarioActual;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La consulta única del adaptador se prueba contra el esquema real de Flyway. Los datos se
 * insertan con SQL para poder fijar estados (BLOQUEADO, INACTIVO) que ninguna API produce
 * todavía.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ContextoAutorizacionAdapter.class)
class ContextoAutorizacionAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ContextoAutorizacionAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    // Hace únicos los códigos de scope de cada test (la tabla scope tiene codigo UNIQUE).
    private final String prefijo = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void usuarioActivoConVariosRolesObtieneLaUnionDeSusScopesSinDuplicados() {
        UUID usuarioId = crearUsuario("ACTIVO", "NULL");
        UUID rolA = crearRolConScopes("compartido", "solo-a");
        UUID rolB = crearRolConScopes("compartido", "solo-b");
        asignarRol(usuarioId, rolA);
        asignarRol(usuarioId, rolB);

        Optional<UsuarioActual> resuelto = adapter.resolver(usuarioId);

        assertThat(resuelto).isPresent();
        assertThat(resuelto.get().id()).isEqualTo(usuarioId);
        assertThat(resuelto.get().scopes())
                .containsExactlyInAnyOrder(sufijo("compartido"), sufijo("solo-a"), sufijo("solo-b"));
    }

    @Test
    void usuarioActivoSinRolesSeResuelveSinScopes() {
        UUID usuarioId = crearUsuario("ACTIVO", "NULL");

        assertThat(adapter.resolver(usuarioId)).hasValueSatisfying(u -> assertThat(u.scopes()).isEmpty());
    }

    @Test
    void usuarioInactivoNoSeResuelve() {
        UUID usuarioId = crearUsuario("INACTIVO", "NULL");
        asignarRol(usuarioId, crearRolConScopes("cualquiera"));

        assertThat(adapter.resolver(usuarioId)).isEmpty();
    }

    @Test
    void usuarioBloqueadoAdministrativamenteNoSeResuelve() {
        UUID usuarioId = crearUsuario("BLOQUEADO", "NULL");
        asignarRol(usuarioId, crearRolConScopes("cualquiera"));

        assertThat(adapter.resolver(usuarioId)).isEmpty();
    }

    @Test
    void elBloqueoTemporalPorIntentosFallidosNoCortaLaSesion() {
        UUID usuarioId = crearUsuario("ACTIVO", "now() + interval '1 hour'");

        assertThat(adapter.resolver(usuarioId)).isPresent();
    }

    @Test
    void usuarioInexistenteNoSeResuelve() {
        assertThat(adapter.resolver(UUID.randomUUID())).isEmpty();
    }

    private String sufijo(String codigo) {
        return codigo + ":" + prefijo;
    }

    private UUID crearUsuario(String estado, String bloqueadoHastaSql) {
        UUID id = UUID.randomUUID();
        ejecutar("INSERT INTO usuario (id, email, nombre_completo, password_hash, estado, bloqueado_hasta) "
                        + "VALUES (?1, ?2, 'Usuario de prueba', 'hash', ?3, " + bloqueadoHastaSql + ")",
                id, id + "@contexto.test", estado);
        return id;
    }

    private UUID crearRolConScopes(String... codigos) {
        UUID rolId = UUID.randomUUID();
        ejecutar("INSERT INTO rol (id, nombre) VALUES (?1, ?2)", rolId, "rol-" + rolId);
        for (String codigo : codigos) {
            ejecutar("INSERT INTO scope (codigo) VALUES (?1) ON CONFLICT (codigo) DO NOTHING", sufijo(codigo));
            ejecutar("INSERT INTO rol_scope (rol_id, scope_id) SELECT ?1, id FROM scope WHERE codigo = ?2",
                    rolId, sufijo(codigo));
        }
        return rolId;
    }

    private void asignarRol(UUID usuarioId, UUID rolId) {
        ejecutar("INSERT INTO usuario_rol (usuario_id, rol_id) VALUES (?1, ?2)", usuarioId, rolId);
    }

    private void ejecutar(String sql, Object... parametros) {
        var query = entityManager.getEntityManager().createNativeQuery(sql);
        for (int i = 0; i < parametros.length; i++) {
            query.setParameter(i + 1, parametros[i]);
        }
        query.executeUpdate();
    }
}
