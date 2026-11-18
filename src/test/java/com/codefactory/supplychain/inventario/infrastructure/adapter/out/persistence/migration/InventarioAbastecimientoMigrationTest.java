package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V29 (HD-01) sobre una base con datos del Sprint 2: las filas existentes siguen
 * siendo válidas, entrante y saliente dejan de estar fijos en 0, el disponible
 * pasa a ser a_la_mano - saliente y el kardex acepta DESPACHO y RECEPCION.
 */
@Testcontainers
class InventarioAbastecimientoMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private final UUID varianteId = UUID.randomUUID();
    private final UUID cdId = UUID.randomUUID();
    private final UUID nodoId = UUID.randomUUID();

    @Test
    void migraSobreDatosPreviosYCambiaLasRestricciones() throws SQLException {
        String schema = "v29_" + UUID.randomUUID().toString().replace("-", "");
        ejecutar(null, "CREATE SCHEMA " + schema);
        migrar(schema, "28");
        insertarEntidades(schema);
        ejecutar(schema, """
                INSERT INTO inventario (nodo_id, variante_id, a_la_mano, disponible_para_uso, entrante, saliente,
                                        pronosticado, version, fecha_actualizacion)
                VALUES (?, ?, 10, 10, 0, 0, 10, 0, now())""", nodoId, varianteId);

        assertThatCode(() -> migrar(schema, "29")).doesNotThrowAnyException();

        assertThatCode(() -> ejecutar(schema, """
                UPDATE inventario SET saliente = 4, disponible_para_uso = 6, entrante = 2, pronosticado = 8
                 WHERE nodo_id = ?""", nodoId)).doesNotThrowAnyException();
        assertThatCode(() -> ejecutar(schema, """
                UPDATE inventario SET a_la_mano = 2, disponible_para_uso = -2, pronosticado = 0
                 WHERE nodo_id = ?""", nodoId)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ejecutar(schema,
                "UPDATE inventario SET disponible_para_uso = 2 WHERE nodo_id = ?", nodoId))
                .hasMessageContaining("chk_inventario_disponible_coherente");
        assertThatThrownBy(() -> ejecutar(schema,
                "UPDATE inventario SET saliente = -1, disponible_para_uso = 3, pronosticado = 5 WHERE nodo_id = ?",
                nodoId))
                .hasMessageContaining("chk_inventario_saliente_no_negativo");
        assertThatThrownBy(() -> ejecutar(schema,
                "UPDATE inventario SET entrante = -1, pronosticado = -3 WHERE nodo_id = ?", nodoId))
                .hasMessageContaining("chk_inventario_entrante_no_negativo");

        for (String tipo : new String[] {"DESPACHO", "RECEPCION"}) {
            assertThatCode(() -> insertarMovimiento(schema, tipo)).doesNotThrowAnyException();
        }
        assertThatThrownBy(() -> insertarMovimiento(schema, "RESERVA"))
                .hasMessageContaining("movimiento_inventario_tipo_check");
    }

    private void insertarEntidades(String schema) throws SQLException {
        UUID categoriaId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        ejecutar(schema, "INSERT INTO categoria (id, nombre) VALUES (?, ?)", categoriaId, "categoria-" + categoriaId);
        ejecutar(schema, "INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)",
                templateId, categoriaId, "template-" + templateId);
        ejecutar(schema, "INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)",
                varianteId, templateId, "sku-" + varianteId);
        ejecutar(schema, "INSERT INTO cd (id, nombre, ubicacion) VALUES (?, ?, 'Bogotá')", cdId, "cd-" + cdId);
        ejecutar(schema, "INSERT INTO nodo (id, tipo, cd_id) VALUES (?, 'CD', ?)", nodoId, cdId);
    }

    private void insertarMovimiento(String schema, String tipo) throws SQLException {
        ejecutar(schema, """
                INSERT INTO movimiento_inventario (nodo_id, variante_id, tipo, cantidad, saldo_resultante)
                VALUES (?, ?, ?, 1, 1)""", nodoId, varianteId, tipo);
    }

    private static void migrar(String schema, String version) {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .target(version)
                .load()
                .migrate();
    }

    private static void ejecutar(String schema, String sql, Object... parametros) throws SQLException {
        try (Connection conexion = conexion(schema); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                sentencia.setObject(i + 1, parametros[i]);
            }
            sentencia.execute();
        }
    }

    private static Connection conexion(String schema) throws SQLException {
        Connection conexion = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        if (schema != null) {
            conexion.setSchema(schema);
        }
        return conexion;
    }
}
