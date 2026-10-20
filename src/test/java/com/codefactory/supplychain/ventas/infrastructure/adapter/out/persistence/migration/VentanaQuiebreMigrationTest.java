package com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class VentanaQuiebreMigrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void fallaSinMutarVentanasAbiertasDuplicadas() throws SQLException {
        String schema = prepararSchema();
        UUID[] entidades = insertarEntidades(schema);
        insertarVentana(schema, entidades, "2026-01-01 09:00:00", null);
        insertarVentana(schema, entidades, "2026-01-02 09:00:00", null);

        assertThatThrownBy(() -> migrarHasta20(schema))
                .isInstanceOf(FlywayException.class)
                .hasStackTraceContaining("Duplicados abiertos")
                .hasStackTraceContaining("tienda=")
                .hasStackTraceContaining("variante=");

        assertThat(contarVentanas(schema)).isEqualTo(2);
        assertThat(existeColumnaMotivo(schema)).isFalse();
    }

    @Test
    void fallaSinMutarFechasInvalidas() throws SQLException {
        String schema = prepararSchema();
        UUID[] entidades = insertarEntidades(schema);
        insertarVentana(schema, entidades, "2026-01-02 09:00:00", "2026-01-01 09:00:00");

        assertThatThrownBy(() -> migrarHasta20(schema))
                .isInstanceOf(FlywayException.class)
                .hasStackTraceContaining("Fechas inválidas")
                .hasStackTraceContaining("inicio=")
                .hasStackTraceContaining("fin=");

        assertThat(contarVentanas(schema)).isEqualTo(1);
        assertThat(existeColumnaMotivo(schema)).isFalse();
    }

    @Test
    void preservaCierresHistoricosSinInventarMotivoDeNegocio() throws SQLException {
        String schema = prepararSchema();
        UUID[] entidades = insertarEntidades(schema);
        insertarVentana(schema, entidades, "2026-01-01 09:00:00", "2026-01-02 09:00:00");

        migrarHasta20(schema);

        assertThat(motivoVentana(schema)).isEqualTo("HISTORICO_NO_CLASIFICADO");
        assertThat(indiceUnicoParcialExiste(schema, "uq_ventana_quiebre_abierta")).isTrue();
        assertThat(constraintExiste(schema, "chk_ventana_quiebre_fechas")).isTrue();
    }

    private static String prepararSchema() {
        String schema = "v20_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = conexion();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear el esquema de prueba", e);
        }
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .target("19")
                .load()
                .migrate();
        return schema;
    }

    private static void migrarHasta20(String schema) {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .target("20")
                .load()
                .migrate();
    }

    private static UUID[] insertarEntidades(String schema) throws SQLException {
        UUID categoriaId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        UUID tiendaId = UUID.randomUUID();
        try (Connection connection = conexion()) {
            fijarSchema(connection, schema);
            try (PreparedStatement categoria = connection.prepareStatement(
                    "INSERT INTO categoria (id, nombre) VALUES (?, ?)");
                 PreparedStatement template = connection.prepareStatement(
                         "INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)");
                 PreparedStatement variante = connection.prepareStatement(
                         "INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)");
                 PreparedStatement tienda = connection.prepareStatement(
                         "INSERT INTO tienda (id, nombre) VALUES (?, ?)")) {
                categoria.setObject(1, categoriaId);
                categoria.setString(2, "categoria-" + categoriaId);
                categoria.executeUpdate();

                template.setObject(1, templateId);
                template.setObject(2, categoriaId);
                template.setString(3, "template-" + templateId);
                template.executeUpdate();

                variante.setObject(1, varianteId);
                variante.setObject(2, templateId);
                variante.setString(3, "sku-" + varianteId);
                variante.executeUpdate();

                tienda.setObject(1, tiendaId);
                tienda.setString(2, "tienda-" + tiendaId);
                tienda.executeUpdate();
            }
        }
        return new UUID[] {varianteId, tiendaId};
    }

    private static void insertarVentana(String schema, UUID[] entidades, String inicio, String fin)
            throws SQLException {
        try (Connection connection = conexion()) {
            fijarSchema(connection, schema);
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO ventana_quiebre (id, variante_id, tienda_id, fecha_inicio, fecha_fin)
                    VALUES (?, ?, ?, CAST(? AS timestamp), CAST(? AS timestamp))
                    """)) {
                insert.setObject(1, UUID.randomUUID());
                insert.setObject(2, entidades[0]);
                insert.setObject(3, entidades[1]);
                insert.setString(4, inicio);
                insert.setString(5, fin);
                insert.executeUpdate();
            }
        }
    }

    private static long contarVentanas(String schema) throws SQLException {
        try (Connection connection = conexion()) {
            fijarSchema(connection, schema);
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT count(*) FROM ventana_quiebre")) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private static String motivoVentana(String schema) throws SQLException {
        try (Connection connection = conexion()) {
            fijarSchema(connection, schema);
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT motivo_cierre FROM ventana_quiebre")) {
                result.next();
                return result.getString(1);
            }
        }
    }

    private static boolean existeColumnaMotivo(String schema) throws SQLException {
        try (Connection connection = conexion()) {
            fijarSchema(connection, schema);
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("""
                         SELECT EXISTS (
                             SELECT 1 FROM information_schema.columns
                             WHERE table_schema = current_schema()
                               AND table_name = 'ventana_quiebre'
                               AND column_name = 'motivo_cierre'
                         )
                         """)) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private static boolean constraintExiste(String schema, String nombre) throws SQLException {
        try (Connection connection = conexion()) {
            fijarSchema(connection, schema);
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT EXISTS (
                        SELECT 1 FROM pg_constraint
                        WHERE connamespace = current_schema()::regnamespace
                          AND conname = ?
                    )
                    """)) {
                statement.setString(1, nombre);
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    return result.getBoolean(1);
                }
            }
        }
    }

    private static boolean indiceUnicoParcialExiste(String schema, String nombre) throws SQLException {
        try (Connection connection = conexion()) {
            fijarSchema(connection, schema);
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT EXISTS (
                        SELECT 1
                        FROM pg_index indice
                        JOIN pg_class nombre_indice ON nombre_indice.oid = indice.indexrelid
                        JOIN pg_namespace esquema ON esquema.oid = nombre_indice.relnamespace
                        WHERE esquema.nspname = current_schema()
                          AND nombre_indice.relname = ?
                          AND indice.indisunique
                          AND indice.indpred IS NOT NULL
                          AND pg_get_expr(indice.indpred, indice.indrelid) LIKE '%fecha_fin IS NULL%'
                          AND pg_get_indexdef(indice.indexrelid) LIKE '%(variante_id, tienda_id)%'
                    )
                    """)) {
                statement.setString(1, nombre);
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    return result.getBoolean(1);
                }
            }
        }
    }

    private static Connection conexion() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void fijarSchema(Connection connection, String schema) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + schema);
        }
    }
}
