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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-34 (D-55): V15 crea la Bodega_Tienda de cada tienda existente que no la tiene, sin
 * duplicar la de las tiendas que ya contaban con una.
 */
@Testcontainers
class BodegaAutomaticaMigrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void todaTiendaExistenteQuedaConUnaSolaBodega() throws SQLException {
        String schema = "v15_" + UUID.randomUUID().toString().replace("-", "");
        ejecutar(null, "CREATE SCHEMA " + schema);
        migrar(schema, "14");
        UUID sinBodega = UUID.randomUUID();
        UUID conBodega = UUID.randomUUID();
        ejecutar(schema, "INSERT INTO tienda (id, nombre) VALUES (?, 'Sin bodega')", sinBodega);
        ejecutar(schema, "INSERT INTO tienda (id, nombre) VALUES (?, 'Con bodega')", conBodega);
        ejecutar(schema, "INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'BODEGA_TIENDA', ?)",
                UUID.randomUUID(), conBodega);

        migrar(schema, "15");

        assertThat(bodegas(schema, sinBodega)).isEqualTo(1);
        assertThat(bodegas(schema, conBodega)).isEqualTo(1);
    }

    private static int bodegas(String schema, UUID tiendaId) throws SQLException {
        try (Connection conexion = conexion(schema);
             PreparedStatement sentencia = conexion.prepareStatement(
                     "SELECT count(*) FROM nodo WHERE tienda_id = ? AND tipo = 'BODEGA_TIENDA'")) {
            sentencia.setObject(1, tiendaId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                resultado.next();
                return resultado.getInt(1);
            }
        }
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
