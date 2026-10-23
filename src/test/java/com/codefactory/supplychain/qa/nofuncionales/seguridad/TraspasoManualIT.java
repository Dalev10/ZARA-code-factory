package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-49 (Sprint 2). Traspaso manual Bodega → Almacén {"varianteId","cantidad"}. Registra al
 * usuario del token, nunca deja la Bodega en negativo (409), tienda no asignada 403, variante
 * inexistente 404.
 */
@Tag("no-funcional")
@Tag("seguridad")
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
    @DisplayName("CP-005 — Un usuario con reposicion:administrar sin la tienda asignada recibe 403")
    void cpHu49005_tiendaNoAsignada() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU49-005");
        Variante variante = crearVariante(tokenAdmin, "HU49-005");
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 5);
        Usuario usuario = usuarioConScopes(tokenAdmin, "hu49-005", "reposicion:administrar");

        traspasar(usuario.token(), tienda.id(), variante.id(), 1).then().statusCode(403);
        given().header("Authorization", bearer(usuario.token()))
                .when().get("/tiendas/" + tienda.id() + "/traspasos-internos")
                .then().statusCode(403);

        assertEquals(5, stock(tokenAdmin, tienda.bodegaId(), variante.id()));
    }
}
