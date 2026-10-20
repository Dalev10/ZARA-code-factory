package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.ResolverUmbralPort;
import com.codefactory.supplychain.soporte.UsuariosDePrueba;
import com.codefactory.supplychain.soporte.UsuariosDePrueba.UsuarioDePrueba;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de HU-40 a HU-42 (registrar, anular y consultar ventas) y del escenario "tiempo real" del plan, de extremo
 * a extremo contra PostgreSQL real: la venta, el traspaso automático (HU-48) y la ventana
 * de quiebre (HU-45) ya se ven al recibir la respuesta (D-18).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(UsuariosDePrueba.class)
@DisplayName("VentaController (HU-40 a HU-42) y tiempo real")
class VentaControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuariosDePrueba usuarios;
    @Autowired private InventarioUseCase inventario;

    @MockitoSpyBean
    private ResolverUmbralPort resolverUmbral;

    private UsuarioDePrueba admin;
    private UsuarioDePrueba vendedor;
    private UUID tiendaId;
    private UUID almacenId;
    private UUID bodegaId;
    private UUID varianteId;
    private String sku;

    @BeforeEach
    void setUp() throws Exception {
        admin = usuarios.conRol("ADMIN");
        String respuesta = mockMvc.perform(post("/api/v1/tiendas")
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Tienda %s\", \"ubicacion\": \"Medellín\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        tiendaId = UUID.fromString(JsonPath.read(respuesta, "$.id"));
        almacenId = nodo("ALMACEN");
        bodegaId = nodo("BODEGA_TIENDA");
        vendedor = usuarios.conRol("VENDEDOR", tiendaId);
        crearVariante();
    }

    @Test
    @DisplayName("""
            Escenario: una venta deja el Almacén bajo el mínimo y se repone en la misma respuesta
              Dado un Almacén con 3 unidades, una Bodega con 10 y un umbral de mínimo 3 y objetivo 6
              Cuando se registra una venta de 1 unidad
              Entonces la respuesta es 201
              Y un GET inmediato muestra el Almacén en 6 y la Bodega en 6
              Y existe un traspaso interno AUTOMATICO de 4 unidades""")
    void ventaReponeElAlmacenEnLaMismaRespuesta() throws Exception {
        inventario.cargar(almacenId, varianteId, 3, null);
        inventario.cargar(bodegaId, varianteId, 10, null);
        crearUmbral(3, 6);

        vender(vendedor, UUID.randomUUID(), 1, false).andExpect(status().isCreated());

        assertThat(aLaMano(almacenId)).isEqualTo(6);
        assertThat(aLaMano(bodegaId)).isEqualTo(6);
        assertThat(jdbc.queryForList(
                "SELECT cantidad FROM traspaso_interno WHERE tienda_id = ? AND origen = 'AUTOMATICO'",
                Integer.class, tiendaId)).containsExactly(4);
    }

    @Test
    @DisplayName("""
            Escenario: si la reacción falla, la venta queda registrada igual
              Dado que el cálculo del traspaso falla
              Cuando se registra una venta
              Entonces la respuesta es 201 y la venta existe""")
    void siLaReaccionFallaLaVentaQuedaRegistrada() throws Exception {
        inventario.cargar(almacenId, varianteId, 3, null);
        doThrow(new IllegalStateException("falla simulada")).when(resolverUmbral).resolver(any(), any());
        UUID transaccion = UUID.randomUUID();

        vender(vendedor, transaccion, 1, false).andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM venta WHERE transaccion_id = ?",
                Integer.class, transaccion)).isEqualTo(1);
        assertThat(aLaMano(almacenId)).isEqualTo(2);
    }

    @Test
    @DisplayName("Una venta que agota el Almacén abre la ventana de quiebre en la misma respuesta (HU-45)")
    void ventaQueAgotaElAlmacenAbreLaVentana() throws Exception {
        inventario.cargar(almacenId, varianteId, 1, null);

        vender(vendedor, UUID.randomUUID(), 1, false).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/ventanas-quiebre").param("tiendaId", tiendaId.toString())
                        .header(HttpHeaders.AUTHORIZATION, vendedor.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].estado").value("ABIERTA"));
    }

    @Test
    @DisplayName("Al desactivar la tienda, sus ventanas abiertas se cierran con TIENDA_DESACTIVADA (D-50)")
    void desactivarLaTiendaCierraSusVentanas() throws Exception {
        inventario.cargar(almacenId, varianteId, 1, null);
        vender(vendedor, UUID.randomUUID(), 1, false).andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/tiendas/{id}", tiendaId).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForList("SELECT motivo_cierre FROM ventana_quiebre WHERE tienda_id = ?",
                String.class, tiendaId)).containsExactly("TIENDA_DESACTIVADA");
    }

    @Test
    @DisplayName("Sin stock responde 409 y no registra nada")
    void sinStockResponde409() throws Exception {
        inventario.cargar(almacenId, varianteId, 1, null);
        UUID transaccion = UUID.randomUUID();

        vender(vendedor, transaccion, 2, false).andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM venta WHERE transaccion_id = ?",
                Integer.class, transaccion)).isZero();
        assertThat(aLaMano(almacenId)).isEqualTo(1);
    }

    @Test
    @DisplayName("Una venta forzada de una variante sin fila deja el Almacén en negativo (D-25, D-35)")
    void ventaForzadaSinFilaQuedaEnNegativo() throws Exception {
        vender(vendedor, UUID.randomUUID(), 1, true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lineas[0].forzada").value(true));

        assertThat(aLaMano(almacenId)).isEqualTo(-1);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM movimiento_inventario m JOIN venta v ON v.id = m.referencia_id WHERE m.nodo_id = ?",
                Integer.class, almacenId)).isEqualTo(1);
    }

    @Test
    @DisplayName("Un reintento con el mismo transaccion_id responde 200 con la venta original (D-69)")
    void reintentoRespondeLaVentaOriginal() throws Exception {
        inventario.cargar(almacenId, varianteId, 5, null);
        UUID transaccion = UUID.randomUUID();

        vender(vendedor, transaccion, 1, false).andExpect(status().isCreated());
        vender(vendedor, transaccion, 1, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaccionId").value(transaccion.toString()));

        assertThat(aLaMano(almacenId)).isEqualTo(4);
    }

    @Test
    @DisplayName("Un vendedor sin la tienda asignada recibe 403")
    void tiendaNoAsignadaResponde403() throws Exception {
        inventario.cargar(almacenId, varianteId, 5, null);
        UsuarioDePrueba otro = usuarios.conRol("VENDEDOR");

        vender(otro, UUID.randomUUID(), 1, false).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("""
            Escenario: ventas simultáneas con reposición no pierden unidades ni agotan conexiones
              Dado un Almacén con 10 unidades, una Bodega con 20 y un umbral de mínimo 3 y objetivo 6
              Cuando se registran 8 ventas de 1 unidad al mismo tiempo
              Entonces cada una responde 201 o, si agotó los reintentos optimistas, 409 (D-24)
              Y Almacén + Bodega bajan exactamente en las unidades vendidas (HU-36)""")
    void ventasSimultaneasConReposicion() throws Exception {
        inventario.cargar(almacenId, varianteId, 10, null);
        inventario.cargar(bodegaId, varianteId, 20, null);
        crearUmbral(3, 6);
        ExecutorService hilos = Executors.newFixedThreadPool(8);
        List<Callable<String>> ventas = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            ventas.add(() -> {
                var respuesta = vender(vendedor, UUID.randomUUID(), 1, false).andReturn().getResponse();
                return respuesta.getStatus() == 201 ? "201" : respuesta.getStatus() + " " + respuesta.getContentAsString();
            });
        }

        List<String> estados = new ArrayList<>();
        for (Future<String> resultado : hilos.invokeAll(ventas, 60, TimeUnit.SECONDS)) {
            estados.add(resultado.get());
        }
        hilos.shutdown();

        String conflicto = "409 {\"mensaje\":\"Otra operación modificó el mismo registro; intente de nuevo\"}";
        assertThat(estados).allMatch(estado -> estado.equals("201") || estado.equals(conflicto));
        long vendidas = estados.stream().filter("201"::equals).count();
        assertThat(vendidas).isPositive();
        assertThat(aLaMano(almacenId) + aLaMano(bodegaId)).isEqualTo(30 - vendidas);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM venta WHERE tienda_id = ?", Long.class, tiendaId))
                .isEqualTo(vendidas);
    }

    @Test
    @DisplayName("""
            Escenario: anular una línea devuelve el stock y no se anula dos veces (HU-41)
              Dado un Almacén con 5 unidades y una venta de 2
              Cuando el supervisor anula la venta con un motivo
              Entonces la venta queda ANULADA con su motivo y el Almacén vuelve a 5
              Y una segunda anulación responde la venta ya anulada sin devolver stock otra vez""")
    void anularUnaLineaDevuelveElStockUnaSolaVez() throws Exception {
        inventario.cargar(almacenId, varianteId, 5, null);
        String ventaId = idDeLinea(vender(vendedor, UUID.randomUUID(), 2, false));
        UsuarioDePrueba supervisor = usuarios.conRol("SUPERVISOR_TIENDA", tiendaId);

        anular("/api/v1/ventas/" + ventaId + "/anular", supervisor, "Error de digitación")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas[0].estado").value("ANULADA"))
                .andExpect(jsonPath("$.lineas[0].motivoAnulacion").value("Error de digitación"))
                .andExpect(jsonPath("$.lineas[0].anuladaPor").value(supervisor.id().toString()));
        assertThat(aLaMano(almacenId)).isEqualTo(5);

        anular("/api/v1/ventas/" + ventaId + "/anular", supervisor, "Otro intento")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas[0].motivoAnulacion").value("Error de digitación"));
        assertThat(aLaMano(almacenId)).isEqualTo(5);
    }

    @Test
    @DisplayName("""
            Escenario: anular una transacción completa es todo o nada (HU-41, D-68)
              Dada una venta de 2 SKU en la misma transacción (sin forzarSinStock, que es opcional)
              Cuando el supervisor anula la transacción
              Entonces ambas líneas quedan ANULADAS y el stock de las dos vuelve""")
    void anularUnaTransaccionAnulaTodasSusLineas() throws Exception {
        String otroSku = crearOtraVariante();
        UUID otraVariante = jdbc.queryForObject("SELECT id FROM variante WHERE sku = ?", UUID.class, otroSku);
        inventario.cargar(almacenId, varianteId, 5, null);
        inventario.cargar(almacenId, otraVariante, 5, null);
        UUID transaccion = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/ventas")
                        .header(HttpHeaders.AUTHORIZATION, vendedor.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transaccionId": "%s", "tiendaId": "%s", "items": [
                                  {"sku": "%s", "cantidad": 1, "precioUnitario": 10.00},
                                  {"sku": "%s", "cantidad": 3, "precioUnitario": 20.00}]}"""
                                .formatted(transaccion, tiendaId, sku, otroSku)))
                .andExpect(status().isCreated());

        anular("/api/v1/ventas/transacciones/" + transaccion + "/anular",
                usuarios.conRol("SUPERVISOR_TIENDA", tiendaId), "Cliente desistió")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas.length()").value(2))
                .andExpect(jsonPath("$.lineas[0].estado").value("ANULADA"))
                .andExpect(jsonPath("$.lineas[1].estado").value("ANULADA"));
        assertThat(aLaMano(almacenId)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT a_la_mano FROM inventario WHERE nodo_id = ? AND variante_id = ?",
                Integer.class, almacenId, otraVariante)).isEqualTo(5);
    }

    @Test
    @DisplayName("Anular exige motivo (400), el scope ventas:anular (403) y una tienda asignada (404)")
    void reglasDeLaAnulacion() throws Exception {
        inventario.cargar(almacenId, varianteId, 5, null);
        String ventaId = idDeLinea(vender(vendedor, UUID.randomUUID(), 1, false));
        String ruta = "/api/v1/ventas/" + ventaId + "/anular";

        anular(ruta, usuarios.conRol("SUPERVISOR_TIENDA", tiendaId), " ").andExpect(status().isBadRequest());
        anular(ruta, vendedor, "Sin permiso").andExpect(status().isForbidden());
        anular(ruta, usuarios.conRol("SUPERVISOR_TIENDA", otraTienda()), "Tienda ajena")
                .andExpect(status().isNotFound());
        assertThat(aLaMano(almacenId)).isEqualTo(4);
    }

    @Test
    @DisplayName("""
            Escenario: la consulta de ventas respeta el alcance por tienda (HU-42, D-45)
              Dada una venta registrada en la tienda T
              Cuando el vendedor de T la consulta por id y en el listado, la ve
              Y un vendedor de otra tienda recibe 404 en el detalle y un listado vacío al filtrar por T""")
    void laConsultaDeVentasRespetaElAlcance() throws Exception {
        inventario.cargar(almacenId, varianteId, 5, null);
        String ventaId = idDeLinea(vender(vendedor, UUID.randomUUID(), 1, false));
        UsuarioDePrueba ajeno = usuarios.conRol("VENDEDOR", otraTienda());

        mockMvc.perform(get("/api/v1/ventas/{id}", ventaId).header(HttpHeaders.AUTHORIZATION, vendedor.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas[0].id").value(ventaId));
        mockMvc.perform(get("/api/v1/ventas").param("tiendaId", tiendaId.toString())
                        .header(HttpHeaders.AUTHORIZATION, vendedor.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
        mockMvc.perform(get("/api/v1/ventas/{id}", ventaId).header(HttpHeaders.AUTHORIZATION, ajeno.bearer()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/ventas").param("tiendaId", tiendaId.toString())
                        .header(HttpHeaders.AUTHORIZATION, ajeno.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("Un cuerpo ilegible o sin cantidad responde 400 con un mensaje, no vacío")
    void cuerpoInvalidoRespondeConMensaje() throws Exception {
        mockMvc.perform(post("/api/v1/ventas").header(HttpHeaders.AUTHORIZATION, vendedor.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"transaccionId\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
        mockMvc.perform(post("/api/v1/ventas").header(HttpHeaders.AUTHORIZATION, vendedor.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transaccionId": "%s", "tiendaId": "%s",
                                 "items": [{"sku": "%s", "precioUnitario": 10.00}]}"""
                                .formatted(UUID.randomUUID(), tiendaId, sku)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("cantidad")));
    }

    private ResultActions anular(String ruta, UsuarioDePrueba usuario, String motivo) throws Exception {
        return mockMvc.perform(post(ruta)
                .header(HttpHeaders.AUTHORIZATION, usuario.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"%s\"}".formatted(motivo)));
    }

    private static String idDeLinea(ResultActions venta) throws Exception {
        String json = venta.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.lineas[0].id");
    }

    private UUID otraTienda() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Cali')", id, "Tienda " + id);
        return id;
    }

    private String crearOtraVariante() {
        UUID categoriaId = UUID.randomUUID();
        jdbc.update("INSERT INTO categoria (id, nombre) VALUES (?, ?)", categoriaId, "Categoría " + categoriaId);
        UUID templateId = UUID.randomUUID();
        jdbc.update("INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)",
                templateId, categoriaId, "Template " + templateId);
        String otroSku = "SKU-" + UUID.randomUUID();
        jdbc.update("INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)", UUID.randomUUID(), templateId, otroSku);
        return otroSku;
    }

    private ResultActions vender(UsuarioDePrueba usuario, UUID transaccion, int cantidad, boolean forzar)
            throws Exception {
        return mockMvc.perform(post("/api/v1/ventas")
                .header(HttpHeaders.AUTHORIZATION, usuario.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"transaccionId": "%s", "tiendaId": "%s", "forzarSinStock": %s,
                         "items": [{"sku": "%s", "cantidad": %d, "precioUnitario": 49.90}]}"""
                        .formatted(transaccion, tiendaId, forzar, sku, cantidad)));
    }

    private void crearUmbral(int minimo, int objetivo) throws Exception {
        mockMvc.perform(post("/api/v1/umbrales-reposicion")
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"varianteId\": \"%s\", \"cantidadMinima\": %d, \"cantidadObjetivo\": %d}"
                                .formatted(varianteId, minimo, objetivo)))
                .andExpect(status().isCreated());
    }

    private int aLaMano(UUID nodoId) {
        return jdbc.queryForObject("SELECT a_la_mano FROM inventario WHERE nodo_id = ? AND variante_id = ?",
                Integer.class, nodoId, varianteId);
    }

    private UUID nodo(String tipo) {
        return jdbc.queryForObject("SELECT id FROM nodo WHERE tienda_id = ? AND tipo = ?", UUID.class, tiendaId, tipo);
    }

    private void crearVariante() {
        UUID categoriaId = UUID.randomUUID();
        jdbc.update("INSERT INTO categoria (id, nombre) VALUES (?, ?)", categoriaId, "Categoría " + categoriaId);
        UUID templateId = UUID.randomUUID();
        jdbc.update("INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)",
                templateId, categoriaId, "Template " + templateId);
        varianteId = UUID.randomUUID();
        sku = "SKU-" + varianteId;
        jdbc.update("INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)", varianteId, templateId, sku);
    }
}
