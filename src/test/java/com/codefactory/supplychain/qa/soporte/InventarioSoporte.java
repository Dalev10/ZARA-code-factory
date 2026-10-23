package com.codefactory.supplychain.qa.soporte;

import com.codefactory.supplychain.qa.config.ClienteApi;
import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Datos de apoyo para los escenarios de inventario (tiendas, bodegas, centros de distribución,
 * variantes, usuarios con rol y operaciones de stock). Todos los ids se guardan en
 * {@link ContextoPrueba} usando el alias del recurso.
 */
@Component
@ScenarioScope
public class InventarioSoporte {

    public static final String PASSWORD = "ClaveSegura-QA-2026!";

    private final ClienteApi api;
    private final ContextoPrueba contexto;

    public InventarioSoporte(ClienteApi api, ContextoPrueba contexto) {
        this.api = api;
        this.contexto = contexto;
    }

    public record Tienda(String id, String nombre, String almacenId, String bodegaId) {
    }

    public record Centro(String id, String nombre, String nodoId) {
    }

    public record Variante(String id, String sku) {
    }

    public record Usuario(String id, String email, String token) {
    }

    public Tienda crearTienda(String alias, String ubicacion) {
        String nombre = ClienteApi.unico(alias);
        Response respuesta = api.post("/tiendas", Map.of("nombre", nombre, "ubicacion", ubicacion));
        exigirCreado("tienda", alias, respuesta);
        Tienda tienda = new Tienda(respuesta.path("id"), respuesta.path("nombre"),
                respuesta.path("almacenId"), respuesta.path("bodegaId"));
        guardarTienda(alias, tienda);
        contexto.id("ultimaTiendaId", tienda.id());
        contexto.texto("ultimaTiendaNombre", tienda.nombre());
        contexto.id("ultimaTiendaAlmacen", tienda.almacenId());
        contexto.id("ultimaTiendaBodega", tienda.bodegaId());
        return tienda;
    }

    public Centro crearCentro(String alias, String ubicacion) {
        String nombre = ClienteApi.unico(alias);
        Response respuesta = api.post("/centros-distribucion", Map.of("nombre", nombre, "ubicacion", ubicacion));
        exigirCreado("centro de distribución", alias, respuesta);
        String cdId = respuesta.path("id");
        String nodoId = api.get("/centros-distribucion/" + cdId).path("nodo.id");
        contexto.setRespuesta(respuesta);
        Centro centro = new Centro(cdId, respuesta.path("nombre"), nodoId);
        contexto.id("cd:" + alias, centro.id());
        contexto.texto("cd:" + alias + ":nombre", centro.nombre());
        contexto.id("cd:" + alias + ":nodo", centro.nodoId());
        contexto.id("ultimoCdId", centro.id());
        contexto.texto("ultimoCdNombre", centro.nombre());
        contexto.id("ultimoCdNodo", centro.nodoId());
        return centro;
    }

    public Variante crearVariante(String alias) {
        Response categoria = api.post("/categorias", Map.of("nombre", ClienteApi.unico("Categoría " + alias)));
        exigirCreado("categoría", alias, categoria);
        String categoriaId = categoria.path("id");

        Response template = api.post("/templates", Map.of(
                "nombre", ClienteApi.unico("Template " + alias),
                "temporada", "OI-2026",
                "proveedor", "Proveedor QA",
                "precioBase", 89900,
                "categoriaId", categoriaId));
        exigirCreado("template", alias, template);

        String sku = ClienteApi.unico("SKU-" + alias).toUpperCase();
        Response variante = api.post("/variantes", Map.of(
                "sku", sku,
                "talla", "M",
                "color", "Negro",
                "templateId", template.path("id")));
        exigirCreado("variante", alias, variante);
        Variante creada = new Variante(variante.path("id"), sku);
        contexto.id("variante:" + alias, creada.id());
        contexto.texto("variante:" + alias + ":sku", creada.sku());
        return creada;
    }

    public Response cargaInicial(String token, String nodoId, List<Map<String, Object>> items) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("items", items);
        return api.postConToken(token, "/nodos/" + nodoId + "/inventario/carga-inicial", cuerpo);
    }

    public Response cargar(String token, String nodoId, String sku, int cantidad) {
        Response respuesta = cargaInicial(token, nodoId, List.of(item(sku, cantidad)));
        if (respuesta.statusCode() != 201) {
            throw new IllegalStateException("No se pudo cargar inventario inicial: "
                    + respuesta.statusCode() + " " + respuesta.asString());
        }
        return respuesta;
    }

    public Response ajustar(String token, String nodoId, String sku, int delta, String motivo, String comentario) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("sku", sku);
        cuerpo.put("delta", delta);
        cuerpo.put("motivo", motivo);
        cuerpo.put("comentario", comentario);
        return api.postConToken(token, "/nodos/" + nodoId + "/inventario/ajustes", cuerpo);
    }

    public int stock(String token, String nodoId, String varianteId) {
        List<Map<String, Object>> filas = api.getConToken(token, "/variantes/" + varianteId + "/inventario")
                .then().statusCode(200)
                .extract().path("findAll { it.nodoId == '" + nodoId + "' }");
        return filas.isEmpty() ? 0 : ((Number) filas.get(0).get("aLaMano")).intValue();
    }

    public boolean tieneFila(String token, String nodoId, String varianteId) {
        List<?> filas = api.getConToken(token, "/variantes/" + varianteId + "/inventario")
                .then().statusCode(200)
                .extract().path("findAll { it.nodoId == '" + nodoId + "' }");
        return !filas.isEmpty();
    }

    public Usuario crearUsuarioConRol(String prefijo, String rol) {
        String rolId = idDeRol(rol);
        return crearUsuario(prefijo, rolId);
    }

    public Usuario crearUsuarioConScopes(String prefijo, String... scopes) {
        Response rol = api.post("/roles", Map.of(
                "nombre", ClienteApi.unico(prefijo + "-rol"),
                "descripcion", "Rol de prueba de inventario"));
        exigirCreado("rol", prefijo, rol);
        String rolId = rol.path("id");
        for (String scope : scopes) {
            String scopeId = api.get("/scopes").then().statusCode(200)
                    .extract().path("find { it.codigo == '" + scope + "' }.id");
            if (scopeId == null) {
                throw new IllegalStateException("No existe el scope " + scope);
            }
            api.post("/roles/" + rolId + "/scopes", Map.of("scopeId", scopeId)).then().statusCode(204);
        }
        return crearUsuario(prefijo, rolId);
    }

    public void asignarTienda(String usuarioId, String tiendaId) {
        api.post("/usuarios/" + usuarioId + "/tiendas", Map.of("tiendaId", tiendaId))
                .then().statusCode(204);
    }

    public void asignarPrioridad(String tiendaId, String centroId) {
        api.put("/tiendas/" + tiendaId + "/prioridad-cd", Map.of("cdIds", List.of(centroId)))
                .then().statusCode(200);
    }

    public Response vender(String token, String tiendaId, String sku, int cantidad, String transaccionId) {
        return api.postConToken(token, "/ventas", cuerpoVenta(transaccionId, tiendaId, sku, cantidad));
    }

    /**
     * Versión sin beans de escenario para usarla desde hilos: los beans @ScenarioScope
     * no se pueden tocar cuando Cucumber no está ejecutando el escenario en ese hilo.
     */
    public static Response venderDirecto(String token, String tiendaId, String sku, int cantidad,
                                         String transaccionId) {
        return postDirecto(token, "/ventas", cuerpoVenta(transaccionId, tiendaId, sku, cantidad));
    }

    public static Response postDirecto(String token, String ruta, Object cuerpo) {
        return given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(cuerpo)
                .when().post(ruta);
    }

    public static Map<String, Object> cuerpoVenta(String transaccionId, String tiendaId, String sku, int cantidad) {
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("sku", sku);
        linea.put("cantidad", cantidad);
        linea.put("precioUnitario", 89900.00);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("transaccionId", transaccionId);
        cuerpo.put("tiendaId", tiendaId);
        cuerpo.put("canal", "TIENDA");
        cuerpo.put("forzarSinStock", false);
        cuerpo.put("items", List.of(linea));
        return cuerpo;
    }

    public static String nuevaTransaccion() {
        return UUID.randomUUID().toString();
    }

    public static Map<String, Object> item(String sku, int cantidad) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("sku", sku);
        item.put("cantidad", cantidad);
        return item;
    }

    public static List<Map<String, Object>> loteDeMasDe500(String prefijo) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            items.add(item(prefijo + "-" + i, 1));
        }
        return items;
    }

    private Usuario crearUsuario(String prefijo, String rolId) {
        String email = ClienteApi.unico(prefijo) + "@supplychain-test.local";
        Response usuario = api.post("/usuarios", Map.of(
                "email", email,
                "nombreCompleto", "Usuario QA " + prefijo,
                "password", PASSWORD));
        exigirCreado("usuario", prefijo, usuario);
        String usuarioId = usuario.path("id");
        api.post("/usuarios/" + usuarioId + "/roles", Map.of("rolId", rolId)).then().statusCode(204);
        String token = api.iniciarSesion(email, PASSWORD);
        return new Usuario(usuarioId, email, token);
    }

    private String idDeRol(String nombre) {
        String id = api.get("/roles").then().statusCode(200)
                .extract().path("find { it.nombre == '" + nombre + "' }.id");
        if (id == null) {
            throw new IllegalStateException("No existe el rol " + nombre);
        }
        return id;
    }

    private void guardarTienda(String alias, Tienda tienda) {
        contexto.id("tienda:" + alias, tienda.id());
        contexto.texto("tienda:" + alias + ":nombre", tienda.nombre());
        contexto.id("tienda:" + alias + ":almacen", tienda.almacenId());
        contexto.id("tienda:" + alias + ":bodega", tienda.bodegaId());
    }

    private void exigirCreado(String tipo, String alias, Response respuesta) {
        if (respuesta.statusCode() != 201) {
            throw new IllegalStateException("No se pudo crear " + tipo + " " + alias
                    + ": " + respuesta.statusCode() + " " + respuesta.asString());
        }
    }
}
