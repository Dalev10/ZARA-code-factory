package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-49 (Sprint 2). Traspaso manual Bodega → Almacén {"varianteId","cantidad"}. Registra al
 * usuario del token, nunca deja la Bodega en negativo (409), tienda no asignada 403, variante
 * inexistente 404.
 */
@Tag("funcional")
@Tag("negative-path")
class TraspasoManualIT extends AbstractCasoPruebaSprint2IT {

    private Response traspasar(String token, String tiendaId, String varianteId, int cantidad) {
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body("""
                        {"varianteId": "%s", "cantidad": %d}
                        """.formatted(varianteId, cantidad))
                .when().post("/tiendas/" + tiendaId + "/traspasos-internos");
    }

    @Test
    @DisplayName("CP-002 — Un traspaso mayor que la Bodega se rechaza (409) sin mover stock")
    void cpHu49002_bodegaInsuficiente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU49-002");
        Variante variante = crearVariante(tokenAdmin, "HU49-002");
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 3);

        traspasar(tokenAdmin, tienda.id(), variante.id(), 4)
                .then().statusCode(409)
                .body("mensaje", notNullValue());

        assertEquals(3, stock(tokenAdmin, tienda.bodegaId(), variante.id()));
        assertEquals(0, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-003 — Rechazar cantidad 0 o sin variante (400)")
    void cpHu49003_datosInvalidos() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU49-003");
        Variante variante = crearVariante(tokenAdmin, "HU49-003");

        traspasar(tokenAdmin, tienda.id(), variante.id(), 0).then().statusCode(400).body("mensaje", notNullValue());
        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json").body("{\"cantidad\": 1}")
                .when().post("/tiendas/" + tienda.id() + "/traspasos-internos")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("CP-004 — Una variante inexistente responde 404")
    void cpHu49004_varianteInexistente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU49-004");

        traspasar(tokenAdmin, tienda.id(), UUID.randomUUID().toString(), 1).then().statusCode(404);
    }

    @Test
    @DisplayName("CP-007 — Consultar un traspaso inexistente responde 404")
    void cpHu49007_traspasoInexistente() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/traspasos-internos/" + UUID.randomUUID())
                .then().statusCode(404);
    }
}
