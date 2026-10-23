package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * HU-51 (Sprint 2). PUT /tiendas/{tiendaId}/prioridad-cd {"cdIds":[...]} reemplaza la lista
 * completa; el orden de la lista es la prioridad. Un CD con prioridades no se elimina (409 con
 * mensaje).
 */
@Tag("no-funcional")
@Tag("seguridad")
class PrioridadCdIT extends AbstractCasoPruebaSprint2IT {

    private String crearCd(String tokenAdmin, String prefijo) {
        return given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Envigado, Antioquia"}
                        """.formatted(unico(prefijo)))
                .when().post("/centros-distribucion")
                .then().statusCode(201)
                .extract().path("id");
    }

    private Response reemplazar(String token, String tiendaId, String... cdIds) {
        String lista = cdIds.length == 0 ? "" : "\"" + String.join("\", \"", cdIds) + "\"";
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body("""
                        {"cdIds": [%s]}
                        """.formatted(lista))
                .when().put("/tiendas/" + tiendaId + "/prioridad-cd");
    }

    @Test
    @DisplayName("CP-007 — Definir prioridades exige inventario:administrar (403 para un vendedor)")
    void cpHu51007_exigeScope() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU51-007");
        String cd = crearCd(tokenAdmin, "CD HU51-007");
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu51-007", "VENDEDOR");
        asignarTienda(tokenAdmin, vendedor.id(), tienda.id());

        reemplazar(vendedor.token(), tienda.id(), cd).then().statusCode(403);
    }
}
