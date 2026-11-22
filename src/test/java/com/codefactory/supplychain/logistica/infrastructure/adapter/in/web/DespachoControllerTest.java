package com.codefactory.supplychain.logistica.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.reposicion.api.TransicionesOrden;
import com.codefactory.supplychain.reposicion.application.port.in.CrearOrdenUseCase;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-46 de extremo a extremo contra PostgreSQL real: la orden se crea con HU-45 y se despacha
 * con los contratos reales de reposición e inventario.
 *
 * Módulo: logistica.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(UsuariosDePrueba.class)
@DisplayName("Despacho de una orden (HU-46)")
class DespachoControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuariosDePrueba usuarios;
    @Autowired private InventarioUseCase inventario;
    @Autowired private CrearOrdenUseCase crearOrden;
    @Autowired private TransicionesOrden transiciones;
    @Autowired private TransactionTemplate tx;

    private UUID tiendaId;
    private UUID bodegaId;
    private UUID varianteId;
    private UUID nodoCd;
    private UsuarioDePrueba operador;

    @BeforeEach
    void setUp() {
        tiendaId = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')", tiendaId, "Tienda " + tiendaId);
        bodegaId = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'BODEGA_TIENDA', ?)", bodegaId, tiendaId);
        varianteId = crearVariante();
        UUID cdId = UUID.randomUUID();
        jdbc.update("INSERT INTO cd (id, nombre, ubicacion) VALUES (?, ?, 'Bogotá')", cdId, "CD " + cdId);
        nodoCd = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, cd_id) VALUES (?, 'CD', ?)", nodoCd, cdId);
        inventario.cargar(nodoCd, varianteId, 30, null);
        jdbc.update("INSERT INTO tienda_cd_prioridad (tienda_id, cd_id, orden_prioridad) VALUES (?, ?, 1)", tiendaId, cdId);
        operador = usuarios.conRol("OPERADOR_CD");
    }

    @Test
    @DisplayName("""
            Escenario: despacho total
              Dado que el CD tiene a la mano la cantidad solicitada
              Cuando registro el despacho
              Entonces se crea un ciclo COMPLETO con fill rate 1
              Y la orden queda DESPACHADA
              Y el inventario del CD se descuenta con kardex DESPACHO y la reserva se libera""")
    void despachoTotal() throws Exception {
        UUID ordenId = crearOrden(10);

        despachar(ordenId, operador)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCiclo").value(1))
                .andExpect(jsonPath("$.cantidadSolicitada").value(10))
                .andExpect(jsonPath("$.cantidadDespachada").value(10))
                .andExpect(jsonPath("$.cantidadPendiente").value(0))
                .andExpect(jsonPath("$.resultado").value("COMPLETO"))
                .andExpect(jsonPath("$.fillRate").value(1.0));

        assertThat(estadoOrden(ordenId)).isEqualTo("DESPACHADA");
        assertThat(cd()).containsEntry("a_la_mano", 20).containsEntry("saliente", 0);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movimiento_inventario WHERE nodo_id = ? AND tipo = 'DESPACHO'",
                Integer.class, nodoCd)).isEqualTo(1);
    }

    @Test
    @DisplayName("""
            Escenario: despacho parcial
              Dada una orden de 10 y un CD que perdió stock y solo tiene 4 a la mano
              Cuando registro el despacho
              Entonces salen 4, el ciclo queda PARCIAL con 6 pendientes y fill rate 0.4
              Y la reserva restante (6) se conserva""")
    void despachoParcial() throws Exception {
        UUID ordenId = crearOrden(10);
        fijarALaManoCd(4);

        despachar(ordenId, operador)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadDespachada").value(4))
                .andExpect(jsonPath("$.cantidadPendiente").value(6))
                .andExpect(jsonPath("$.resultado").value("PARCIAL"))
                .andExpect(jsonPath("$.fillRate").value(0.4));

        assertThat(cd()).containsEntry("a_la_mano", 0).containsEntry("saliente", 6);
    }

    @Test
    @DisplayName("""
            Escenario: sin stock real
              Dado que el CD no tiene nada a la mano
              Cuando intento despachar
              Entonces responde 409 y no crea ciclo ni cambia la orden""")
    void sinStockResponde409SinTocarNada() throws Exception {
        UUID ordenId = crearOrden(10);
        fijarALaManoCd(0);

        despachar(ordenId, operador).andExpect(status().isConflict());

        assertThat(estadoOrden(ordenId)).isEqualTo("APROBADA");
        assertThat(ciclosDe(ordenId)).isZero();
        assertThat(cd()).containsEntry("saliente", 10);
    }

    @Test
    @DisplayName("""
            Escenario: estado inválido
              Dado que la orden ya está DESPACHADA
              Cuando intento despacharla otra vez, responde 409
              Y una orden inexistente responde 404""")
    void estadoInvalidoResponde409() throws Exception {
        UUID ordenId = crearOrden(10);
        despachar(ordenId, operador).andExpect(status().isCreated());

        despachar(ordenId, operador).andExpect(status().isConflict());
        despachar(UUID.randomUUID(), operador).andExpect(status().isNotFound());
        assertThat(ciclosDe(ordenId)).isEqualTo(1);
    }

    @Test
    @DisplayName("""
            Escenario: nuevo ciclo tras reapertura
              Dada una orden de 10 despachada en parte (4), en tránsito y recibida en parte (4): REABIERTA con 6
              Y un CD que vuelve a tener 10 a la mano
              Cuando se despacha el pendiente
              Entonces se crea el ciclo 2 de la misma orden con 6 solicitados y la reserva queda en 0""")
    void nuevoCicloTrasReapertura() throws Exception {
        UUID ordenId = crearOrden(10);
        fijarALaManoCd(4);
        despachar(ordenId, operador).andExpect(status().isCreated());
        tx.executeWithoutResult(estado -> {
            transiciones.marcarEnTransito(ordenId);
            transiciones.registrarRecepcion(ordenId, 4);
        });
        assertThat(estadoOrden(ordenId)).isEqualTo("REABIERTA");
        fijarALaManoCd(10);

        despachar(ordenId, operador)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCiclo").value(2))
                .andExpect(jsonPath("$.cantidadSolicitada").value(6))
                .andExpect(jsonPath("$.resultado").value("COMPLETO"));

        assertThat(cd()).containsEntry("a_la_mano", 4).containsEntry("saliente", 0);
        mockMvc.perform(get("/api/v1/ciclos-despacho").param("ordenId", ordenId.toString())
                        .header(HttpHeaders.AUTHORIZATION, usuarios.conRol("COORDINADOR_TRANSPORTE").bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].numeroCiclo").value(2));
    }

    @Test
    @DisplayName("Dos despachos simultáneos de la misma orden: uno crea el ciclo y el otro responde 409")
    void dosDespachosSimultaneos() throws Exception {
        UUID ordenId = crearOrden(10);
        CountDownLatch salida = new CountDownLatch(1);
        Callable<Integer> intento = () -> {
            salida.await();
            return despachar(ordenId, operador).andReturn().getResponse().getStatus();
        };
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        var resultados = List.of(hilos.submit(intento), hilos.submit(intento));
        salida.countDown();
        List<Integer> estados = List.of(resultados.get(0).get(30, TimeUnit.SECONDS),
                resultados.get(1).get(30, TimeUnit.SECONDS));
        hilos.shutdown();

        assertThat(estados).containsExactlyInAnyOrder(201, 409);
        assertThat(ciclosDe(ordenId)).isEqualTo(1);
        assertThat(cd()).containsEntry("a_la_mano", 20);
    }

    @Test
    @DisplayName("Consultar ciclos: por id, filtro sinViaje y 404")
    void consultarCiclos() throws Exception {
        UUID ordenId = crearOrden(10);
        String cicloId = com.jayway.jsonpath.JsonPath.read(
                despachar(ordenId, operador).andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/ciclos-despacho/{id}", cicloId).header(HttpHeaders.AUTHORIZATION, operador.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordenId").value(ordenId.toString()));
        mockMvc.perform(get("/api/v1/ciclos-despacho").param("ordenId", ordenId.toString()).param("sinViaje", "true")
                        .header(HttpHeaders.AUTHORIZATION, operador.bearer()))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/ciclos-despacho/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, operador.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sin token 401; sin despacho:operar, despachar responde 403 y el coordinador solo consulta")
    void seguridad() throws Exception {
        UUID ordenId = crearOrden(10);
        mockMvc.perform(post("/api/v1/ordenes-reabastecimiento/{id}/despachos", ordenId))
                .andExpect(status().isUnauthorized());
        despachar(ordenId, usuarios.conRol("COORDINADOR_TRANSPORTE")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/ciclos-despacho").header(HttpHeaders.AUTHORIZATION, usuarios.conScopes(Set.of()).bearer()))
                .andExpect(status().isForbidden());
        assertThat(estadoOrden(ordenId)).isEqualTo("APROBADA");
    }

    private ResultActions despachar(UUID ordenId, UsuarioDePrueba usuario) throws Exception {
        return mockMvc.perform(post("/api/v1/ordenes-reabastecimiento/{id}/despachos", ordenId)
                .header(HttpHeaders.AUTHORIZATION, usuario.bearer()));
    }

    private UUID crearOrden(int cantidad) {
        UUID ordenId = tx.execute(estado -> crearOrden.crear(null, tiendaId, varianteId, cantidad)).getId();
        assertThat(estadoOrden(ordenId)).isEqualTo("APROBADA");
        return ordenId;
    }

    /** Simula una merma o un ingreso en el CD sin tocar la reserva, respetando los CHECK de V29. */
    private void fijarALaManoCd(int aLaMano) {
        jdbc.update("""
                UPDATE inventario SET a_la_mano = ?, disponible_para_uso = ? - saliente,
                                      pronosticado = ? + entrante - saliente
                WHERE nodo_id = ? AND variante_id = ?""", aLaMano, aLaMano, aLaMano, nodoCd, varianteId);
    }

    private Map<String, Object> cd() {
        return jdbc.queryForMap("SELECT a_la_mano, saliente FROM inventario WHERE nodo_id = ? AND variante_id = ?",
                nodoCd, varianteId);
    }

    private String estadoOrden(UUID ordenId) {
        return jdbc.queryForObject("SELECT estado FROM orden_reabastecimiento WHERE id = ?", String.class, ordenId);
    }

    private int ciclosDe(UUID ordenId) {
        return jdbc.queryForObject("SELECT count(*) FROM ciclo_despacho WHERE orden_id = ?", Integer.class, ordenId);
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
