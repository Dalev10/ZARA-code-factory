package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.modulo;

import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.reposicion.api.ConsultaOrdenes;
import com.codefactory.supplychain.reposicion.api.OrdenNoEncontradaException;
import com.codefactory.supplychain.reposicion.api.OrdenResumen;
import com.codefactory.supplychain.reposicion.application.port.in.CrearOrdenUseCase;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code reposicion/api}: {@code consultarOrden} (sección 4.2) contra PostgreSQL real,
 * sobre órdenes creadas por HU-45.
 *
 * Módulo: reposicion.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("ConsultaOrdenes (reposicion/api)")
class ConsultaOrdenesAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private ConsultaOrdenes consultaOrdenes;
    @Autowired private CrearOrdenUseCase crearOrden;
    @Autowired private InventarioUseCase inventario;
    @Autowired private TransactionTemplate tx;
    @Autowired private JdbcTemplate jdbc;

    private UUID tiendaId;
    private UUID varianteId;

    @BeforeEach
    void setUp() {
        tiendaId = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')", tiendaId, "Tienda " + tiendaId);
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'BODEGA_TIENDA', ?)", UUID.randomUUID(), tiendaId);
        varianteId = crearVariante();
    }

    @Test
    @DisplayName("""
            Escenario: consultar una orden con CD asignado
              Dado una orden de 10 unidades creada por HU-45 con un CD que la cubre
              Cuando otro módulo consulta la orden
              Entonces recibe su tienda, CD, variante, estado APROBADA, solicitada 10 y pendiente 10""")
    void ordenConCd() {
        UUID cdId = cdConStock(30);
        jdbc.update("INSERT INTO tienda_cd_prioridad (tienda_id, cd_id, orden_prioridad) VALUES (?, ?, 1)",
                tiendaId, cdId);
        OrdenReabastecimiento orden = tx.execute(s -> crearOrden.crear(null, tiendaId, varianteId, 10));

        OrdenResumen resumen = consultaOrdenes.consultarOrden(orden.getId());

        assertThat(resumen).isEqualTo(new OrdenResumen(orden.getId(), tiendaId, cdId, varianteId,
                "APROBADA", 10, 10));
    }

    @Test
    @DisplayName("""
            Escenario: consultar una orden sin CD
              Dado una orden GENERADA sin CD
              Cuando otro módulo consulta la orden
              Entonces recibe cdId nulo y estado GENERADA""")
    void ordenSinCd() {
        OrdenReabastecimiento orden = tx.execute(s -> crearOrden.crear(null, tiendaId, varianteId, 7));

        OrdenResumen resumen = consultaOrdenes.consultarOrden(orden.getId());

        assertThat(resumen.cdId()).isNull();
        assertThat(resumen.estado()).isEqualTo("GENERADA");
        assertThat(resumen.cantidadSolicitada()).isEqualTo(7);
        assertThat(resumen.cantidadPendiente()).isEqualTo(7);
    }

    @Test
    @DisplayName("""
            Escenario: orden inexistente
              Cuando otro módulo consulta una orden que no existe
              Entonces recibe OrdenNoEncontradaException""")
    void ordenInexistente() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> consultaOrdenes.consultarOrden(id))
                .isInstanceOf(OrdenNoEncontradaException.class)
                .hasMessageContaining(id.toString());
    }

    private UUID cdConStock(int cantidad) {
        UUID cdId = UUID.randomUUID();
        jdbc.update("INSERT INTO cd (id, nombre, ubicacion) VALUES (?, ?, 'Bogotá')", cdId, "CD " + cdId);
        UUID nodo = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, cd_id) VALUES (?, 'CD', ?)", nodo, cdId);
        inventario.cargar(nodo, varianteId, cantidad, null);
        return cdId;
    }

    private UUID crearVariante() {
        UUID categoriaId = UUID.randomUUID();
        jdbc.update("INSERT INTO categoria (id, nombre) VALUES (?, ?)", categoriaId, "Categoría " + categoriaId);
        UUID templateId = UUID.randomUUID();
        jdbc.update("INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)",
                templateId, categoriaId, "Template " + templateId);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)", id, templateId, "SKU-" + id);
        return id;
    }
}
