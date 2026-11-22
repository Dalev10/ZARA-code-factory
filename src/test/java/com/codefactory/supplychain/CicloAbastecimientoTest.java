package com.codefactory.supplychain;

import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.soporte.AbastecimientoDePrueba;
import com.codefactory.supplychain.soporte.UsuariosDePrueba;
import com.codefactory.supplychain.soporte.UsuariosDePrueba.UsuarioDePrueba;
import com.jayway.jsonpath.JsonPath;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HD-06: el ciclo completo de abastecimiento por HTTP, cada paso con el rol que lo hace,
 * comprobando los invariantes de cuadre (plan, sección 2.5) después de cada paso:
 * <ul>
 *   <li>pendiente P = S − ΣR;</li>
 *   <li>entrante de la Bodega = S − ΣR (la orden tiene CD y no se liberó nada);</li>
 *   <li>reserva viva en el CD (saliente) = max(S − ΣD, 0);</li>
 *   <li>Σ despachado de los ciclos recibidos = ΣR + Σ discrepancia (pérdida en tránsito).</li>
 * </ul>
 * El recorrido incluye un despacho parcial, una pérdida en tránsito con reapertura y un segundo
 * ciclo que despacha unidades ya perdidas: baja a_la_mano del CD pero no la reserva ya consumida.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import({UsuariosDePrueba.class, AbastecimientoDePrueba.class})
@DisplayName("Ciclo completo de abastecimiento (HD-06)")
class CicloAbastecimientoTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuariosDePrueba usuarios;
    @Autowired private AbastecimientoDePrueba red;
    @Autowired private InventarioUseCase inventario;

    @Test
    @DisplayName("""
            Escenario: venta → traspaso → recomendación → aprobación → orden → despacho → viaje → llegada → recepción
              Dado un CD con 100, un Almacén con 2 (umbral 3/6) y una Bodega con 6 (nivel CD 5/20)
              Cuando una venta deja el Almacén bajo su umbral, el traspaso vacía la Bodega y nace una recomendación de 19
              Y se aprueba: la orden reserva 19 en el CD y suma 19 de entrante en la Bodega
              Y una merma deja el CD en 15: el ciclo 1 despacha 15 (PARCIAL) y quedan 4 de reserva
              Y llegan 13 de 15: 2 de pérdida en tránsito y la orden queda REABIERTA con 6 pendientes
              Y con el CD repuesto, el ciclo 2 despacha 6, libera los 4 de reserva y llegan los 6
              Entonces la orden queda CERRADA, la Bodega en 20 (el objetivo) sin entrante y el CD sin reserva
              Y en cada paso se cumplen los invariantes de cuadre""")
    void cicloCompletoConParcialYReapertura() throws Exception {
        // ---- Red: CD, tienda con Almacén y Bodega, variante con stock y niveles ----
        UUID varianteId = red.crearVariante();
        String sku = jdbc.queryForObject("SELECT sku FROM variante WHERE id = ?", String.class, varianteId);
        UUID cdId = red.crearCd(varianteId, 100);
        UUID tiendaId = red.crearTienda(cdId);
        UUID nodoCd = nodo("SELECT id FROM nodo WHERE cd_id = ?", cdId);
        UUID almacen = nodo("SELECT id FROM nodo WHERE tienda_id = ? AND tipo = 'ALMACEN'", tiendaId);
        UUID bodega = nodo("SELECT id FROM nodo WHERE tienda_id = ? AND tipo = 'BODEGA_TIENDA'", tiendaId);
        inventario.cargar(almacen, varianteId, 2, null);
        inventario.cargar(bodega, varianteId, 6, null);

        UsuarioDePrueba admin = usuarios.conRol("ADMIN");
        UsuarioDePrueba vendedor = usuarios.conRol("VENDEDOR", tiendaId);
        UsuarioDePrueba planificador = usuarios.conRol("PLANIFICADOR_ABASTECIMIENTO", tiendaId);
        UsuarioDePrueba operadorCd = usuarios.conRol("OPERADOR_CD");
        UsuarioDePrueba coordinador = usuarios.conRol("COORDINADOR_TRANSPORTE");
        UsuarioDePrueba encargado = usuarios.conRol("ENCARGADO_BODEGA", tiendaId);

        // Los niveles se crean después de la carga: crearlos no evalúa nada (D-57).
        enviar(post("/api/v1/umbrales-reposicion"), admin,
                "{\"varianteId\": \"%s\", \"cantidadMinima\": 3, \"cantidadObjetivo\": 6}".formatted(varianteId))
                .andExpect(status().isCreated());
        enviar(post("/api/v1/niveles-reposicion-cd"), admin,
                "{\"varianteId\": \"%s\", \"cantidadMinima\": 5, \"cantidadObjetivo\": 20}".formatted(varianteId))
                .andExpect(status().isCreated());

        // ---- 1. Venta: el traspaso repone el Almacén desde la Bodega y nace la recomendación (HU-43) ----
        enviar(post("/api/v1/ventas"), vendedor, """
                {"transaccionId": "%s", "tiendaId": "%s", "forzarSinStock": false,
                 "items": [{"sku": "%s", "cantidad": 1, "precioUnitario": 49.90}]}"""
                .formatted(UUID.randomUUID(), tiendaId, sku))
                .andExpect(status().isCreated());
        assertThat(aLaMano(almacen, varianteId)).isEqualTo(6);
        assertThat(aLaMano(bodega, varianteId)).isEqualTo(1);

        String recomendaciones = leer(mockMvc.perform(get("/api/v1/recomendaciones")
                .param("tiendaId", tiendaId.toString()).param("estado", "BORRADOR")
                .header(HttpHeaders.AUTHORIZATION, planificador.bearer())).andExpect(status().isOk()));
        assertThat((Integer) JsonPath.read(recomendaciones, "$.totalElements")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(recomendaciones, "$.content[0].cantidadSugerida")).isEqualTo(19);
        String recomendacionId = JsonPath.read(recomendaciones, "$.content[0].id");

        // ---- 2. Aprobación: crea la orden con CD y reserva (HU-44, HU-45) ----
        String aprobacion = leer(mockMvc.perform(post("/api/v1/recomendaciones/{id}/aprobar", recomendacionId)
                .header(HttpHeaders.AUTHORIZATION, planificador.bearer())).andExpect(status().isCreated()));
        UUID ordenId = UUID.fromString(JsonPath.read(aprobacion, "$.orden.id"));
        int s = 19;
        assertThat(red.estadoOrden(ordenId)).isEqualTo("APROBADA");
        cuadre(ordenId, s, nodoCd, bodega, varianteId);

        // ---- 3. Merma en el CD y despacho parcial (HU-46) ----
        enviar(post("/api/v1/nodos/{id}/inventario/ajustes", nodoCd), admin,
                "{\"sku\": \"%s\", \"delta\": -85, \"motivo\": \"MERMA\"}".formatted(sku))
                .andExpect(status().isCreated());
        String ciclo1 = leer(mockMvc.perform(post("/api/v1/ordenes-reabastecimiento/{id}/despachos", ordenId)
                        .header(HttpHeaders.AUTHORIZATION, operadorCd.bearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadDespachada").value(15))
                .andExpect(jsonPath("$.resultado").value("PARCIAL")));
        assertThat(aLaMano(nodoCd, varianteId)).isZero();
        cuadre(ordenId, s, nodoCd, bodega, varianteId);

        // ---- 4. Viaje, salida y llegada (HU-47, HU-48) ----
        String parada1 = viajeEntregado(cdId, JsonPath.read(ciclo1, "$.id"), coordinador);
        assertThat(red.estadoOrden(ordenId)).isEqualTo("EN_TRANSITO");
        cuadre(ordenId, s, nodoCd, bodega, varianteId);

        // ---- 5. Recepción con pérdida en tránsito: la orden se reabre (HU-49) ----
        recibir(parada1, 13, encargado)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discrepancia").value(2))
                .andExpect(jsonPath("$.estadoOrden").value("REABIERTA"))
                .andExpect(jsonPath("$.cantidadPendienteOrden").value(6));
        assertThat(aLaMano(bodega, varianteId)).isEqualTo(1 + 13);
        cuadre(ordenId, s, nodoCd, bodega, varianteId);

        // ---- 6. CD repuesto y segundo ciclo: sale lo pendiente, se libera solo la reserva que quedaba ----
        enviar(post("/api/v1/nodos/{id}/inventario/ajustes", nodoCd), admin,
                "{\"sku\": \"%s\", \"delta\": 30, \"motivo\": \"INGRESO_MANUAL\"}".formatted(sku))
                .andExpect(status().isCreated());
        String ciclo2 = leer(mockMvc.perform(post("/api/v1/ordenes-reabastecimiento/{id}/despachos", ordenId)
                        .header(HttpHeaders.AUTHORIZATION, operadorCd.bearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCiclo").value(2))
                .andExpect(jsonPath("$.cantidadSolicitada").value(6))
                .andExpect(jsonPath("$.resultado").value("COMPLETO")));
        assertThat(aLaMano(nodoCd, varianteId)).isEqualTo(30 - 6);
        cuadre(ordenId, s, nodoCd, bodega, varianteId);

        String parada2 = viajeEntregado(cdId, JsonPath.read(ciclo2, "$.id"), coordinador);
        recibir(parada2, 6, encargado)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoOrden").value("CERRADA"))
                .andExpect(jsonPath("$.cantidadPendienteOrden").value(0));

        // ---- Cierre: todo cuadra ----
        cuadre(ordenId, s, nodoCd, bodega, varianteId);
        assertThat(red.estadoOrden(ordenId)).isEqualTo("CERRADA");
        assertThat(aLaMano(bodega, varianteId)).isEqualTo(20);
        assertThat(entero("SELECT entrante FROM inventario WHERE nodo_id = ? AND variante_id = ?", bodega, varianteId))
                .isZero();
        assertThat(entero("SELECT count(*) FROM movimiento_inventario WHERE nodo_id = ? AND tipo = 'DESPACHO'", nodoCd))
                .isEqualTo(2);
        assertThat(entero("SELECT count(*) FROM movimiento_inventario WHERE nodo_id = ? AND tipo = 'RECEPCION'", bodega))
                .isEqualTo(2);
    }

    /** Invariantes de la sección 2.5 para una orden de {@code s} unidades con un único CD. */
    private void cuadre(UUID ordenId, int s, UUID nodoCd, UUID bodega, UUID varianteId) {
        int despachado = entero("SELECT COALESCE(SUM(cantidad_despachada), 0) FROM ciclo_despacho WHERE orden_id = ?",
                ordenId);
        int recibido = entero("""
                SELECT COALESCE(SUM(r.cantidad_recibida), 0) FROM recepcion r
                JOIN ciclo_despacho c ON c.id = r.ciclo_despacho_id WHERE c.orden_id = ?""", ordenId);
        int discrepancia = entero("""
                SELECT COALESCE(SUM(r.discrepancia), 0) FROM recepcion r
                JOIN ciclo_despacho c ON c.id = r.ciclo_despacho_id WHERE c.orden_id = ?""", ordenId);
        int despachadoRecibido = entero("""
                SELECT COALESCE(SUM(c.cantidad_despachada), 0) FROM ciclo_despacho c
                WHERE c.orden_id = ? AND EXISTS (SELECT 1 FROM recepcion r WHERE r.ciclo_despacho_id = c.id)""",
                ordenId);

        assertThat(entero("SELECT cantidad_pendiente FROM orden_reabastecimiento WHERE id = ?", ordenId))
                .as("pendiente = S − ΣR").isEqualTo(s - recibido);
        assertThat(entero("SELECT entrante FROM inventario WHERE nodo_id = ? AND variante_id = ?", bodega, varianteId))
                .as("entrante de la Bodega = S − ΣR").isEqualTo(s - recibido);
        assertThat(entero("SELECT saliente FROM inventario WHERE nodo_id = ? AND variante_id = ?", nodoCd, varianteId))
                .as("reserva viva en el CD = max(S − ΣD, 0)").isEqualTo(Math.max(s - despachado, 0));
        assertThat(despachadoRecibido).as("despachado = recibido + pérdida en tránsito")
                .isEqualTo(recibido + discrepancia);
    }

    /** Arma un viaje con el ciclo, lo hace salir y llegar; devuelve la ruta de recepción de su parada. */
    private String viajeEntregado(UUID cdId, String cicloId, UsuarioDePrueba coordinador) throws Exception {
        String viaje = leer(enviar(post("/api/v1/viajes"), coordinador, """
                {"cdId": "%s", "vehiculo": "Camión ABC-123", "paradas": [{"cicloDespachoId": "%s"}]}"""
                .formatted(cdId, cicloId)).andExpect(status().isCreated()));
        String viajeId = JsonPath.read(viaje, "$.id");
        String paradaId = JsonPath.read(viaje, "$.paradas[0].id");
        mockMvc.perform(post("/api/v1/viajes/{id}/salida", viajeId).header(HttpHeaders.AUTHORIZATION, coordinador.bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/viajes/{id}/paradas/{p}/llegada", viajeId, paradaId)
                        .header(HttpHeaders.AUTHORIZATION, coordinador.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"));
        return "/api/v1/viajes/" + viajeId + "/paradas/" + paradaId + "/recepcion";
    }

    private ResultActions recibir(String ruta, int cantidad, UsuarioDePrueba encargado) throws Exception {
        return enviar(post(ruta), encargado, "{\"cantidadRecibida\": " + cantidad + "}");
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, UsuarioDePrueba usuario, String cuerpo)
            throws Exception {
        return mockMvc.perform(peticion.header(HttpHeaders.AUTHORIZATION, usuario.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private static String leer(ResultActions respuesta) throws Exception {
        return respuesta.andReturn().getResponse().getContentAsString();
    }

    private int aLaMano(UUID nodo, UUID varianteId) {
        return entero("SELECT a_la_mano FROM inventario WHERE nodo_id = ? AND variante_id = ?", nodo, varianteId);
    }

    private UUID nodo(String sql, UUID id) {
        return jdbc.queryForObject(sql, UUID.class, id);
    }

    private int entero(String sql, Object... argumentos) {
        return jdbc.queryForObject(sql, Integer.class, argumentos);
    }
}
