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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de aceptación de HU-37 (carga inicial) de extremo a extremo,
 * contra PostgreSQL real: endpoint, autorización por tipo de nodo (D-31),
 * validación del lote (D-30) y solo filas nuevas (D-28).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(UsuariosDePrueba.class)
class CargaInicialControllerTest {

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
            Escenario: carga inicial de un nodo vacío
              Dado un Almacén sin filas de inventario
              Cuando se carga un lote de 3 SKU válidos
              Entonces la respuesta es 201
              Y existen 3 filas de inventario en el nodo
              Y existen 3 movimientos CARGA_INICIAL con el usuario del token""")
    void cargaInicialDeUnNodoVacio() throws Exception {
        String a = crearVariante();
        String b = crearVariante();
        String c = crearVariante();

        cargar(almacenId, supervisor, Map.of(a, 5, b, 3, c, 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nodoId").value(almacenId.toString()))
                .andExpect(jsonPath("$.filasCreadas").value(3))
                .andExpect(jsonPath("$.unidadesTotales").value(10));

        assertThat(contarFilas(almacenId)).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM movimiento_inventario
                 WHERE nodo_id = ? AND tipo = 'CARGA_INICIAL' AND usuario_id = ?""",
                Integer.class, almacenId, supervisor.id())).isEqualTo(3);
    }

    @Test
    @DisplayName("""
            Escenario: colisión rechaza el lote completo
              Dado un Almacén con la variante X ya cargada
              Cuando se carga un lote con X y con Y
              Entonces la respuesta es 409 listando X
              Y no se crea la fila de Y""")
    void colisionRechazaElLoteCompleto() throws Exception {
        String x = crearVariante();
        String y = crearVariante();
        cargar(almacenId, supervisor, Map.of(x, 4)).andExpect(status().isCreated());

        cargar(almacenId, supervisor, Map.of(x, 1, y, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString(x)))
                .andExpect(jsonPath("$.mensaje").value(not(containsString(y))));

        assertThat(contarFilas(almacenId)).isEqualTo(1);
        assertThat(aLaMano(almacenId, x)).isEqualTo(4);
    }

    @Test
    @DisplayName("""
            Escenario: nueva colección en un nodo ya operativo
              Dado un Almacén con variantes cargadas
              Cuando se carga un lote solo con variantes nuevas
              Entonces la respuesta es 201
              Y las variantes anteriores no cambian""")
    void nuevaColeccionEnUnNodoYaOperativo() throws Exception {
        String anterior = crearVariante();
        cargar(almacenId, supervisor, Map.of(anterior, 7)).andExpect(status().isCreated());

        cargar(almacenId, supervisor, Map.of(crearVariante(), 2, crearVariante(), 3))
                .andExpect(status().isCreated());

        assertThat(contarFilas(almacenId)).isEqualTo(3);
        assertThat(aLaMano(almacenId, anterior)).isEqualTo(7);
    }

    @Test
    @DisplayName("""
            Escenario: SKU inexistente rechaza el lote
              Cuando se carga un lote con un SKU que no existe en catálogo
              Entonces la respuesta es 400 indicando el SKU
              Y no se crea ninguna fila""")
    void skuInexistenteRechazaElLote() throws Exception {
        cargar(almacenId, supervisor, Map.of(crearVariante(), 1, "SKU-QUE-NO-EXISTE", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("SKU-QUE-NO-EXISTE")));

        assertThat(contarFilas(almacenId)).isZero();
    }

    @Test
    @DisplayName("""
            Escenario: SKU repetido rechaza el lote
              Cuando se carga un lote con el mismo SKU dos veces
              Entonces la respuesta es 400 indicando el SKU
              Y no se crea ninguna fila""")
    void skuRepetidoRechazaElLote() throws Exception {
        String x = crearVariante();

        enviar(almacenId, supervisor, """
                {"items": [{"sku": "%s", "cantidad": 1}, {"sku": "%s", "cantidad": 2}]}
                """.formatted(x, x))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString(x)));

        assertThat(contarFilas(almacenId)).isZero();
    }

    @Test
    @DisplayName("""
            Escenario: un SKU cargado con 0 cuenta como cargado
              Cuando se carga un lote con un SKU en 0
              Entonces la respuesta es 201
              Y existe la fila con a_la_mano 0""")
    void skuCargadoEnCeroCuentaComoCargado() throws Exception {
        String x = crearVariante();

        cargar(almacenId, supervisor, Map.of(x, 0)).andExpect(status().isCreated());

        assertThat(aLaMano(almacenId, x)).isZero();
        cargar(almacenId, supervisor, Map.of(x, 3)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("""
            Escenario: un CD exige el scope de inventario de CD
              Dado un CD sin filas de inventario
              Cuando un supervisor de tienda carga un lote en el CD
              Entonces la respuesta es 403
              Cuando un operador de CD carga el mismo lote
              Entonces la respuesta es 201""")
    void unCdExigeElScopeDeInventarioCd() throws Exception {
        UUID cdNodoId = crearNodoDeCd();
        String x = crearVariante();

        cargar(cdNodoId, supervisor, Map.of(x, 10)).andExpect(status().isForbidden());
        cargar(cdNodoId, usuarios.conRol("OPERADOR_CD"), Map.of(x, 10)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("""
            Escenario: un usuario de tienda no carga en una tienda ajena
              Dado un supervisor asignado solo a la tienda T1
              Cuando carga un lote en el Almacén de T2
              Entonces la respuesta es 404""")
    void unUsuarioDeTiendaNoCargaEnUnaTiendaAjena() throws Exception {
        UUID almacenAjeno = crearNodoDeTienda(crearTienda());

        cargar(almacenAjeno, supervisor, Map.of(crearVariante(), 1)).andExpect(status().isNotFound());
        assertThat(contarFilas(almacenAjeno)).isZero();
    }

    @Test
    void sinScopeDeInventarioRespondeProhibido() throws Exception {
        cargar(almacenId, usuarios.conRol("VENDEDOR", tiendaId), Map.of(crearVariante(), 1))
                .andExpect(status().isForbidden());
    }

    @Test
    void unNodoInexistenteRespondeNoEncontrado() throws Exception {
        cargar(UUID.randomUUID(), usuarios.conRol("ADMIN"), Map.of(crearVariante(), 1))
                .andExpect(status().isNotFound());
    }

    @Test
    void rechazaLotesQueNoCumplenElFormato() throws Exception {
        enviar(almacenId, supervisor, "{\"items\": []}").andExpect(status().isBadRequest());
        enviar(almacenId, supervisor, """
                {"items": [{"sku": "SKU-1", "cantidad": -1}]}
                """).andExpect(status().isBadRequest());
        enviar(almacenId, supervisor, """
                {"items": [{"sku": " ", "cantidad": 1}]}
                """).andExpect(status().isBadRequest());
        String quinientosUno = IntStream.rangeClosed(0, 500)
                .mapToObj(i -> "{\"sku\": \"SKU-" + i + "\", \"cantidad\": 1}")
                .collect(Collectors.joining(",", "{\"items\": [", "]}"));
        enviar(almacenId, supervisor, quinientosUno).andExpect(status().isBadRequest());

        assertThat(contarFilas(almacenId)).isZero();
    }

    private ResultActions cargar(UUID nodoId, UsuarioDePrueba usuario, Map<String, Integer> items)
            throws Exception {
        String cuerpo = items.entrySet().stream()
                .map(item -> "{\"sku\": \"%s\", \"cantidad\": %d}".formatted(item.getKey(), item.getValue()))
                .collect(Collectors.joining(",", "{\"items\": [", "]}"));
        return enviar(nodoId, usuario, cuerpo);
    }

    private ResultActions enviar(UUID nodoId, UsuarioDePrueba usuario, String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/v1/nodos/{nodoId}/inventario/carga-inicial", nodoId)
                .header(HttpHeaders.AUTHORIZATION, usuario.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private int contarFilas(UUID nodoId) {
        return jdbc.queryForObject("SELECT count(*) FROM inventario WHERE nodo_id = ?", Integer.class, nodoId);
    }

    private int aLaMano(UUID nodoId, String sku) {
        return jdbc.queryForObject("""
                SELECT i.a_la_mano FROM inventario i JOIN variante v ON v.id = i.variante_id
                 WHERE i.nodo_id = ? AND v.sku = ?""", Integer.class, nodoId, sku);
    }

    private UUID crearTienda() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')",
                id, "Tienda Carga " + id);
        return id;
    }

    private UUID crearNodoDeTienda(UUID tienda) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'ALMACEN', ?)", id, tienda);
        return id;
    }

    private UUID crearNodoDeCd() {
        UUID cdId = UUID.randomUUID();
        jdbc.update("INSERT INTO cd (id, nombre) VALUES (?, ?)", cdId, "CD Carga " + cdId);
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
