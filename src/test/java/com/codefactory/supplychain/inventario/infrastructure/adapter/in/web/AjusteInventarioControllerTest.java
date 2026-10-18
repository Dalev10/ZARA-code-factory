package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de aceptación de HU-29 (ajuste de inventario) de extremo a
 * extremo, contra PostgreSQL real.
 *
 * TODO(shared): los criterios "no puede dejar el stock en negativo" (422) y
 * "no se ajusta una variante no cargada" (409) responden hoy 400, porque
 * GlobalExceptionHandler traduce toda ReglaDeNegocioException a 400. Al
 * cambiar shared/, actualizar los status esperados de esos dos escenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(UsuariosDePrueba.class)
class AjusteInventarioControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UsuariosDePrueba usuarios;

    private UUID tiendaId;
    private UUID almacenId;
    private UsuarioDePrueba supervisor;

    @BeforeEach
    void setUp() {
        tiendaId = crearTienda();
        almacenId = crearNodoDeTienda(tiendaId);
        supervisor = usuarios.conRol("SUPERVISOR_TIENDA", tiendaId);
    }

    @Test
    @DisplayName("""
            Escenario: merma reduce el stock y queda en kardex
              Dado un Almacén con la variante X con 5 unidades
              Cuando se registra un ajuste MERMA de -2 unidades con el usuario U
              Entonces a_la_mano es 3
              Y el kardex tiene un movimiento AJUSTE con motivo MERMA y usuario U""")
    void mermaReduceElStockYQuedaEnKardex() throws Exception {
        String x = crearVarianteCargada(almacenId, 5);

        ajustar(almacenId, supervisor, x, -2, "MERMA", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.delta").value(-2))
                .andExpect(jsonPath("$.motivo").value("MERMA"))
                .andExpect(jsonPath("$.aLaMano").value(3));

        assertThat(aLaMano(almacenId, x)).isEqualTo(3);
        Map<String, Object> movimiento = jdbc.queryForMap("""
                SELECT m.cantidad, m.motivo, m.usuario_id FROM movimiento_inventario m
                  JOIN variante v ON v.id = m.variante_id
                 WHERE m.nodo_id = ? AND v.sku = ? AND m.tipo = 'AJUSTE'""", almacenId, x);
        assertThat(movimiento)
                .containsEntry("cantidad", -2)
                .containsEntry("motivo", "MERMA")
                .containsEntry("usuario_id", supervisor.id());
    }

    @Test
    @DisplayName("""
            Escenario: el ajuste no puede dejar el stock en negativo
              Dado un Almacén con la variante X con 1 unidad
              Cuando se registra un ajuste de -2 unidades
              Entonces la respuesta es 422 (hoy 400, pendiente del cambio en shared)
              Y el stock sigue en 1""")
    void elAjusteNoPuedeDejarElStockEnNegativo() throws Exception {
        String x = crearVarianteCargada(almacenId, 1);

        ajustar(almacenId, supervisor, x, -2, "CONTEO_FISICO", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("negativo")));

        assertThat(aLaMano(almacenId, x)).isEqualTo(1);
        assertThat(contarAjustes(almacenId)).isZero();
    }

    @Test
    @DisplayName("""
            Escenario: no se ajusta una variante no cargada
              Dado un Almacén sin fila para la variante Z
              Cuando se registra un ajuste sobre Z
              Entonces la respuesta es 409 indicando que la variante no está cargada (hoy 400)
              Y no se crea ninguna fila""")
    void noSeAjustaUnaVarianteNoCargada() throws Exception {
        String z = crearVariante();

        ajustar(almacenId, supervisor, z, 3, "INGRESO_MANUAL", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("no está cargada")));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventario WHERE nodo_id = ?",
                Integer.class, almacenId)).isZero();
    }

    @Test
    @DisplayName("""
            Escenario: una corrección exige comentario
              Dado un Almacén con la variante X con 4 unidades
              Cuando se registra un ajuste CORRECCION sin comentario
              Entonces la respuesta es 400
              Cuando se registra con comentario
              Entonces la respuesta es 201 y el kardex guarda el comentario""")
    void unaCorreccionExigeComentario() throws Exception {
        String x = crearVarianteCargada(almacenId, 4);

        ajustar(almacenId, supervisor, x, 1, "CORRECCION", null).andExpect(status().isBadRequest());
        ajustar(almacenId, supervisor, x, 1, "CORRECCION", "Conteo mal digitado")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comentario").value("Conteo mal digitado"));

        assertThat(aLaMano(almacenId, x)).isEqualTo(5);
    }

    @Test
    void rechazaRequestsInvalidos() throws Exception {
        String x = crearVarianteCargada(almacenId, 4);

        ajustar(almacenId, supervisor, x, 1, "REGALO", null).andExpect(status().isBadRequest());
        ajustar(almacenId, supervisor, x, 0, "MERMA", null).andExpect(status().isBadRequest());
        ajustar(almacenId, supervisor, "SKU-QUE-NO-EXISTE", 1, "MERMA", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("SKU-QUE-NO-EXISTE")));
        enviar(almacenId, supervisor, "{\"sku\": \"%s\", \"motivo\": \"MERMA\"}".formatted(x))
                .andExpect(status().isBadRequest());

        assertThat(aLaMano(almacenId, x)).isEqualTo(4);
    }

    @Test
    @DisplayName("""
            Escenario: un CD exige el scope de inventario de CD
              Dado un CD con la variante X cargada
              Cuando un supervisor de tienda la ajusta
              Entonces la respuesta es 403
              Cuando un operador de CD la ajusta
              Entonces la respuesta es 201""")
    void unCdExigeElScopeDeInventarioCd() throws Exception {
        UUID cdNodoId = crearNodoDeCd();
        UsuarioDePrueba operadorCd = usuarios.conRol("OPERADOR_CD");
        String x = crearVariante();
        cargar(cdNodoId, operadorCd, x, 10);

        ajustar(cdNodoId, supervisor, x, 5, "INGRESO_MANUAL", null).andExpect(status().isForbidden());
        ajustar(cdNodoId, operadorCd, x, 5, "INGRESO_MANUAL", null).andExpect(status().isCreated());

        assertThat(aLaMano(cdNodoId, x)).isEqualTo(15);
    }

    @Test
    @DisplayName("""
            Escenario: un usuario de tienda no ajusta una tienda ajena
              Dado un supervisor asignado solo a la tienda T1
              Cuando ajusta una variante del Almacén de T2
              Entonces la respuesta es 404""")
    void unUsuarioDeTiendaNoAjustaUnaTiendaAjena() throws Exception {
        UUID tiendaAjena = crearTienda();
        UUID almacenAjeno = crearNodoDeTienda(tiendaAjena);
        String x = crearVariante();
        cargar(almacenAjeno, usuarios.conRol("SUPERVISOR_TIENDA", tiendaAjena), x, 3);

        ajustar(almacenAjeno, supervisor, x, -1, "MERMA", null).andExpect(status().isNotFound());
        assertThat(aLaMano(almacenAjeno, x)).isEqualTo(3);
    }

    private ResultActions ajustar(UUID nodoId, UsuarioDePrueba usuario, String sku, int delta,
                                  String motivo, String comentario) throws Exception {
        String campoComentario = comentario == null ? "" : ", \"comentario\": \"%s\"".formatted(comentario);
        return enviar(nodoId, usuario, "{\"sku\": \"%s\", \"delta\": %d, \"motivo\": \"%s\"%s}"
                .formatted(sku, delta, motivo, campoComentario));
    }

    private ResultActions enviar(UUID nodoId, UsuarioDePrueba usuario, String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/v1/nodos/{nodoId}/inventario/ajustes", nodoId)
                .header(HttpHeaders.AUTHORIZATION, usuario.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private void cargar(UUID nodoId, UsuarioDePrueba usuario, String sku, int cantidad) throws Exception {
        mockMvc.perform(post("/api/v1/nodos/{nodoId}/inventario/carga-inicial", nodoId)
                        .header(HttpHeaders.AUTHORIZATION, usuario.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [{\"sku\": \"%s\", \"cantidad\": %d}]}".formatted(sku, cantidad)))
                .andExpect(status().isCreated());
    }

    private String crearVarianteCargada(UUID nodoId, int cantidad) throws Exception {
        String sku = crearVariante();
        cargar(nodoId, supervisor, sku, cantidad);
        return sku;
    }

    private int aLaMano(UUID nodoId, String sku) {
        return jdbc.queryForObject("""
                SELECT i.a_la_mano FROM inventario i JOIN variante v ON v.id = i.variante_id
                 WHERE i.nodo_id = ? AND v.sku = ?""", Integer.class, nodoId, sku);
    }

    private int contarAjustes(UUID nodoId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM movimiento_inventario WHERE nodo_id = ? AND tipo = 'AJUSTE'",
                Integer.class, nodoId);
    }

    private UUID crearTienda() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')",
                id, "Tienda Ajuste " + id);
        return id;
    }

    private UUID crearNodoDeTienda(UUID tienda) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'ALMACEN', ?)", id, tienda);
        return id;
    }

    private UUID crearNodoDeCd() {
        UUID cdId = UUID.randomUUID();
        jdbc.update("INSERT INTO cd (id, nombre) VALUES (?, ?)", cdId, "CD Ajuste " + cdId);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, cd_id) VALUES (?, 'CD', ?)", id, cdId);
        return id;
    }

    private String crearVariante() {
        UUID categoriaId = UUID.randomUUID();
        jdbc.update("INSERT INTO categoria (id, nombre) VALUES (?, ?)", categoriaId, "Categoría " + categoriaId);
        UUID templateId = UUID.randomUUID();
        jdbc.update("INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)",
                templateId, categoriaId, "Template " + templateId);
        String sku = "SKU-" + UUID.randomUUID();
        jdbc.update("INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)", UUID.randomUUID(), templateId, sku);
        return sku;
    }
}
