package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

/**
 * HU-43 y HU-44 (Sprint 2). Una solicitud no valida stock: guarda el stockSistema del Almacén
 * aunque sea positivo (inventario fantasma). La misma clave con los mismos datos responde 200;
 * con otros datos, 409. Anular exige motivo y repetir la anulación es idempotente (200).
 */
@Tag("funcional")
@Tag("negative-path")
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
    @DisplayName("CP-004 — La misma clave con otros datos responde 409")
    void cpHu43004_claveReutilizadaConOtrosDatos() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU43-004");
        Variante variante = crearVariante(tokenAdmin, "HU43-004");
        String clave = unico("clave");
        registrar(tokenAdmin, tienda.id(), variante.id(), 1, clave).then().statusCode(201);

        registrar(tokenAdmin, tienda.id(), variante.id(), 3, clave)
                .then().statusCode(409)
                .body("mensaje", notNullValue());
    }

    @Test
    @DisplayName("CP-005 — Una variante fuera de catálogo responde 404")
    void cpHu43005_varianteFueraDeCatalogo() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU43-005");

        registrar(tokenAdmin, tienda.id(), UUID.randomUUID().toString(), 1, unico("clave"))
                .then().statusCode(404)
                .body("mensaje", notNullValue());
    }

    @Test
    @DisplayName("CP-007 — Rechazar sin clave de idempotencia o con cantidad 0 (400)")
    void cpHu43007_rechazarDatosInvalidos() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU43-007");
        Variante variante = crearVariante(tokenAdmin, "HU43-007");

        registrar(tokenAdmin, tienda.id(), variante.id(), 1, "").then().statusCode(400);
        registrar(tokenAdmin, tienda.id(), variante.id(), 0, unico("clave")).then().statusCode(400);
    }
}
