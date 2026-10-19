package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V25 y V26 sobre datos previos: el saldo del kardex se completa con la suma acumulada,
 * la anulación pierde su motivo redundante, y los CHECK nuevos toleran filas históricas
 * pero rechazan las nuevas.
 */
@Testcontainers
class KardexYRestriccionesMigrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private final UUID varianteId = UUID.randomUUID();
    private final UUID tiendaId = UUID.randomUUID();
    private final UUID nodoId = UUID.randomUUID();

    @Test
    void migraElHistoricoYExigeLasRestriccionesALasFilasNuevas() throws SQLException {
        String schema = "v26_" + UUID.randomUUID().toString().replace("-", "");
        ejecutar(null, "CREATE SCHEMA " + schema);
        migrar(schema, "24");
        insertarEntidades(schema);
        insertarMovimiento(schema, "CARGA_INICIAL", 5, null, "2026-01-01 09:00:00");
        insertarMovimiento(schema, "VENTA", -2, null, "2026-01-02 09:00:00");
        insertarMovimiento(schema, "ANULACION_VENTA", 2, "ANULACION_VENTA", "2026-01-03 09:00:00");
        ejecutar(schema, "INSERT INTO venta (id, variante_id, tienda_id, cantidad, precio) VALUES (?, ?, ?, 0, 10)",
                UUID.randomUUID(), varianteId, tiendaId);
        ejecutar(schema, """
                INSERT INTO solicitud_no_satisfecha (id, variante_id, tienda_id, registrado_por, clave_idempotencia)
                VALUES (?, ?, ?, 'texto libre', ?)""", UUID.randomUUID(), varianteId, tiendaId, "legacy-1");

        migrar(schema, "26");

        assertThat(consultar(schema, "SELECT saldo_resultante FROM movimiento_inventario ORDER BY ocurrido_en"))
                .containsExactly("5", "3", "5");
        assertThat(consultar(schema, "SELECT motivo FROM movimiento_inventario WHERE tipo = 'ANULACION_VENTA'"))
                .containsExactly((String) null);
        assertThat(consultar(schema, """
                SELECT data_type FROM information_schema.columns
                 WHERE table_schema = current_schema() AND table_name = 'traspaso_interno'
                   AND column_name = 'timestamp_traspaso'"""))
                .containsExactly("timestamp with time zone");
        assertThatThrownBy(() -> ejecutar(schema,
                "INSERT INTO venta (id, variante_id, tienda_id, cantidad, precio) VALUES (?, ?, ?, 0, 10)",
                UUID.randomUUID(), varianteId, tiendaId))
                .hasMessageContaining("chk_venta_cantidad_positiva");
        assertThatThrownBy(() -> ejecutar(schema, """
                INSERT INTO solicitud_no_satisfecha (id, variante_id, tienda_id, clave_idempotencia)
                VALUES (?, ?, ?, ?)""", UUID.randomUUID(), varianteId, tiendaId, "nueva-1"))
                .hasMessageContaining("chk_solicitud_registrado_por");
        assertThatThrownBy(() -> insertarMovimiento(schema, "AJUSTE", 1, "TEXTO_LIBRE", "2026-01-04 09:00:00"))
                .hasMessageContaining("movimiento_inventario_motivo_check");
    }

    private void insertarEntidades(String schema) throws SQLException {
        UUID categoriaId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        ejecutar(schema, "INSERT INTO categoria (id, nombre) VALUES (?, ?)", categoriaId, "categoria-" + categoriaId);
        ejecutar(schema, "INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)",
                templateId, categoriaId, "template-" + templateId);
        ejecutar(schema, "INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)",
                varianteId, templateId, "sku-" + varianteId);
        ejecutar(schema, "INSERT INTO tienda (id, nombre) VALUES (?, ?)", tiendaId, "tienda-" + tiendaId);
        ejecutar(schema, "INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'ALMACEN', ?)", nodoId, tiendaId);
    }

    private void insertarMovimiento(String schema, String tipo, int cantidad, String motivo, String fecha)
            throws SQLException {
        String columnasSaldo = existeColumna(schema) ? ", saldo_resultante" : "";
        String valorSaldo = existeColumna(schema) ? ", 0" : "";
        ejecutar(schema, "INSERT INTO movimiento_inventario (id, nodo_id, variante_id, tipo, cantidad, motivo, ocurrido_en"
                        + columnasSaldo + ") VALUES (?, ?, ?, ?, ?, ?, CAST(? AS timestamptz)" + valorSaldo + ")",
                UUID.randomUUID(), nodoId, varianteId, tipo, cantidad, motivo, fecha);
    }

    private static boolean existeColumna(String schema) throws SQLException {
        return !consultar(schema, """
                SELECT 1 FROM information_schema.columns
                 WHERE table_schema = current_schema() AND table_name = 'movimiento_inventario'
                   AND column_name = 'saldo_resultante'""").isEmpty();
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

    private static List<String> consultar(String schema, String sql) throws SQLException {
        List<String> filas = new ArrayList<>();
        try (Connection conexion = conexion(schema);
             Statement sentencia = conexion.createStatement();
             ResultSet resultado = sentencia.executeQuery(sql)) {
            while (resultado.next()) {
                filas.add(resultado.getString(1));
            }
        }
        return filas;
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
