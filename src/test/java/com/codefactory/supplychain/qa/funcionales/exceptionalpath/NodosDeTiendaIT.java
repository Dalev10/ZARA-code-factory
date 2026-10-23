package com.codefactory.supplychain.qa.funcionales.exceptionalpath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-34 (Sprint 2, D-55). La tienda nace con su Almacén y su Bodega_Tienda; sus ids se obtienen
 * de POST /tiendas o GET /tiendas/{id}. El listado no trae los nodos.
 */
@Tag("funcional")
@Tag("exceptional-path")
class NodosDeTiendaIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-006 — Una tienda desactivada conserva su Bodega (baja lógica, D-50)")
    void cpHu34006_tiendaDesactivadaConservaBodega() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU34-006");

        given().header("Authorization", bearer(tokenAdmin))
                .when().delete("/tiendas/" + tienda.id())
                .then().statusCode(204);

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/tiendas/" + tienda.id())
                .then().statusCode(200)
                .body("estado", equalTo("INACTIVA"))
                .body("bodegaId", equalTo(tienda.bodegaId()));
    }
}
