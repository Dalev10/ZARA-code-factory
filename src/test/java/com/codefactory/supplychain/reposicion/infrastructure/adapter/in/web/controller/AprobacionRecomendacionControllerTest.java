package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.controller;

import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de extremo a extremo contra PostgreSQL real, sin dobles: la orden la crea
 * el {@code CrearOrdenUseCase} real de HU-45, el nivel sale de V35 y el rol de V34.
 *
 * Módulo: reposicion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(UsuariosDePrueba.class)
@DisplayName("AprobacionRecomendacionController (HU-44)")
class AprobacionRecomendacionControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuariosDePrueba usuarios;
    @Autowired private InventarioUseCase inventario;

    private UUID tiendaId;
    private UUID bodegaId;
    private UUID varianteId;
    private UsuarioDePrueba planificador;

    @BeforeEach
    void setUp() throws Exception {
        tiendaId = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')",
                tiendaId, "Tienda " + tiendaId);
        bodegaId = crearNodoTienda("BODEGA_TIENDA");
        crearNodoTienda("ALMACEN");
        varianteId = crearVariante();
        planificador = usuarios.conRol("PLANIFICADOR_ABASTECIMIENTO", tiendaId);
    }

    @Test
    @DisplayName("""
            Escenario: Aprobar sin cambios
              Dado que existe una Recomendación en BORRADOR
              Cuando la apruebo sin modificar la cantidad
              Entonces cantidad_aprobada es igual a cantidad_sugerida
              Y la Recomendación queda APROBADA y se crea la orden""")
    void aprobarSinCambios() throws Exception {
        UUID cdId = cdConStock(50);
        UUID id = recomendacionEnBorrador(); // sugerida 12

        aprobar(id, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.cantidadSugerida").value(12))
                .andExpect(jsonPath("$.cantidadAprobada").value(12))
                .andExpect(jsonPath("$.desviacion").value(0))
                .andExpect(jsonPath("$.aprobadoPor").value(planificador.id().toString()))
                .andExpect(jsonPath("$.fechaAprobacion").isNotEmpty())
                .andExpect(jsonPath("$.orden.recomendacionId").value(id.toString()))
                .andExpect(jsonPath("$.orden.estado").value("APROBADA"))
                .andExpect(jsonPath("$.orden.cdAsignadoId").value(cdId.toString()))
                .andExpect(jsonPath("$.orden.cantidadTotalSolicitada").value(12));

        assertThat(jdbc.queryForMap("SELECT estado, cantidad_aprobada, aprobado_por FROM recomendacion WHERE id = ?", id))
                .containsEntry("estado", "APROBADA")
                .containsEntry("cantidad_aprobada", 12)
                .containsEntry("aprobado_por", planificador.id());
        assertThat(ordenesDe(id)).isEqualTo(1);
    }

    @Test
    @DisplayName("""
            Escenario: Aprobar con cantidad editada
              Dado que existe una Recomendación en BORRADOR
              Cuando cambio la cantidad y apruebo
              Entonces se guarda cantidad_aprobada
              Y la desviación contra cantidad_sugerida queda visible en la respuesta""")
    void aprobarConCantidadEditada() throws Exception {
        cdConStock(50);
        UUID id = recomendacionEnBorrador();

        aprobar(id, "{\"cantidadAprobada\": 15}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadAprobada").value(15))
                .andExpect(jsonPath("$.desviacion").value(3))
                .andExpect(jsonPath("$.orden.cantidadTotalSolicitada").value(15));

        assertThat(jdbc.queryForObject("SELECT cantidad_aprobada FROM recomendacion WHERE id = ?",
                Integer.class, id)).isEqualTo(15);
    }

    @Test
    @DisplayName("""
            Escenario: Cantidad inválida
              Cuando ingreso una cantidad menor o igual a cero
              Entonces el sistema responde 400, indica el error y no cambia nada""")
    void cantidadInvalida() throws Exception {
        cdConStock(50);
        UUID id = recomendacionEnBorrador();

        for (String cuerpo : List.of("{\"cantidadAprobada\": 0}", "{\"cantidadAprobada\": -3}")) {
            aprobar(id, cuerpo)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value(containsString("mayor que cero")));
        }

        assertThat(estadoDe(id)).isEqualTo("BORRADOR");
        assertThat(jdbc.queryForObject("SELECT cantidad_aprobada FROM recomendacion WHERE id = ?",
                Integer.class, id)).isNull();
        assertThat(ordenesDe(id)).isZero();
    }

    @Test
    @DisplayName("""
            Escenario: Rechazar
              Cuando rechazo una Recomendación
              Entonces queda RECHAZADA, no se genera orden y se conserva el registro""")
    void rechazar() throws Exception {
        UUID id = recomendacionEnBorrador();

        rechazarla(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));

        assertThat(estadoDe(id)).isEqualTo("RECHAZADA");
        assertThat(ordenesDe(id)).isZero();
    }

    @Test
    @DisplayName("""
            Escenario: Ya resuelta
              Dado que la Recomendación no está en BORRADOR
              Cuando intento aprobarla o rechazarla
              Entonces el sistema responde 409 y no crea otra orden""")
    void yaResuelta() throws Exception {
        cdConStock(50);
        UUID aprobada = recomendacionEnBorrador();
        aprobar(aprobada, null).andExpect(status().isCreated());

        aprobar(aprobada, null).andExpect(status().isConflict());
        rechazarla(aprobada).andExpect(status().isConflict());
        assertThat(ordenesDe(aprobada)).isEqualTo(1);
        assertThat(estadoDe(aprobada)).isEqualTo("APROBADA");

        varianteId = crearVariante(); // otro SKU: el entrante de la orden ya cubre el primero
        UUID rechazada = recomendacionEnBorrador();
        rechazarla(rechazada).andExpect(status().isOk());
        aprobar(rechazada, null).andExpect(status().isConflict());
        rechazarla(rechazada).andExpect(status().isConflict());
        assertThat(ordenesDe(rechazada)).isZero();
    }

    @Test
    @DisplayName("aprobar sin stock en ningún CD responde 201 con la orden GENERADA sin CD y la aprobación se conserva")
    void aprobarSinCd() throws Exception {
        UUID cdId = cdConStock(2); // no cubre 12
        UUID id = recomendacionEnBorrador();

        aprobar(id, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.orden.estado").value("GENERADA"))
                .andExpect(jsonPath("$.orden.cdAsignadoId").value(nullValue()))
                .andExpect(jsonPath("$.orden.intentos[0].cdId").value(cdId.toString()))
                .andExpect(jsonPath("$.orden.intentos[0].resultado").value("SIN_STOCK"));

        assertThat(estadoDe(id)).isEqualTo("APROBADA");
    }

    @Test
    @DisplayName("fuera del alcance o inexistente responde 404 y no cambia nada")
    void fueraDeAlcance() throws Exception {
        UUID id = recomendacionEnBorrador();
        UsuarioDePrueba ajeno = usuarios.conRol("PLANIFICADOR_ABASTECIMIENTO");

        mockMvc.perform(conCuerpo(post("/api/v1/recomendaciones/{id}/aprobar", id), ajeno, null))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/recomendaciones/{id}/rechazar", id)
                        .header(HttpHeaders.AUTHORIZATION, ajeno.bearer()))
                .andExpect(status().isNotFound());
        aprobar(UUID.randomUUID(), null).andExpect(status().isNotFound());

        assertThat(estadoDe(id)).isEqualTo("BORRADOR");
    }

    @Test
    @DisplayName("sin el scope reposicion:planificar responde 403")
    void sinScope() throws Exception {
        UUID id = recomendacionEnBorrador();
        UsuarioDePrueba sinScope = usuarios.conScopes(Set.of("reposicion:administrar"), tiendaId);

        mockMvc.perform(conCuerpo(post("/api/v1/recomendaciones/{id}/aprobar", id), sinScope, null))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/recomendaciones/{id}/rechazar", id)
                        .header(HttpHeaders.AUTHORIZATION, sinScope.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("dos aprobaciones simultáneas: una crea la orden y la otra responde 409")
    void aprobacionesSimultaneas() throws Exception {
        cdConStock(100);
        UUID id = recomendacionEnBorrador();
        int hilos = 4;
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        try {
            List<Future<Integer>> resultados = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                Callable<Integer> tarea = () -> {
                    salida.await();
                    return aprobar(id, null).andReturn().getResponse().getStatus();
                };
                resultados.add(pool.submit(tarea));
            }
            salida.countDown();
            List<Integer> codigos = new ArrayList<>();
            for (Future<Integer> resultado : resultados) {
                codigos.add(resultado.get());
            }
            assertThat(codigos).containsOnly(201, 409);
            assertThat(codigos).filteredOn(codigo -> codigo == 201).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(ordenesDe(id)).isEqualTo(1);
    }

    // -------------------------------------------------------------------------

    private ResultActions aprobar(UUID id, String cuerpo) throws Exception {
        return mockMvc.perform(conCuerpo(post("/api/v1/recomendaciones/{id}/aprobar", id), planificador, cuerpo));
    }

    private ResultActions rechazarla(UUID id) throws Exception {
        return mockMvc.perform(post("/api/v1/recomendaciones/{id}/rechazar", id)
                .header(HttpHeaders.AUTHORIZATION, planificador.bearer()));
    }

    private static MockHttpServletRequestBuilder conCuerpo(MockHttpServletRequestBuilder request,
                                                           UsuarioDePrueba usuario, String cuerpo) {
        request.header(HttpHeaders.AUTHORIZATION, usuario.bearer());
        return cuerpo == null ? request : request.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    /** Bodega en 3 y nivel 5/15: la evaluación crea un BORRADOR con sugerida 12. */
    private UUID recomendacionEnBorrador() throws Exception {
        inventario.cargar(bodegaId, varianteId, 3, null);
        mockMvc.perform(post("/api/v1/niveles-reposicion-cd")
                        .header(HttpHeaders.AUTHORIZATION, planificador.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"varianteId\": \"%s\", \"cantidadMinima\": 5, \"cantidadObjetivo\": 15}"
                                .formatted(varianteId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/recomendaciones/evaluar")
                        .param("tiendaId", tiendaId.toString())
                        .header(HttpHeaders.AUTHORIZATION, planificador.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recomendacionesCreadas").value(1));
        return jdbc.queryForObject("""
                SELECT id FROM recomendacion WHERE tienda_id = ? AND variante_id = ? AND estado = 'BORRADOR'""",
                UUID.class, tiendaId, varianteId);
    }

    private UUID cdConStock(int cantidad) {
        UUID cdId = UUID.randomUUID();
        jdbc.update("INSERT INTO cd (id, nombre, ubicacion) VALUES (?, ?, 'Bogotá')", cdId, "CD " + cdId);
        UUID nodoCd = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, cd_id) VALUES (?, 'CD', ?)", nodoCd, cdId);
        jdbc.update("INSERT INTO tienda_cd_prioridad (tienda_id, cd_id, orden_prioridad) VALUES (?, ?, 1)",
                tiendaId, cdId);
        inventario.cargar(nodoCd, varianteId, cantidad, null);
        return cdId;
    }

    private String estadoDe(UUID id) {
        return jdbc.queryForObject("SELECT estado FROM recomendacion WHERE id = ?", String.class, id);
    }

    private int ordenesDe(UUID recomendacionId) {
        return jdbc.queryForObject("SELECT count(*) FROM orden_reabastecimiento WHERE recomendacion_id = ?",
                Integer.class, recomendacionId);
    }

    private UUID crearNodoTienda(String tipo) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, ?, ?)", id, tipo, tiendaId);
        return id;
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
