package com.codefactory.supplychain.qa.funcionales.exceptionalpath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * HU-43 y HU-44 (Sprint 2). Una solicitud no valida stock: guarda el stockSistema del Almacén
 * aunque sea positivo (inventario fantasma). La misma clave con los mismos datos responde 200;
 * con otros datos, 409. Anular exige motivo y repetir la anulación es idempotente (200).
 */
@Tag("funcional")
@Tag("exceptional-path")
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
    @DisplayName("CP-002 — La cantidad es opcional y sin fila el stock del sistema es 0")
    void cpHu43002_cantidadOpcional() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU43-002");
        Variante variante = crearVariante(tokenAdmin, "HU43-002");

        registrar(tokenAdmin, tienda.id(), variante.id(), null, unico("clave"))
                .then().statusCode(201)
                .body("cantidad", equalTo(1))
                .body("stockSistema", equalTo(0));
    }

    @Test
    @DisplayName("CP-003 — La misma clave con los mismos datos responde 200 sin duplicar")
    void cpHu43003_claveIdempotenteMismosDatos() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU43-003");
        Variante variante = crearVariante(tokenAdmin, "HU43-003");
        String clave = unico("clave");

        String id = registrar(tokenAdmin, tienda.id(), variante.id(), 1, clave).then().statusCode(201)
                .extract().path("id");
        registrar(tokenAdmin, tienda.id(), variante.id(), 1, clave).then().statusCode(200)
                .body("id", equalTo(id));

        given().header("Authorization", bearer(tokenAdmin))
                .queryParam("tiendaId", tienda.id())
                .when().get("/solicitudes-no-satisfechas")
                .then().statusCode(200)
                .body("content", hasSize(1));
    }

    @Test
    @DisplayName("CP-010 — Anular una solicitud dos veces responde 200 con la anulación original")
    void cpHu44003_anulacionRepetidaIdempotente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU44-003");
        Variante variante = crearVariante(tokenAdmin, "HU44-003");
        String id = registrar(tokenAdmin, tienda.id(), variante.id(), 1, unico("clave"))
                .then().statusCode(201).extract().path("id");

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json").body("{\"motivo\": \"Primera\"}")
                .when().post("/solicitudes-no-satisfechas/" + id + "/anular")
                .then().statusCode(200);
        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json").body("{\"motivo\": \"Segunda\"}")
                .when().post("/solicitudes-no-satisfechas/" + id + "/anular")
                .then().statusCode(200)
                .body("motivoAnulacion", equalTo("Primera"));
    }
}
