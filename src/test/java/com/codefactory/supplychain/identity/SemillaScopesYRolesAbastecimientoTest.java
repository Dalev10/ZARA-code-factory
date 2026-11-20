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
 * HD-03 (Sprint 3): fija lo que siembra V34 (decisión D-94 y sección 4.5 del plan). Si alguien
 * cambia los scopes de un rol de abastecimiento en una migración, este test obliga a hacerlo a
 * conciencia.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SemillaScopesYRolesAbastecimientoTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final List<String> SCOPES_SPRINT_3 = List.of(
            "reposicion:planificar", "despacho:operar", "transporte:operar", "recepcion:registrar");

    @Autowired
    private EntityManager entityManager;

    @Test
    void cadaRolDeAbastecimientoTieneExactamenteSusScopes() {
        assertThat(scopesDe("PLANIFICADOR_ABASTECIMIENTO")).containsExactlyInAnyOrder(
                "reposicion:planificar", "reposicion:administrar", "inventario:consultar");
        assertThat(scopesDe("COORDINADOR_TRANSPORTE")).containsExactlyInAnyOrder(
                "transporte:operar", "inventario:consultar");
        assertThat(scopesDe("ENCARGADO_BODEGA")).containsExactlyInAnyOrder(
                "recepcion:registrar", "inventario:consultar");
    }

    @Test
    void adminYOperadorCdRecibenLosScopesQueLesCorresponden() {
        assertThat(scopesDe("ADMIN")).containsAll(SCOPES_SPRINT_3);
        assertThat(scopesDe("OPERADOR_CD")).contains("despacho:operar");
    }

    @Test
    void sonSensiblesTodosSalvoElDeRegistrar() {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = entityManager.createNativeQuery(
                        "SELECT codigo, sensible FROM scope WHERE codigo IN (:codigos)")
                .setParameter("codigos", SCOPES_SPRINT_3)
                .getResultList();
        Map<String, Boolean> sensibles = filas.stream()
                .collect(Collectors.toMap(fila -> (String) fila[0], fila -> (Boolean) fila[1]));

        assertThat(sensibles).containsExactlyInAnyOrderEntriesOf(Map.of(
                "reposicion:planificar", true, "despacho:operar", true,
                "transporte:operar", true, "recepcion:registrar", false));
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
