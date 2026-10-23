package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-43 y HU-44 (Sprint 2). Una solicitud no valida stock: guarda el stockSistema del Almacén
 * aunque sea positivo (inventario fantasma). La misma clave con los mismos datos responde 200;
 * con otros datos, 409. Anular exige motivo y repetir la anulación es idempotente (200).
 */
@Tag("no-funcional")
@Tag("seguridad")
class SolicitudNoSatisfechaIT extends AbstractCasoPruebaSprint2IT {

    private Response registrar(String token, String tiendaId, String varianteId, Integer cantidad, String clave) {
        String cantidadJson = cantidad == null ? "" : "\"cantidad\": " + cantidad + ",";
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s", "varianteId": "%s", %s "claveIdempotencia": "%s"}
                        """.formatted(tiendaId, varianteId, cantidadJson, clave))
                .when().post("/solicitudes-no-satisfechas");
    }

    @Test
    @DisplayName("CP-006 — Registrar en una tienda no asignada responde 403")
    void cpHu43006_tiendaNoAsignada() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU43-006");
        Variante variante = crearVariante(tokenAdmin, "HU43-006");
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu43-006", "VENDEDOR");

        registrar(vendedor.token(), tienda.id(), variante.id(), 1, unico("clave")).then().statusCode(403);
    }

    @Test
    @DisplayName("CP-008 — Consultar una solicitud por id; la de otra tienda responde 404")
    void cpHu44001_consultarPorIdConAlcance() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda propia = crearTienda(tokenAdmin, "Tienda HU44-001A");
        Tienda ajena = crearTienda(tokenAdmin, "Tienda HU44-001B");
        Variante variante = crearVariante(tokenAdmin, "HU44-001");
        String idAjena = registrar(tokenAdmin, ajena.id(), variante.id(), 1, unico("clave"))
                .then().statusCode(201).extract().path("id");
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu44-001", "VENDEDOR");
        asignarTienda(tokenAdmin, vendedor.id(), propia.id());

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/solicitudes-no-satisfechas/" + idAjena)
                .then().statusCode(200)
                .body("id", equalTo(idAjena));
        given().header("Authorization", bearer(vendedor.token()))
                .when().get("/solicitudes-no-satisfechas/" + idAjena)
                .then().statusCode(404);
    }

    @Test
    @DisplayName("CP-011 — Un vendedor no puede anular solicitudes (403)")
    void cpHu44004_vendedorNoAnula() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU44-004");
        Variante variante = crearVariante(tokenAdmin, "HU44-004");
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu44-004", "VENDEDOR");
        asignarTienda(tokenAdmin, vendedor.id(), tienda.id());
        String id = registrar(vendedor.token(), tienda.id(), variante.id(), 1, unico("clave"))
                .then().statusCode(201).extract().path("id");

        given().header("Authorization", bearer(vendedor.token()))
                .contentType("application/json").body("{\"motivo\": \"Sin permiso\"}")
                .when().post("/solicitudes-no-satisfechas/" + id + "/anular")
                .then().statusCode(403);
    }
}
