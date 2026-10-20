package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.migration;

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
 * HU-35 (D-52): la migración pasa con y sin templates huérfanos. Con huérfanos, quedan en
 * SIN_CATEGORIA; sin ellos, SIN_CATEGORIA no queda en el catálogo (V16 + V27).
 */
@Testcontainers
class CategoriaObligatoriaMigrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void conHuerfanosQuedanEnSinCategoria() throws SQLException {
        String schema = nuevoSchema();
        UUID templateId = UUID.randomUUID();
        ejecutar(schema, "INSERT INTO template (id, nombre) VALUES (?, 'Huérfano')", templateId);

        migrar(schema, "27");

        assertThat(contar(schema, """
                SELECT count(*) FROM template t JOIN categoria c ON c.id = t.categoria_id
                 WHERE c.nombre = 'SIN_CATEGORIA'""")).isEqualTo(1);
    }

    @Test
    void sinHuerfanosNoQuedaSinCategoria() throws SQLException {
        String schema = nuevoSchema();
        UUID categoriaId = UUID.randomUUID();
        ejecutar(schema, "INSERT INTO categoria (id, nombre) VALUES (?, 'Camisas')", categoriaId);
        ejecutar(schema, "INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, 'Polo')",
                UUID.randomUUID(), categoriaId);

        migrar(schema, "27");

        assertThat(contar(schema, "SELECT count(*) FROM categoria WHERE nombre = 'SIN_CATEGORIA'")).isZero();
        assertThat(contar(schema, "SELECT count(*) FROM template WHERE categoria_id IS NULL")).isZero();
    }

    private static String nuevoSchema() throws SQLException {
        String schema = "v27_" + UUID.randomUUID().toString().replace("-", "");
        ejecutar(null, "CREATE SCHEMA " + schema);
        migrar(schema, "15");
        return schema;
    }

    private static int contar(String schema, String sql) throws SQLException {
        try (Connection conexion = conexion(schema);
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            resultado.next();
            return resultado.getInt(1);
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
