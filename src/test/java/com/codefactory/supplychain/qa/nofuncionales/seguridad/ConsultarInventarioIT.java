package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * HU-39 (Sprint 2). Consultas de stock por nodo y por variante y kardex con filtros
 * tipo/desde/hasta en UTC. Sin acceso global solo se ven los nodos de las tiendas asignadas (un
 * nodo ajeno responde 404); un CD lo ven ADMIN y OPERADOR_CD.
 */
@Tag("no-funcional")
@Tag("seguridad")
class ConsultarInventarioIT extends AbstractCasoPruebaSprint2IT {

    private String nodoDeCdNuevo(String tokenAdmin, String prefijo) {
        String cdId = given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Bello, Antioquia"}
                        """.formatted(unico(prefijo)))
                .when().post("/centros-distribucion")
                .then().statusCode(201)
                .extract().path("id");
        return given().header("Authorization", bearer(tokenAdmin))
                .when().get("/centros-distribucion/" + cdId)
                .then().statusCode(200)
                .extract().path("nodo.id");
    }

    @Test
    @DisplayName("CP-005 — Un vendedor no ve el inventario de una tienda que no tiene asignada (404)")
    void cpHu39005_nodoAjenoResponde404() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda propia = crearTienda(tokenAdmin, "Tienda HU39-005A");
        Tienda ajena = crearTienda(tokenAdmin, "Tienda HU39-005B");
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu39-005", "VENDEDOR");
        asignarTienda(tokenAdmin, vendedor.id(), propia.id());

        given().header("Authorization", bearer(vendedor.token()))
                .when().get("/nodos/" + propia.almacenId() + "/inventario")
                .then().statusCode(200);
        given().header("Authorization", bearer(vendedor.token()))
                .when().get("/nodos/" + ajena.almacenId() + "/inventario")
                .then().statusCode(404);
        given().header("Authorization", bearer(vendedor.token()))
                .when().get("/nodos/" + ajena.almacenId() + "/inventario/movimientos")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("CP-006 — La consulta por variante solo muestra nodos de las tiendas asignadas")
    void cpHu39006_stockDeVarianteFiltradoPorAlcance() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda propia = crearTienda(tokenAdmin, "Tienda HU39-006A");
        Tienda ajena = crearTienda(tokenAdmin, "Tienda HU39-006B");
        Variante variante = crearVariante(tokenAdmin, "HU39-006");
        cargar(tokenAdmin, propia.almacenId(), variante.sku(), 3);
        cargar(tokenAdmin, ajena.almacenId(), variante.sku(), 8);
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu39-006", "VENDEDOR");
        asignarTienda(tokenAdmin, vendedor.id(), propia.id());

        given().header("Authorization", bearer(vendedor.token()))
                .when().get("/variantes/" + variante.id() + "/inventario")
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].nodoId", equalTo(propia.almacenId()));
    }

    @Test
    @DisplayName("CP-007 — Un CD lo ven ADMIN y OPERADOR_CD; un VENDEDOR recibe 404")
    void cpHu39007_visibilidadDeCd() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String nodoCd = nodoDeCdNuevo(tokenAdmin, "CD HU39-007");
        Usuario operador = usuarioConRol(tokenAdmin, "hu39-007-op", "OPERADOR_CD");
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu39-007-ven", "VENDEDOR");

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/nodos/" + nodoCd + "/inventario").then().statusCode(200);
        given().header("Authorization", bearer(operador.token()))
                .when().get("/nodos/" + nodoCd + "/inventario").then().statusCode(200);
        given().header("Authorization", bearer(vendedor.token()))
                .when().get("/nodos/" + nodoCd + "/inventario").then().statusCode(404);
    }

    @Test
    @DisplayName("CP-008 — Consultar inventario sin el scope inventario:consultar responde 403")
    void cpHu39008_sinScopeDeConsulta() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU39-008");
        Usuario usuario = usuarioConScopes(tokenAdmin, "hu39-008", "ventas:registrar");
        asignarTienda(tokenAdmin, usuario.id(), tienda.id());

        given().header("Authorization", bearer(usuario.token()))
                .when().get("/nodos/" + tienda.almacenId() + "/inventario")
                .then().statusCode(403);
    }
}
