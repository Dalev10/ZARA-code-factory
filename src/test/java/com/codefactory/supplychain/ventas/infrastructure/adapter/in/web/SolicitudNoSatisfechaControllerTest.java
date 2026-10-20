package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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
 * Criterios de HU-43 (registrar solicitud no satisfecha) y HU-44 (consultarla y anularla)
 * de extremo a extremo, contra PostgreSQL real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(UsuariosDePrueba.class)
@DisplayName("SolicitudNoSatisfechaController (HU-43, HU-44)")
class SolicitudNoSatisfechaControllerTest {

    private static final String RUTA = "/api/v1/solicitudes-no-satisfechas";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuariosDePrueba usuarios;
    @Autowired private InventarioUseCase inventario;

    private UUID tiendaId;
    private UUID almacenId;
    private UUID varianteId;
    private UsuarioDePrueba vendedor;

    @BeforeEach
    void setUp() {
        tiendaId = crearTienda();
        almacenId = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'ALMACEN', ?)", almacenId, tiendaId);
        varianteId = crearVariante();
        vendedor = usuarios.conRol("VENDEDOR", tiendaId);
    }

    @Test
    @DisplayName("""
            Escenario: la solicitud guarda el stock del Almacén y su autor (HU-43, D-40)
              Dado un Almacén con 2 unidades de la variante X
              Cuando el vendedor registra una solicitud de 3 unidades de X
              Entonces la respuesta es 201 con stock_sistema 2 y el vendedor como registrador""")
    void guardaElStockDelAlmacenYSuAutor() throws Exception {
        inventario.cargar(almacenId, varianteId, 2, null);

        registrar(vendedor, "clave-" + UUID.randomUUID(), 3)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockSistema").value(2))
                .andExpect(jsonPath("$.cantidad").value(3))
                .andExpect(jsonPath("$.registradoPor").value(vendedor.id().toString()))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    @DisplayName("""
            Escenario: un reintento no duplica la demanda (HU-43, D-43)
              Dada una solicitud registrada con la clave K
              Cuando se repite con la misma clave y los mismos datos, la respuesta es 200 con la original
              Y si se repite con otros datos, la respuesta es 409""")
    void unReintentoNoDuplica() throws Exception {
        String clave = "clave-" + UUID.randomUUID();
        String id = id(registrar(vendedor, clave, 1).andExpect(status().isCreated()));

        registrar(vendedor, clave, 1).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        registrar(vendedor, clave, 5).andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM solicitud_no_satisfecha WHERE clave_idempotencia = ?",
                Integer.class, clave)).isEqualTo(1);
    }

    @Test
    @DisplayName("Una variante fuera de catálogo responde 404 y una tienda no asignada, 403 (D-42)")
    void varianteYTiendaSeValidan() throws Exception {
        mockMvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, vendedor.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(tiendaId, UUID.randomUUID(), "clave-" + UUID.randomUUID(), 1)))
                .andExpect(status().isNotFound());
        registrar(usuarios.conRol("VENDEDOR", crearTienda()), "clave-" + UUID.randomUUID(), 1)
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("""
            Escenario: consultar y anular siguen las reglas de ventas (HU-44, D-44, D-45)
              Dada una solicitud de la tienda T
              Entonces su vendedor la ve en el listado y por id; un vendedor de otra tienda recibe 404
              Y el supervisor la anula dejando su auditoría; anularla otra vez responde 409""")
    void consultarYAnular() throws Exception {
        String id = id(registrar(vendedor, "clave-" + UUID.randomUUID(), 1).andExpect(status().isCreated()));
        UsuarioDePrueba ajeno = usuarios.conRol("VENDEDOR", crearTienda());
        UsuarioDePrueba supervisor = usuarios.conRol("SUPERVISOR_TIENDA", tiendaId);

        mockMvc.perform(get(RUTA).param("tiendaId", tiendaId.toString())
                        .header(HttpHeaders.AUTHORIZATION, vendedor.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
        mockMvc.perform(get(RUTA + "/{id}", id).header(HttpHeaders.AUTHORIZATION, ajeno.bearer()))
                .andExpect(status().isNotFound());

        anular(vendedor, id).andExpect(status().isForbidden());
        anular(supervisor, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"))
                .andExpect(jsonPath("$.anuladaPor").value(supervisor.id().toString()));
        anular(supervisor, id).andExpect(status().isConflict());
    }

    private ResultActions registrar(UsuarioDePrueba usuario, String clave, int cantidad) throws Exception {
        return mockMvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, usuario.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(tiendaId, varianteId, clave, cantidad)));
    }

    private ResultActions anular(UsuarioDePrueba usuario, String id) throws Exception {
        return mockMvc.perform(post(RUTA + "/{id}/anular", id).header(HttpHeaders.AUTHORIZATION, usuario.bearer()));
    }

    private static String cuerpo(UUID tienda, UUID variante, String clave, int cantidad) {
        return "{\"tiendaId\": \"%s\", \"varianteId\": \"%s\", \"claveIdempotencia\": \"%s\", \"cantidad\": %d}"
                .formatted(tienda, variante, clave, cantidad);
    }

    private static String id(ResultActions respuesta) throws Exception {
        return JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private UUID crearTienda() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')", id, "Tienda " + id);
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
