package com.codefactory.supplychain.identity;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-33: fija lo que siembra V14 (decisiones D-61 y D-62). Si alguien cambia los scopes de
 * un rol operativo en una migración, este test obliga a hacerlo a conciencia.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SemillaScopesYRolesOperativosTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final List<String> SCOPES_SPRINT_2 = List.of(
            "tiendas:operar-todas", "inventario:administrar", "inventario-cd:administrar", "inventario:consultar",
            "ventas:registrar", "ventas:forzar-sin-stock", "ventas:anular", "ventas:consultar",
            "reposicion:administrar");

    @Autowired
    private EntityManager entityManager;

    @Test
    void cadaRolOperativoTieneExactamenteSusScopes() {
        assertThat(scopesDe("VENDEDOR")).containsExactlyInAnyOrder(
                "ventas:registrar", "ventas:consultar", "inventario:consultar", "ventas:forzar-sin-stock");
        assertThat(scopesDe("SUPERVISOR_TIENDA")).containsExactlyInAnyOrder(
                "ventas:registrar", "ventas:consultar", "inventario:consultar", "ventas:forzar-sin-stock",
                "ventas:anular", "inventario:administrar");
        assertThat(scopesDe("OPERADOR_CD")).containsExactlyInAnyOrder(
                "inventario-cd:administrar", "inventario:consultar");
    }

    @Test
    void adminTieneLosNuevosScopesAdemasDeLosQueYaTenia() {
        assertThat(scopesDe("ADMIN"))
                .containsAll(SCOPES_SPRINT_2)
                .contains("usuarios:administrar", "tiendas:administrar", "catalogo:administrar")
                .hasSize(15);
    }

    @Test
    void sonSensiblesTodosLosScopesNuevosSalvoLosDeConsultarYRegistrar() {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = entityManager.createNativeQuery(
                        "SELECT codigo, sensible FROM scope WHERE codigo IN (:codigos)")
                .setParameter("codigos", SCOPES_SPRINT_2)
                .getResultList();
        Map<String, Boolean> sensibles = filas.stream()
                .collect(Collectors.toMap(fila -> (String) fila[0], fila -> (Boolean) fila[1]));

        assertThat(sensibles).hasSize(9).containsAllEntriesOf(Map.of(
                "inventario:consultar", false, "ventas:registrar", false, "ventas:consultar", false,
                "tiendas:operar-todas", true, "inventario:administrar", true, "inventario-cd:administrar", true,
                "ventas:forzar-sin-stock", true, "ventas:anular", true, "reposicion:administrar", true));
    }

    @SuppressWarnings("unchecked")
    private List<String> scopesDe(String nombreRol) {
        return entityManager.createNativeQuery("""
                        SELECT s.codigo FROM rol r
                        JOIN rol_scope rs ON rs.rol_id = r.id
                        JOIN scope s ON s.id = rs.scope_id
                        WHERE r.nombre = :nombre
                        """)
                .setParameter("nombre", nombreRol)
                .getResultList();
    }
}
