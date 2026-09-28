package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-10 — Eliminar Bodega_Tienda. Casos CP-HU10-001 a CP-HU10-003.
 */
class Hu10EliminarBodegaTiendaIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-HU10-001 — Eliminar Bodega_Tienda sin inventario asociado")
    void cpHu10001_eliminarBodegaSinInventarioAsociado() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "La Ceja, Antioquia"}
                        """.formatted(unico("Tienda HU10-001")))
                .when().post("/tiendas")
                .then().statusCode(201)
                .extract().path("id");
        String bodegaId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(tiendaId))
                .when().post("/bodegas-tienda")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().delete("/bodegas-tienda/" + bodegaId)
                .then().statusCode(204);

        // AC-4: una Bodega_Tienda eliminada no debe estar disponible para nuevas
        // asignaciones — pero SÍ debe permitir registrar una nueva bodega para la
        // misma Tienda, ya que la anterior ya no existe.
        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(tiendaId))
                .when().post("/bodegas-tienda")
                .then().statusCode(201);
    }

    @Test
    @Disabled("No ejecutable en Sprint 1: no existe ningún endpoint de inventario para generar la "
            + "precondición 'Bodega_Tienda con inventario asociado'. El guard de código "
            + "(BodegaTiendaConInventarioAsociadoException en BodegaTiendaService.eliminar) existe, "
            + "pero no se puede activar todavía vía API.")
    @DisplayName("CP-HU10-002 — Bloquear eliminación de Bodega_Tienda con inventario asociado")
    void cpHu10002_bloquearEliminacionConInventarioAsociado() {
        // Intencionalmente vacío: ver motivo en @Disabled.
    }

    @Test
    @DisplayName("CP-HU10-003 — Conservar información histórica tras eliminar una Bodega_Tienda [defecto D-03]")
    void cpHu10003_conservarInformacionHistorica() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "El Retiro, Antioquia"}
                        """.formatted(unico("Tienda HU10-003")))
                .when().post("/tiendas")
                .then().statusCode(201)
                .extract().path("id");
        String bodegaId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(tiendaId))
                .when().post("/bodegas-tienda")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().delete("/bodegas-tienda/" + bodegaId)
                .then().statusCode(204);

        // AC-5: la información histórica que deba conservarse no debe verse afectada.
        // Hoy el borrado es físico: la consulta devuelve 404 de inmediato, sin ningún
        // rastro histórico. Este assert documenta el defecto D-03 (severidad Media).
        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/" + bodegaId)
                .then().statusCode(200)
                .body("id", equalTo(bodegaId));
    }
}
