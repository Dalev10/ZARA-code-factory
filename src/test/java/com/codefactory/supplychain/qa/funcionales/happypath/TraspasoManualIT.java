package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-49 (Sprint 2). Traspaso manual Bodega → Almacén {"varianteId","cantidad"}. Registra al
 * usuario del token, nunca deja la Bodega en negativo (409), tienda no asignada 403, variante
 * inexistente 404.
 */
@Tag("funcional")
@Tag("happy-path")
class TraspasoManualIT extends AbstractCasoPruebaSprint2IT {

    private Response traspasar(String token, String tiendaId, String varianteId, int cantidad) {
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body("""
                        {"varianteId": "%s", "cantidad": %d}
                        """.formatted(varianteId, cantidad))
                .when().post("/tiendas/" + tiendaId + "/traspasos-internos");
    }

    private String idDelAdmin(String tokenAdmin) {
        return given().header("Authorization", bearer(tokenAdmin))
                .when().get("/usuarios")
                .then().statusCode(200)
                .extract().path("find { it.email == '" + ADMIN_EMAIL + "' }.id");
    }

    @Test
    @DisplayName("CP-001 — Registrar un traspaso manual mueve stock de la Bodega al Almacén")
    void cpHu49001_traspasoManualValido() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU49-001");
        Variante variante = crearVariante(tokenAdmin, "HU49-001");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 2);
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 10);

        traspasar(tokenAdmin, tienda.id(), variante.id(), 4)
                .then().statusCode(201)
                .body("origen", equalTo("MANUAL"))
                .body("cantidad", equalTo(4))
                .body("tiendaId", equalTo(tienda.id()))
                .body("registradoPor", equalTo(idDelAdmin(tokenAdmin)))
                .body("timestampTraspaso", endsWith("Z"));

        assertEquals(6, stock(tokenAdmin, tienda.almacenId(), variante.id()));
        assertEquals(6, stock(tokenAdmin, tienda.bodegaId(), variante.id()));
    }

    @Test
    @DisplayName("CP-006 — Listar los traspasos de una tienda y consultar uno por id")
    void cpHu49006_listarYConsultar() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU49-006");
        Variante variante = crearVariante(tokenAdmin, "HU49-006");
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 10);
        String id = traspasar(tokenAdmin, tienda.id(), variante.id(), 2).then().statusCode(201)
                .extract().path("id");
        traspasar(tokenAdmin, tienda.id(), variante.id(), 3).then().statusCode(201);

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/tiendas/" + tienda.id() + "/traspasos-internos")
                .then().statusCode(200)
                .body("$", hasSize(2));
        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/traspasos-internos/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id))
                .body("cantidad", equalTo(2));
    }

    @Test
    @DisplayName("CP-008 — El traspaso queda en el kardex de la Bodega y del Almacén")
    void cpHu49008_traspasoEnKardex() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU49-008");
        Variante variante = crearVariante(tokenAdmin, "HU49-008");
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 10);
        traspasar(tokenAdmin, tienda.id(), variante.id(), 3).then().statusCode(201);

        given().header("Authorization", bearer(tokenAdmin))
                .queryParam("tipo", "TRASPASO_SALIDA")
                .when().get("/nodos/" + tienda.bodegaId() + "/inventario/movimientos")
                .then().statusCode(200)
                .body("content", hasSize(1));
        given().header("Authorization", bearer(tokenAdmin))
                .queryParam("tipo", "TRASPASO_ENTRADA")
                .when().get("/nodos/" + tienda.almacenId() + "/inventario/movimientos")
                .then().statusCode(200)
                .body("content", hasSize(1));
    }
}
