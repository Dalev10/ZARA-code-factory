package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.api.MovimientosAbastecimiento;
import com.codefactory.supplychain.reposicion.api.TransicionesOrden;
import com.codefactory.supplychain.soporte.AbastecimientoDePrueba;
import com.codefactory.supplychain.soporte.UsuariosDePrueba;
import com.codefactory.supplychain.soporte.UsuariosDePrueba.UsuarioDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-53 de extremo a extremo: la orden llega a REABIERTA por el ciclo real (despacho parcial y
 * recepción parcial) y al cerrarla el CD y la Bodega quedan sin nada retenido (riesgo D-2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import({UsuariosDePrueba.class, AbastecimientoDePrueba.class})
@DisplayName("Cerrar con faltante o cancelar una orden (HU-53)")
class CierreOrdenControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuariosDePrueba usuarios;
    @Autowired private AbastecimientoDePrueba red;
    @Autowired private TransicionesOrden transiciones;
    @Autowired private MovimientosAbastecimiento movimientos;
    @Autowired private TransactionTemplate tx;

    private UUID varianteId;
    private UUID cdId;
    private UUID tiendaId;
    private UsuarioDePrueba planificador;

    @BeforeEach
    void setUp() {
        varianteId = red.crearVariante();
        cdId = red.crearCd(varianteId, 10);
        tiendaId = red.crearTienda(cdId);
        planificador = usuarios.conRol("PLANIFICADOR_ABASTECIMIENTO", tiendaId);
    }

    @Test
    @DisplayName("""
            Escenario: cerrar con faltante libera lo retenido
              Dada una orden de 10 de la que se despacharon 4 (el CD perdió stock) y se recibieron 4: REABIERTA
              Y quedan 6 de reserva en el CD y 6 de entrante en la Bodega
              Cuando el planificador la cierra con un motivo
              Entonces la orden queda CERRADA con su motivo, su autor y 6 pendientes como registro
              Y el CD queda sin reserva y la Bodega sin entrante""")
    void cerrarConFaltante() throws Exception {
        UUID ordenId = red.crearOrden(tiendaId, varianteId, 10);
        fijarALaManoCd(4);
        red.despachar(ordenId);
        recibir(ordenId, 4);
        assertThat(red.estadoOrden(ordenId)).isEqualTo("REABIERTA");
        assertThat(cd()).containsEntry("saliente", 6);
        assertThat(entranteBodega()).isEqualTo(6);

        cerrar(ordenId, planificador, "El proveedor no repone esta referencia")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"))
                .andExpect(jsonPath("$.cantidadPendiente").value(6))
                .andExpect(jsonPath("$.motivoCierre").value("El proveedor no repone esta referencia"))
                .andExpect(jsonPath("$.cerradaPor").value(planificador.id().toString()))
                .andExpect(jsonPath("$.fechaCierre").exists());

        assertThat(cd()).containsEntry("saliente", 0);
        assertThat(entranteBodega()).isZero();
        cerrar(ordenId, planificador, "Otra vez").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("""
            Escenario: cancelar una orden sin CD
              Dada una orden GENERADA porque ningún CD tenía disponible
              Cuando el planificador la cancela con un motivo
              Entonces queda CANCELADA sin CD y el inventario no cambia""")
    void cancelarSinCd() throws Exception {
        UUID ordenId = red.crearOrden(tiendaId, varianteId, 50);
        assertThat(red.estadoOrden(ordenId)).isEqualTo("GENERADA");

        mockMvc.perform(post("/api/v1/ordenes-reabastecimiento/{id}/cancelar", ordenId)
                        .header(HttpHeaders.AUTHORIZATION, planificador.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"Ningún CD tiene la referencia\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.cdAsignadoId").doesNotExist());

        assertThat(cd()).containsEntry("a_la_mano", 10).containsEntry("saliente", 0);
    }

    @Test
    @DisplayName("Estado inválido 409, motivo vacío 400, tienda ajena 404 y sin scope 403")
    void reglas() throws Exception {
        UUID aprobada = red.crearOrden(tiendaId, varianteId, 5);

        cerrar(aprobada, planificador, "No aplica").andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/ordenes-reabastecimiento/{id}/cancelar", aprobada)
                        .header(HttpHeaders.AUTHORIZATION, planificador.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\": \"No aplica\"}"))
                .andExpect(status().isConflict());
        cerrar(aprobada, planificador, " ").andExpect(status().isBadRequest());
        UUID otraTienda = red.crearTienda(cdId);
        cerrar(aprobada, usuarios.conRol("PLANIFICADOR_ABASTECIMIENTO", otraTienda), "Ajena")
                .andExpect(status().isNotFound());
        cerrar(aprobada, usuarios.conScopes(Set.of(), tiendaId), "Sin scope").andExpect(status().isForbidden());
        assertThat(red.estadoOrden(aprobada)).isEqualTo("APROBADA");
    }

    private ResultActions cerrar(UUID ordenId, UsuarioDePrueba usuario, String motivo) throws Exception {
        return mockMvc.perform(post("/api/v1/ordenes-reabastecimiento/{id}/cerrar", ordenId)
                .header(HttpHeaders.AUTHORIZATION, usuario.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"%s\"}".formatted(motivo)));
    }

    /** Salida del viaje y recepción en la Bodega con los contratos reales (lo que hacen HU-48 y HU-49). */
    private void recibir(UUID ordenId, int cantidad) {
        tx.executeWithoutResult(estado -> {
            transiciones.marcarEnTransito(ordenId);
            movimientos.registrarRecepcion(tiendaId, varianteId, cantidad, UUID.randomUUID());
            transiciones.registrarRecepcion(ordenId, cantidad);
        });
    }

    /** Simula una merma en el CD sin tocar la reserva, respetando los CHECK de V29. */
    private void fijarALaManoCd(int aLaMano) {
        jdbc.update("""
                UPDATE inventario SET a_la_mano = ?, disponible_para_uso = ? - saliente,
                                      pronosticado = ? + entrante - saliente
                WHERE nodo_id = (SELECT id FROM nodo WHERE cd_id = ?) AND variante_id = ?""",
                aLaMano, aLaMano, aLaMano, cdId, varianteId);
    }

    private Map<String, Object> cd() {
        return jdbc.queryForMap("""
                SELECT a_la_mano, saliente FROM inventario
                WHERE nodo_id = (SELECT id FROM nodo WHERE cd_id = ?) AND variante_id = ?""", cdId, varianteId);
    }

    private int entranteBodega() {
        return jdbc.queryForObject("""
                SELECT entrante FROM inventario
                WHERE nodo_id = (SELECT id FROM nodo WHERE tienda_id = ? AND tipo = 'BODEGA_TIENDA') AND variante_id = ?""",
                Integer.class, tiendaId, varianteId);
    }
}
