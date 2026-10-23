package com.codefactory.supplychain.qa.soporte;

import io.restassured.response.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Base de los casos del Sprint 2 (HU-30 a HU-51). Arma por la API los datos que la guía de QA del
 * Sprint 2 indica: tiendas con su Almacén y su Bodega, variantes de catálogo, inventario inicial,
 * umbrales y usuarios con los roles sembrados (VENDEDOR, SUPERVISOR_TIENDA, OPERADOR_CD).
 */
public abstract class AbstractCasoPruebaSprint2IT extends AbstractCasoPruebaIT {

    protected static final String PASSWORD = "ClaveSegura-QA-2026!";

    protected record Tienda(String id, String almacenId, String bodegaId) {
    }

    protected record Variante(String id, String sku, String categoriaId) {
    }

    protected record Usuario(String id, String email, String token) {
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected Tienda crearTienda(String tokenAdmin, String prefijo) {
        Response r = given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Medellín, Antioquia"}
                        """.formatted(unico(prefijo)))
                .when().post("/tiendas")
                .then().statusCode(201)
                .extract().response();
        return new Tienda(r.path("id"), r.path("almacenId"), r.path("bodegaId"));
    }

    protected String crearCategoria(String tokenAdmin, String prefijo) {
        return given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"nombre": "%s"}
                        """.formatted(unico(prefijo)))
                .when().post("/categorias")
                .then().statusCode(201)
                .extract().path("id");
    }

    protected Variante crearVariante(String tokenAdmin, String prefijo) {
        return crearVarianteEnCategoria(tokenAdmin, prefijo, crearCategoria(tokenAdmin, "Cat " + prefijo));
    }

    protected Variante crearVarianteEnCategoria(String tokenAdmin, String prefijo, String categoriaId) {
        String templateId = given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "temporada": "OI-2026", "proveedor": "Proveedor QA",
                         "precioBase": 89900, "categoriaId": "%s"}
                        """.formatted(unico("Template " + prefijo), categoriaId))
                .when().post("/templates")
                .then().statusCode(201)
                .extract().path("id");
        String sku = unico("SKU-" + prefijo.replace(' ', '-')).toUpperCase();
        String varianteId = given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"sku": "%s", "talla": "M", "color": "Negro", "templateId": "%s"}
                        """.formatted(sku, templateId))
                .when().post("/variantes")
                .then().statusCode(201)
                .extract().path("id");
        return new Variante(varianteId, sku, categoriaId);
    }

    protected Response cargaInicial(String token, String nodoId, String itemsJson) {
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body("""
                        {"items": [%s]}
                        """.formatted(itemsJson))
                .when().post("/nodos/" + nodoId + "/inventario/carga-inicial");
    }

    protected static String item(String sku, int cantidad) {
        return """
                {"sku": "%s", "cantidad": %d}""".formatted(sku, cantidad);
    }

    protected void cargar(String token, String nodoId, String sku, int cantidad) {
        cargaInicial(token, nodoId, item(sku, cantidad)).then().statusCode(201);
    }

    protected Response ajustar(String token, String nodoId, String sku, int delta, String motivo, String comentario) {
        String comentarioJson = comentario == null ? "null" : "\"" + comentario + "\"";
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body("""
                        {"sku": "%s", "delta": %d, "motivo": "%s", "comentario": %s}
                        """.formatted(sku, delta, motivo, comentarioJson))
                .when().post("/nodos/" + nodoId + "/inventario/ajustes");
    }

    /** Stock a la mano de una variante en un nodo; sin fila el stock es 0 (guía, "Filas por demanda"). */
    protected int stock(String tokenAdmin, String nodoId, String varianteId) {
        List<Map<String, Object>> filas = given().header("Authorization", bearer(tokenAdmin))
                .when().get("/variantes/" + varianteId + "/inventario")
                .then().statusCode(200)
                .extract().path("findAll { it.nodoId == '" + nodoId + "' }");
        return filas.isEmpty() ? 0 : (Integer) filas.get(0).get("aLaMano");
    }

    protected boolean tieneFila(String tokenAdmin, String nodoId, String varianteId) {
        List<?> filas = given().header("Authorization", bearer(tokenAdmin))
                .when().get("/variantes/" + varianteId + "/inventario")
                .then().statusCode(200)
                .extract().path("findAll { it.nodoId == '" + nodoId + "' }");
        return !filas.isEmpty();
    }

    protected String crearUmbralVariante(String tokenAdmin, String varianteId, int minimo, int objetivo) {
        return given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"varianteId": "%s", "cantidadMinima": %d, "cantidadObjetivo": %d}
                        """.formatted(varianteId, minimo, objetivo))
                .when().post("/umbrales-reposicion")
                .then().statusCode(201)
                .extract().path("id");
    }

    protected String crearUmbralCategoria(String tokenAdmin, String categoriaId, int minimo, int objetivo) {
        return given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"categoriaId": "%s", "cantidadMinima": %d, "cantidadObjetivo": %d}
                        """.formatted(categoriaId, minimo, objetivo))
                .when().post("/umbrales-reposicion")
                .then().statusCode(201)
                .extract().path("id");
    }

    protected String idDeRol(String tokenAdmin, String nombre) {
        return given().header("Authorization", bearer(tokenAdmin))
                .when().get("/roles")
                .then().statusCode(200)
                .extract().path("find { it.nombre == '" + nombre + "' }.id");
    }

    /** Usuario nuevo con uno de los roles sembrados por V14, ya autenticado. */
    protected Usuario usuarioConRol(String tokenAdmin, String prefijo, String rol) {
        String email = unico(prefijo) + "@supplychain-test.local";
        String usuarioId = registrarUsuario(tokenAdmin, email, "Usuario QA " + prefijo, PASSWORD);
        asignarRolAUsuario(tokenAdmin, usuarioId, idDeRol(tokenAdmin, rol));
        return new Usuario(usuarioId, email, iniciarSesion(email, PASSWORD));
    }

    /** Usuario con un rol propio que solo tiene los scopes indicados, ya autenticado. */
    protected Usuario usuarioConScopes(String tokenAdmin, String prefijo, String... scopes) {
        String rolId = crearRol(tokenAdmin, unico(prefijo + "-rol"));
        for (String scope : scopes) {
            asignarScopeARol(tokenAdmin, rolId, idDeScope(tokenAdmin, scope));
        }
        String email = unico(prefijo) + "@supplychain-test.local";
        String usuarioId = registrarUsuario(tokenAdmin, email, "Usuario QA " + prefijo, PASSWORD);
        asignarRolAUsuario(tokenAdmin, usuarioId, rolId);
        return new Usuario(usuarioId, email, iniciarSesion(email, PASSWORD));
    }

    protected void asignarTienda(String tokenAdmin, String usuarioId, String tiendaId) {
        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(tiendaId))
                .when().post("/usuarios/" + usuarioId + "/tiendas")
                .then().statusCode(204);
    }

    protected static String cuerpoVenta(String transaccionId, String tiendaId, boolean forzar, String... items) {
        return """
                {"transaccionId": "%s", "tiendaId": "%s", "canal": "TIENDA", "forzarSinStock": %s,
                 "items": [%s]}
                """.formatted(transaccionId, tiendaId, forzar, String.join(",", items));
    }

    protected static String linea(String sku, int cantidad) {
        return """
                {"sku": "%s", "cantidad": %d, "precioUnitario": 89900.00}""".formatted(sku, cantidad);
    }

    protected Response registrarVenta(String token, String cuerpo) {
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body(cuerpo)
                .when().post("/ventas");
    }

    /** Registra una venta normal de una línea y devuelve la respuesta 201. */
    protected Response vender(String token, String tiendaId, String sku, int cantidad) {
        return registrarVenta(token, cuerpoVenta(UUID.randomUUID().toString(), tiendaId, false, linea(sku, cantidad)))
                .then().statusCode(201)
                .extract().response();
    }

    protected Response venderForzado(String token, String tiendaId, String sku, int cantidad) {
        return registrarVenta(token, cuerpoVenta(UUID.randomUUID().toString(), tiendaId, true, linea(sku, cantidad)))
                .then().statusCode(201)
                .extract().response();
    }

    protected List<Map<String, Object>> traspasos(String tokenAdmin, String tiendaId) {
        return given().header("Authorization", bearer(tokenAdmin))
                .when().get("/tiendas/" + tiendaId + "/traspasos-internos")
                .then().statusCode(200)
                .extract().path("$");
    }

    protected List<Map<String, Object>> ventanas(String tokenAdmin, String tiendaId, String varianteId) {
        return given().header("Authorization", bearer(tokenAdmin))
                .queryParam("tiendaId", tiendaId)
                .queryParam("varianteId", varianteId)
                .when().get("/ventanas-quiebre")
                .then().statusCode(200)
                .extract().path("content");
    }
}
