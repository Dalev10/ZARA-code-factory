package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * HU-07 (Sprint 1) actualizada por D-55 (HU-34, Sprint 2): la Bodega_Tienda ya no se
 * registra por API, se crea junto con su tienda.
 */
@Tag("funcional")
@Tag("negative-path")
class RegistrarBodegaTiendaIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-002 — Consultar la Bodega_Tienda de una Tienda inexistente responde 404")
    void cpHu07002_rechazarSinTiendaValida() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/tienda/" + UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    @DisplayName("CP-003 — Registrar una Bodega_Tienda por API ya no está permitido (405)")
    void cpHu07003_rechazarBodegaTiendaDuplicada() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(UUID.randomUUID()))
                .when().post("/bodegas-tienda")
                .then().statusCode(405);
    }
}
