package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * HU-45 y HU-46 (Sprint 2). Una ventana se abre cuando el Almacén cruza de > 0 a ≤ 0 y se cierra
 * con REPOSICION al volver a > 0. Una venta forzada la cierra con VENTA_FORZADA y nunca la abre.
 * Desactivar la tienda la cierra con TIENDA_DESACTIVADA. Una carga en 0 no abre ventana. La
 * reconciliación ejecutada dos veces no cambia nada la segunda vez.
 */
@Tag("no-funcional")
@Tag("seguridad")
class VentanaQuiebreIT extends AbstractCasoPruebaSprint2IT {

    private Response reconciliar(String token, String tiendaId) {
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(tiendaId))
                .when().post("/ventanas-quiebre/reconciliar");
    }

    @Test
    @DisplayName("CP-009 — Una ventana de otra tienda responde 404 para un vendedor sin acceso")
    void cpHu46002_ventanaDeOtraTienda() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda ajena = crearTienda(tokenAdmin, "Tienda HU46-002");
        Variante variante = crearVariante(tokenAdmin, "HU46-002");
        cargar(tokenAdmin, ajena.almacenId(), variante.sku(), 1);
        vender(tokenAdmin, ajena.id(), variante.sku(), 1);
        String ventanaId = (String) ventanas(tokenAdmin, ajena.id(), variante.id()).get(0).get("id");
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu46-002", "VENDEDOR");

        given().header("Authorization", bearer(vendedor.token()))
                .when().get("/ventanas-quiebre/" + ventanaId)
                .then().statusCode(404);
    }

    @Test
    @DisplayName("CP-011 — Reconciliar exige inventario:administrar (403 para un vendedor)")
    void cpHu46004_reconciliarExigeScope() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU46-004");
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu46-004", "VENDEDOR");
        asignarTienda(tokenAdmin, vendedor.id(), tienda.id());

        reconciliar(vendedor.token(), tienda.id()).then().statusCode(403);
    }
}
