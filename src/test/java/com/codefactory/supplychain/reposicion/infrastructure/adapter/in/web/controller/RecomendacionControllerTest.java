package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.controller;

import com.codefactory.supplychain.inventario.api.ReservasInventario;
import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.reposicion.application.port.in.TraspasoManualUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.ResolverNivelPort;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import com.codefactory.supplychain.soporte.UsuariosDePrueba;
import com.codefactory.supplychain.soporte.UsuariosDePrueba.UsuarioDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de HU-43 de extremo a extremo contra PostgreSQL real.
 * <p>
 * El nivel de reposición (HD-04) aún no está en {@code develop}: se usa un doble de
 * {@link ResolverNivelPort}. El scope {@code reposicion:planificar} lo siembra HD-03
 * (V30); mientras tanto la prueba lo inserta si falta.
 *
 * Módulo: reposicion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import({UsuariosDePrueba.class, RecomendacionControllerTest.NivelesDePrueba.class})
@DisplayName("RecomendacionController (HU-43)")
class RecomendacionControllerTest {

    private static final String PLANIFICAR = "reposicion:planificar";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuariosDePrueba usuarios;
    @Autowired private InventarioUseCase inventario;
    @Autowired private ReservasInventario reservas;
    @Autowired private TraspasoManualUseCase traspasoManual;
    @Autowired private TransactionTemplate tx;
    @Autowired private NivelesDePrueba niveles;

    private UUID tiendaId;
    private UUID bodegaId;
    private UUID almacenId;
    private UUID varianteId;
    private UsuarioDePrueba planificador;

    @BeforeEach
    void setUp() {
        jdbc.update("""
                INSERT INTO scope (codigo, descripcion, sensible) VALUES (?, 'Planificar reposición al CD', false)
                ON CONFLICT (codigo) DO NOTHING""", PLANIFICAR);
        tiendaId = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')",
                tiendaId, "Tienda " + tiendaId);
        bodegaId = crearNodoTienda("BODEGA_TIENDA");
        almacenId = crearNodoTienda("ALMACEN");
        varianteId = crearVariante();
        planificador = usuarios.conScopes(Set.of(PLANIFICAR), tiendaId);
    }

    @Test
    @DisplayName("""
            Escenario: Recomendación por consumo de la Bodega_Tienda
              Dado que el pronosticado de una Bodega_Tienda para un SKU cae bajo el mínimo del nivel configurado
              Cuando el sistema evalúa la reposición
              Entonces crea una Recomendación en estado BORRADOR
              Y la cantidad_sugerida es el objetivo menos el pronosticado""")
    void recomendacionPorConsumo() throws Exception {
        inventario.cargar(bodegaId, varianteId, 3, null);
        niveles.configurar(varianteId, 5, 15);

        evaluar(planificador, tiendaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recomendacionesCreadas").value(1))
                .andExpect(jsonPath("$.recomendaciones[0].estado").value("BORRADOR"))
                .andExpect(jsonPath("$.recomendaciones[0].cantidadSugerida").value(12))
                .andExpect(jsonPath("$.recomendaciones[0].varianteId").value(varianteId.toString()));
    }

    @Test
    @DisplayName("""
            Escenario: CD sugerido blando
              Dado que la tienda tiene una lista de CDs por prioridad
              Cuando se genera la Recomendación
              Entonces se sugiere el CD de mayor prioridad
              Y no se valida stock real todavía""")
    void cdSugeridoBlando() throws Exception {
        UUID cdSecundario = crearCd();
        UUID cdPrincipal = crearCd(); // sin stock: no se valida
        priorizar(cdPrincipal, 1);
        priorizar(cdSecundario, 2);
        inventario.cargar(bodegaId, varianteId, 1, null);
        niveles.configurar(varianteId, 5, 10);

        evaluar(planificador, tiendaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recomendaciones[0].cdSugeridoId").value(cdPrincipal.toString()));
    }

    @Test
    @DisplayName("""
            Escenario: Sin duplicados
              Dado que ya existe una Recomendación en BORRADOR para el mismo SKU y tienda
              Cuando el sistema vuelve a evaluar
              Entonces no crea otra""")
    void sinDuplicados() throws Exception {
        inventario.cargar(bodegaId, varianteId, 2, null);
        niveles.configurar(varianteId, 5, 10);

        evaluar(planificador, tiendaId).andExpect(jsonPath("$.recomendacionesCreadas").value(1));
        evaluar(planificador, tiendaId).andExpect(jsonPath("$.recomendacionesCreadas").value(0));

        assertThat(recomendacionesEnBd()).isEqualTo(1);
    }

    @Test
    @DisplayName("""
            Escenario: Orden ya en camino
              Dado que existe una orden con CD asignado y su entrante ya está sumado en la Bodega
              Cuando el sistema evalúa
              Entonces el pronosticado ya la incluye y no se vuelve a recomendar lo pedido""")
    void ordenYaEnCamino() throws Exception {
        UUID cdId = crearCd();
        UUID nodoCd = crearNodoCd(cdId);
        inventario.cargar(nodoCd, varianteId, 50, null);
        inventario.cargar(bodegaId, varianteId, 3, null);
        niveles.configurar(varianteId, 5, 15);
        // La orden con CD reserva: entrante de la Bodega += 12 → pronosticado 15.
        var resultado = tx.execute(estado -> reservas.reservar(cdId, tiendaId, varianteId, 12, UUID.randomUUID()));
        assertThat(resultado.asignada()).isTrue();

        evaluar(planificador, tiendaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recomendacionesCreadas").value(0));
    }

    @Test
    @DisplayName("""
            Escenario: Orden sin CD
              Dado que existe una orden GENERADA sin CD para el SKU y la tienda
              Cuando el sistema evalúa
              Entonces no genera una nueva Recomendación""")
    void ordenSinCd() throws Exception {
        inventario.cargar(bodegaId, varianteId, 1, null);
        niveles.configurar(varianteId, 5, 10);
        jdbc.update("""
                INSERT INTO orden_reabastecimiento (id, tienda_id, variante_id, cantidad_total_solicitada,
                    cantidad_pendiente, estado)
                VALUES (?, ?, ?, 9, 9, 'GENERADA')""", UUID.randomUUID(), tiendaId, varianteId);

        evaluar(planificador, tiendaId).andExpect(jsonPath("$.recomendacionesCreadas").value(0));
        assertThat(recomendacionesEnBd()).isZero();
    }

    @Test
    @DisplayName("""
            Escenario: Sin nivel configurado
              Dado que el SKU no tiene nivel de reposición
              Cuando el sistema evalúa
              Entonces no genera Recomendación""")
    void sinNivelConfigurado() throws Exception {
        inventario.cargar(bodegaId, varianteId, 1, null);

        evaluar(planificador, tiendaId).andExpect(jsonPath("$.recomendacionesCreadas").value(0));
        assertThat(recomendacionesEnBd()).isZero();
    }

    @Test
    @DisplayName("""
            Escenario: Tiempo real
              Dado un traspaso o una venta que deja la Bodega bajo el mínimo
              Cuando se recibe la respuesta
              Entonces un GET inmediato ya muestra la Recomendación""")
    void tiempoReal() throws Exception {
        inventario.cargar(bodegaId, varianteId, 10, null);
        niveles.configurar(varianteId, 5, 12);

        // Traspaso de 8: la Bodega queda en 2 (TRASPASO_SALIDA, que el listener del traspaso ignora).
        traspasoManual.registrar(operadorDeTienda(), tiendaId, varianteId, 8);

        mockMvc.perform(get("/api/v1/recomendaciones")
                        .param("tiendaId", tiendaId.toString())
                        .param("estado", "BORRADOR")
                        .header(HttpHeaders.AUTHORIZATION, planificador.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].cantidadSugerida").value(10))
                .andExpect(jsonPath("$.content[0].cdSugeridoId").value(nullValue()));
    }

    @Test
    @DisplayName("""
            Escenario: una falla del listener no revierte el movimiento de origen
              Dado que la evaluación de la recomendación falla
              Cuando se registra un traspaso que baja la Bodega
              Entonces el traspaso queda registrado y no hay recomendación""")
    void fallaDelListenerNoRevierteElOrigen() {
        inventario.cargar(bodegaId, varianteId, 10, null);
        niveles.fallarPara(varianteId);

        var traspaso = traspasoManual.registrar(operadorDeTienda(), tiendaId, varianteId, 8);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM traspaso_interno WHERE id = ?",
                Integer.class, traspaso.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT a_la_mano FROM inventario WHERE nodo_id = ? AND variante_id = ?",
                Integer.class, bodegaId, varianteId)).isEqualTo(2);
        assertThat(recomendacionesEnBd()).isZero();
    }

    @Test
    @DisplayName("GET por id de una tienda fuera del alcance responde 404")
    void obtenerFueraDeAlcance() throws Exception {
        inventario.cargar(bodegaId, varianteId, 1, null);
        niveles.configurar(varianteId, 5, 10);
        String id = evaluar(planificador, tiendaId).andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"recomendaciones\":\\[\\{\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/v1/recomendaciones/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, planificador.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        UsuarioDePrueba ajeno = usuarios.conScopes(Set.of(PLANIFICAR));
        mockMvc.perform(get("/api/v1/recomendaciones/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, ajeno.bearer()))
                .andExpect(status().isNotFound());
        evaluar(ajeno, tiendaId).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/recomendaciones").param("tiendaId", tiendaId.toString())
                        .header(HttpHeaders.AUTHORIZATION, ajeno.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("sin el scope reposicion:planificar responde 403")
    void sinScope() throws Exception {
        UsuarioDePrueba sinScope = usuarios.conScopes(Set.of("reposicion:administrar"), tiendaId);
        mockMvc.perform(get("/api/v1/recomendaciones").header(HttpHeaders.AUTHORIZATION, sinScope.bearer()))
                .andExpect(status().isForbidden());
        evaluar(sinScope, tiendaId).andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------

    private ResultActions evaluar(UsuarioDePrueba usuario, UUID tienda) throws Exception {
        return mockMvc.perform(post("/api/v1/recomendaciones/evaluar")
                .param("tiendaId", tienda.toString())
                .header(HttpHeaders.AUTHORIZATION, usuario.bearer()));
    }

    private UsuarioActual operadorDeTienda() {
        return new UsuarioActual(planificador.id(), Set.of("reposicion:administrar"), Set.of(tiendaId));
    }

    private int recomendacionesEnBd() {
        return jdbc.queryForObject("SELECT count(*) FROM recomendacion WHERE tienda_id = ?", Integer.class, tiendaId);
    }

    private UUID crearNodoTienda(String tipo) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, ?, ?)", id, tipo, tiendaId);
        return id;
    }

    private UUID crearCd() {
        UUID cdId = UUID.randomUUID();
        jdbc.update("INSERT INTO cd (id, nombre, ubicacion) VALUES (?, ?, 'Bogotá')", cdId, "CD " + cdId);
        return cdId;
    }

    private UUID crearNodoCd(UUID cdId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, cd_id) VALUES (?, 'CD', ?)", id, cdId);
        return id;
    }

    private void priorizar(UUID cdId, int prioridad) {
        jdbc.update("INSERT INTO tienda_cd_prioridad (tienda_id, cd_id, orden_prioridad) VALUES (?, ?, ?)",
                tiendaId, cdId, prioridad);
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

    /**
     * Doble de {@link ResolverNivelPort}: permite fijar niveles y simular fallas por variante.
     * Es {@code @Primary} porque la implementación real (HD-04) también está en el contexto.
     */
    @TestConfiguration
    @Primary
    static class NivelesDePrueba implements ResolverNivelPort {

        private final Map<UUID, NivelAplicable> porVariante = new ConcurrentHashMap<>();
        private final Set<UUID> conFalla = ConcurrentHashMap.newKeySet();

        void configurar(UUID varianteId, int minimo, int objetivo) {
            porVariante.put(varianteId, new NivelAplicable(minimo, objetivo));
        }

        void fallarPara(UUID varianteId) {
            conFalla.add(varianteId);
        }

        @Override
        public Optional<NivelAplicable> resolver(UUID tiendaId, UUID varianteId) {
            if (conFalla.contains(varianteId)) {
                throw new IllegalStateException("Falla simulada del nivel de reposición");
            }
            return Optional.ofNullable(porVariante.get(varianteId));
        }
    }
}
