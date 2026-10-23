package com.codefactory.supplychain.qa.soporte;

import com.codefactory.supplychain.qa.config.ClienteApi;
import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@Component
@ScenarioScope
public class VentasDatos {

    public record Tienda(String id, String almacenId, String bodegaId) {
    }

    public record Variante(String id, String sku, String categoriaId) {
    }

    public record Usuario(String id, String email, String token) {
    }

    public record LineaVenta(String varianteAlias, int cantidad) {
    }

    private final ClienteApi api;
    private final ContextoPrueba contexto;
    private final Map<String, Object> memorias = new HashMap<>();

    public VentasDatos(ClienteApi api, ContextoPrueba contexto) {
        this.api = api;
        this.contexto = contexto;
    }

    public Tienda crearTienda(String alias) {
        Response respuesta = api.post("/tiendas", Map.of(
                "nombre", ClienteApi.unico("Tienda " + alias),
                "ubicacion", "Medellín, Antioquia"));
        exigir(respuesta, 201, "crear la tienda " + alias);
        Tienda tienda = new Tienda(respuesta.path("id"), respuesta.path("almacenId"), respuesta.path("bodegaId"));
        contexto.id("tienda:" + alias, tienda.id());
        contexto.id("tienda:" + alias + ":almacen", tienda.almacenId());
        contexto.id("tienda:" + alias + ":bodega", tienda.bodegaId());
        contexto.id("almacen:" + alias, tienda.almacenId());
        contexto.id("bodega:" + alias, tienda.bodegaId());
        contexto.id("tiendaId", tienda.id());
        contexto.id("almacenId", tienda.almacenId());
        return tienda;
    }

    public Variante crearVariante(String alias) {
        Response categoria = api.post("/categorias", Map.of(
                "nombre", ClienteApi.unico("Categoría " + alias)));
        exigir(categoria, 201, "crear la categoría de la variante " + alias);
        String categoriaId = categoria.path("id");

        Response template = api.post("/templates", Map.of(
                "nombre", ClienteApi.unico("Template " + alias),
                "temporada", "OI-2026",
                "proveedor", "Proveedor QA",
                "precioBase", 89900,
                "categoriaId", categoriaId));
        exigir(template, 201, "crear el template de la variante " + alias);

        String sku = ClienteApi.unico("SKU-" + alias.replace(' ', '-')).toUpperCase();
        Response respuesta = api.post("/variantes", Map.of(
                "sku", sku,
                "talla", "M",
                "color", "Negro",
                "templateId", template.path("id")));
        exigir(respuesta, 201, "crear la variante " + alias);

        String varianteId = respuesta.path("id");
        contexto.id("variante:" + alias, varianteId);
        contexto.id("varianteId", varianteId);
        contexto.id("categoria:" + alias, categoriaId);
        contexto.texto("variante:" + alias + ":sku", sku);
        contexto.texto("sku:" + alias, sku);
        return new Variante(varianteId, sku, categoriaId);
    }

    public void cargar(String tiendaAlias, String varianteAlias, int cantidad) {
        Response respuesta = cargaInicial(tiendaAlias, varianteAlias, cantidad);
        exigir(respuesta, 201, "cargar inventario de la variante " + varianteAlias);
    }

    public Response cargaInicial(String tiendaAlias, String varianteAlias, int cantidad) {
        return api.post("/nodos/" + almacen(tiendaAlias) + "/inventario/carga-inicial",
                itemsJson(varianteAlias, cantidad));
    }

    public Response ajustar(String tiendaAlias, String varianteAlias, int delta, String motivo) {
        String cuerpo = """
                {"sku": "%s", "delta": %d, "motivo": "%s", "comentario": null}"""
                .formatted(sku(varianteAlias), delta, motivo);
        return api.post("/nodos/" + almacen(tiendaAlias) + "/inventario/ajustes", cuerpo);
    }

    public int stock(String tiendaAlias, String varianteAlias) {
        Response respuesta = api.get("/variantes/" + variante(varianteAlias) + "/inventario");
        exigir(respuesta, 200, "consultar el inventario de la variante " + varianteAlias);
        List<Map<String, Object>> filas = filas(respuesta);
        String almacenId = almacen(tiendaAlias);
        return filas.stream()
                .filter(fila -> almacenId.equals(fila.get("nodoId")))
                .findFirst()
                .map(fila -> ((Number) fila.get("aLaMano")).intValue())
                .orElse(0);
    }

    public boolean tieneFila(String tiendaAlias, String varianteAlias) {
        Response respuesta = api.get("/variantes/" + variante(varianteAlias) + "/inventario");
        exigir(respuesta, 200, "consultar el inventario de la variante " + varianteAlias);
        String almacenId = almacen(tiendaAlias);
        return filas(respuesta).stream().anyMatch(fila -> almacenId.equals(fila.get("nodoId")));
    }

    public Usuario usuarioConRol(String alias, String rol) {
        String email = ClienteApi.unico(alias) + "@supplychain-test.local";
        String usuarioId = registrarUsuario(email, "Usuario QA " + alias, ClienteApi.PASSWORD_QA);
        asignarRol(usuarioId, idDeRol(rol));
        return guardarUsuario(alias, usuarioId, email);
    }

    public Usuario usuarioConScopes(String alias, String... scopes) {
        String rolId = crearRol(ClienteApi.unico(alias + "-rol"));
        for (String scope : scopes) {
            asignarScope(rolId, idDeScope(scope));
        }
        String email = ClienteApi.unico(alias) + "@supplychain-test.local";
        String usuarioId = registrarUsuario(email, "Usuario QA " + alias, ClienteApi.PASSWORD_QA);
        asignarRol(usuarioId, rolId);
        return guardarUsuario(alias, usuarioId, email);
    }

    public void asignarTienda(String usuarioAlias, String tiendaAlias) {
        Response respuesta = api.post("/usuarios/" + usuario(usuarioAlias) + "/tiendas",
                Map.of("tiendaId", tienda(tiendaAlias)));
        exigir(respuesta, 204, "asignar la tienda " + tiendaAlias + " al usuario " + usuarioAlias);
    }

    public Response vender(String token, String tiendaAlias, String transaccionAlias, boolean forzar,
                           List<LineaVenta> lineas) {
        return venderConBody(token, tiendaAlias, transaccionAlias,
                cuerpoVenta(transaccion(transaccionAlias), tienda(tiendaAlias), forzar, lineas, true));
    }

    public Response venderSinOpcionales(String token, String tiendaAlias, String transaccionAlias,
                                        String varianteAlias, int cantidad) {
        String cuerpo = """
                {"transaccionId": "%s", "tiendaId": "%s", "items": [%s]}"""
                .formatted(transaccion(transaccionAlias), tienda(tiendaAlias), linea(varianteAlias, cantidad));
        return venderConBody(token, tiendaAlias, transaccionAlias, cuerpo);
    }

    public Response venderConPrecio(String token, String tiendaAlias, String transaccionAlias,
                                    String varianteAlias, int cantidad, String precio) {
        String cuerpo = cuerpoVenta(transaccion(transaccionAlias), tienda(tiendaAlias), false,
                List.of(new LineaVenta(varianteAlias, cantidad)), false, precio);
        return venderConBody(token, tiendaAlias, transaccionAlias, cuerpo);
    }

    public Response venderConSku(String token, String tiendaAlias, String transaccionAlias,
                                 String sku, int cantidad) {
        String item = """
                {"sku": "%s", "cantidad": %d, "precioUnitario": 89900.00}""".formatted(sku, cantidad);
        String cuerpo = """
                {"transaccionId": "%s", "tiendaId": "%s", "canal": "TIENDA", "forzarSinStock": false,
                 "items": [%s]}""".formatted(transaccion(transaccionAlias), tienda(tiendaAlias), item);
        return venderConBody(token, tiendaAlias, transaccionAlias, cuerpo);
    }

    public Response venderSinLineas(String token, String tiendaAlias, String transaccionAlias) {
        String cuerpo = cuerpoVenta(transaccion(transaccionAlias), tienda(tiendaAlias), false,
                List.of(), true);
        return venderConBody(token, tiendaAlias, transaccionAlias, cuerpo);
    }

    public Response repetirUltimaVenta() {
        String cuerpo = contexto.texto("venta:ultimoCuerpo");
        if (cuerpo == null) {
            throw new IllegalStateException("No hay una venta registrada para repetir");
        }
        String token = contexto.texto("venta:ultimoToken");
        Response respuesta = token == null
                ? api.post("/ventas", cuerpo)
                : api.postConToken(token, "/ventas", cuerpo);
        guardarVenta(respuesta, contexto.texto("venta:ultimaTransaccionAlias"));
        return respuesta;
    }

    public Response anularLinea(String token, String lineaAlias, String motivoJson) {
        return postComo(token, "/ventas/" + linea(lineaAlias) + "/anular", motivoJson);
    }

    public Response anularTransaccion(String token, String transaccionAlias, String motivoJson) {
        return postComo(token, "/ventas/transacciones/" + transaccion(transaccionAlias) + "/anular", motivoJson);
    }

    public Response anularLineaInexistente(String token, String motivoJson) {
        return postComo(token, "/ventas/" + UUID.randomUUID() + "/anular", motivoJson);
    }

    public Response anularTransaccionInexistente(String token, String motivoJson) {
        return postComo(token, "/ventas/transacciones/" + UUID.randomUUID() + "/anular", motivoJson);
    }

    public Response consultarVentas(String token, Map<String, ?> filtros) {
        return getComo(token, "/ventas", filtros);
    }

    public Response consultarVentasDesdeFuturo(String token, String tiendaAlias) {
        String futuro = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        return getComo(token, "/ventas", Map.of("tiendaId", tienda(tiendaAlias), "desde", futuro));
    }

    public Response consultarLinea(String token, String lineaAlias) {
        return getComo(token, "/ventas/" + linea(lineaAlias), Map.of());
    }

    public Response registrarSolicitud(String token, String tiendaAlias, String varianteAlias,
                                       Integer cantidad, String claveAlias, String clave) {
        String cantidadJson = cantidad == null ? "" : "\"cantidad\": " + cantidad + ",";
        String cuerpo = """
                {"tiendaId": "%s", "varianteId": "%s", %s "claveIdempotencia": "%s"}"""
                .formatted(tienda(tiendaAlias), variante(varianteAlias), cantidadJson, clave);
        Response respuesta = postComo(token, "/solicitudes-no-satisfechas", cuerpo);
        if (claveAlias != null) {
            contexto.texto("solicitudClave:" + claveAlias, clave);
            if (respuesta.path("id") != null) {
                contexto.id("solicitud:" + claveAlias, respuesta.path("id"));
            }
        }
        if (respuesta.path("id") != null) {
            contexto.id("solicitudId", respuesta.path("id"));
        }
        return respuesta;
    }

    public Response registrarSolicitudVarianteInexistente(String token, String tiendaAlias, Integer cantidad,
                                                          String claveAlias, String clave) {
        String cantidadJson = cantidad == null ? "" : "\"cantidad\": " + cantidad + ",";
        String cuerpo = """
                {"tiendaId": "%s", "varianteId": "%s", %s "claveIdempotencia": "%s"}"""
                .formatted(tienda(tiendaAlias), UUID.randomUUID(), cantidadJson, clave);
        Response respuesta = postComo(token, "/solicitudes-no-satisfechas", cuerpo);
        if (claveAlias != null) {
            contexto.texto("solicitudClave:" + claveAlias, clave);
        }
        return respuesta;
    }

    public String clave(String alias) {
        String clave = contexto.texto("solicitudClave:" + alias);
        if (clave != null) {
            return clave;
        }
        String nueva = ClienteApi.unico("clave");
        contexto.texto("solicitudClave:" + alias, nueva);
        return nueva;
    }

    public Response anularSolicitud(String token, String solicitudAlias, String motivoJson) {
        return postComo(token, "/solicitudes-no-satisfechas/" + solicitud(solicitudAlias) + "/anular", motivoJson);
    }

    public Response consultarSolicitud(String token, String solicitudAlias) {
        return getComo(token, "/solicitudes-no-satisfechas/" + solicitud(solicitudAlias), Map.of());
    }

    public Response consultarSolicitudes(String token, Map<String, ?> filtros) {
        return getComo(token, "/solicitudes-no-satisfechas", filtros);
    }

    public Response consultarVentanas(String token, String tiendaAlias, String varianteAlias) {
        Response respuesta = getComo(token, "/ventanas-quiebre",
                Map.of("tiendaId", tienda(tiendaAlias), "varianteId", variante(varianteAlias)));
        recordarVentanaDeLaRespuesta(respuesta);
        return respuesta;
    }

    public Response consultarVentanasAbiertas(String token, String tiendaAlias) {
        Response respuesta = getComo(token, "/ventanas-quiebre",
                Map.of("tiendaId", tienda(tiendaAlias), "abierta", true));
        recordarVentanaDeLaRespuesta(respuesta);
        return respuesta;
    }

    public Response consultarVentanaActual(String token) {
        String ventanaId = contexto.id("ventana:id");
        if (ventanaId == null) {
            throw new IllegalStateException("No se ha consultado ninguna ventana de quiebre");
        }
        return getComo(token, "/ventanas-quiebre/" + ventanaId, Map.of());
    }

    public List<Map<String, Object>> ventanas(String tiendaAlias, String varianteAlias) {
        Response respuesta = consultarVentanas(null, tiendaAlias, varianteAlias);
        exigir(respuesta, 200, "consultar las ventanas de quiebre");
        return respuesta.jsonPath().getList("content");
    }

    public void recordarVentanas(String tiendaAlias, String varianteAlias) {
        memorias.put("ventanas", ventanas(tiendaAlias, varianteAlias));
    }

    public boolean ventanasSinCambios(String tiendaAlias, String varianteAlias) {
        return Objects.equals(memorias.get("ventanas"), ventanas(tiendaAlias, varianteAlias));
    }

    public Response reconciliar(String token, String tiendaAlias) {
        return postComo(token, "/ventanas-quiebre/reconciliar", Map.of("tiendaId", tienda(tiendaAlias)));
    }

    public Response reconciliarSinTienda(String token) {
        return postComo(token, "/ventanas-quiebre/reconciliar", Map.of());
    }

    public String tokenDe(String usuarioAlias) {
        if (usuarioAlias == null) {
            return contexto.getToken();
        }
        String token = contexto.texto("usuarioToken:" + usuarioAlias);
        if (token == null) {
            throw new IllegalStateException("No existe el usuario " + usuarioAlias);
        }
        return token;
    }

    public String transaccion(String alias) {
        String existente = contexto.id("transaccion:" + alias);
        if (existente != null) {
            return existente;
        }
        String nueva = UUID.randomUUID().toString();
        contexto.id("transaccion:" + alias, nueva);
        return nueva;
    }

    public String transaccionId(String alias) {
        return contexto.id("transaccion:" + alias);
    }

    private Response venderConBody(String token, String tiendaAlias, String transaccionAlias, String cuerpo) {
        Response respuesta = postComo(token, "/ventas", cuerpo);
        contexto.texto("venta:ultimoCuerpo", cuerpo);
        contexto.texto("venta:ultimoToken", token);
        contexto.texto("venta:ultimaTransaccionAlias", transaccionAlias);
        contexto.id("venta:tienda:" + transaccionAlias, tienda(tiendaAlias));
        guardarVenta(respuesta, transaccionAlias);
        return respuesta;
    }

    private void guardarVenta(Response respuesta, String transaccionAlias) {
        Object lineaId = respuesta.path("lineas[0].id");
        if (lineaId != null) {
            contexto.id("linea:" + transaccionAlias, lineaId.toString());
            contexto.id("venta:ultimaLinea", lineaId.toString());
        }
        Object id = respuesta.path("id");
        if (id != null) {
            contexto.id("venta:" + transaccionAlias, id.toString());
        }
    }

    private String cuerpoVenta(String transaccionId, String tiendaId, boolean forzar,
                               List<LineaVenta> lineas, boolean incluirOpcionales) {
        List<String> items = new ArrayList<>();
        for (LineaVenta linea : lineas) {
            items.add(linea(linea.varianteAlias(), linea.cantidad()));
        }
        if (!incluirOpcionales) {
            return """
                    {"transaccionId": "%s", "tiendaId": "%s", "items": [%s]}"""
                    .formatted(transaccionId, tiendaId, String.join(",", items));
        }
        return """
                {"transaccionId": "%s", "tiendaId": "%s", "canal": "TIENDA", "forzarSinStock": %s,
                 "items": [%s]}""".formatted(transaccionId, tiendaId, forzar, String.join(",", items));
    }

    private String cuerpoVenta(String transaccionId, String tiendaId, boolean forzar,
                               List<LineaVenta> lineas, boolean incluirOpcionales, String precio) {
        List<String> items = new ArrayList<>();
        for (LineaVenta linea : lineas) {
            items.add("""
                    {"sku": "%s", "cantidad": %d, "precioUnitario": %s}"""
                    .formatted(sku(linea.varianteAlias()), linea.cantidad(), precio));
        }
        return """
                {"transaccionId": "%s", "tiendaId": "%s", "canal": "TIENDA", "forzarSinStock": %s,
                 "items": [%s]}""".formatted(transaccionId, tiendaId, forzar, String.join(",", items));
    }

    private String linea(String varianteAlias, int cantidad) {
        return """
                {"sku": "%s", "cantidad": %d, "precioUnitario": 89900.00}"""
                .formatted(sku(varianteAlias), cantidad);
    }

    private String itemsJson(String varianteAlias, int cantidad) {
        return """
                {"items": [{"sku": "%s", "cantidad": %d}]}""".formatted(sku(varianteAlias), cantidad);
    }

    private String registrarUsuario(String email, String nombreCompleto, String password) {
        Response respuesta = api.post("/usuarios", Map.of(
                "email", email,
                "nombreCompleto", nombreCompleto,
                "password", password));
        exigir(respuesta, 201, "registrar el usuario " + email);
        return respuesta.path("id");
    }

    private String crearRol(String nombre) {
        Response respuesta = api.post("/roles", Map.of(
                "nombre", nombre,
                "descripcion", "Rol de prueba de ventas"));
        exigir(respuesta, 201, "crear el rol " + nombre);
        return respuesta.path("id");
    }

    private String idDeRol(String nombre) {
        Response respuesta = api.get("/roles");
        exigir(respuesta, 200, "consultar los roles");
        return respuesta.path("find { it.nombre == '" + nombre + "' }.id");
    }

    private String idDeScope(String codigo) {
        Response respuesta = api.get("/scopes");
        exigir(respuesta, 200, "consultar los scopes");
        return respuesta.path("find { it.codigo == '" + codigo + "' }.id");
    }

    private void asignarRol(String usuarioId, String rolId) {
        Response respuesta = api.post("/usuarios/" + usuarioId + "/roles", Map.of("rolId", rolId));
        exigir(respuesta, 204, "asignar el rol al usuario");
    }

    private void asignarScope(String rolId, String scopeId) {
        Response respuesta = api.post("/roles/" + rolId + "/scopes", Map.of("scopeId", scopeId));
        exigir(respuesta, 204, "asignar el scope al rol");
    }

    private Usuario guardarUsuario(String alias, String usuarioId, String email) {
        String token = api.iniciarSesion(email, ClienteApi.PASSWORD_QA);
        contexto.id("usuario:" + alias, usuarioId);
        contexto.texto("usuarioEmail:" + alias, email);
        contexto.texto("usuarioToken:" + alias, token);
        contexto.id("usuarioId", usuarioId);
        return new Usuario(usuarioId, email, token);
    }

    private Response postComo(String token, String ruta, Object cuerpo) {
        if (token == null || token.equals(contexto.getToken())) {
            return api.post(ruta, cuerpo);
        }
        return api.postConToken(token, ruta, cuerpo);
    }

    private Response getComo(String token, String ruta, Map<String, ?> query) {
        if (token == null || token.equals(contexto.getToken())) {
            return query.isEmpty() ? api.get(ruta) : api.get(ruta, query);
        }
        Response respuesta = given()
                .header("Authorization", "Bearer " + token)
                .queryParams(query)
                .when().get(ruta);
        contexto.setRespuesta(respuesta);
        return respuesta;
    }

    private void recordarVentanaDeLaRespuesta(Response respuesta) {
        List<Map<String, Object>> content = respuesta.jsonPath().getList("content");
        if (content != null && !content.isEmpty()) {
            contexto.id("ventana:id", String.valueOf(content.get(0).get("id")));
        }
    }

    private List<Map<String, Object>> filas(Response respuesta) {
        List<Map<String, Object>> filas = respuesta.jsonPath().getList("$");
        return filas == null ? List.of() : filas;
    }

    private String tienda(String alias) {
        return requerido("tienda:" + alias, "tienda " + alias);
    }

    private String almacen(String alias) {
        return requerido("almacen:" + alias, "almacén de la tienda " + alias);
    }

    private String variante(String alias) {
        return requerido("variante:" + alias, "variante " + alias);
    }

    private String sku(String alias) {
        String sku = contexto.texto("sku:" + alias);
        if (sku == null) {
            throw new IllegalStateException("No existe la variante " + alias);
        }
        return sku;
    }

    private String usuario(String alias) {
        return requerido("usuario:" + alias, "usuario " + alias);
    }

    private String linea(String alias) {
        return requerido("linea:" + alias, "línea de venta " + alias);
    }

    private String solicitud(String alias) {
        return requerido("solicitud:" + alias, "solicitud no satisfecha " + alias);
    }

    private String requerido(String clave, String descripcion) {
        String valor = contexto.id(clave);
        if (valor == null) {
            throw new IllegalStateException("No existe " + descripcion);
        }
        return valor;
    }

    private void exigir(Response respuesta, int codigo, String accion) {
        if (respuesta.statusCode() != codigo) {
            throw new IllegalStateException("No se pudo " + accion + ": "
                    + respuesta.statusCode() + " " + respuesta.asString());
        }
    }
}
