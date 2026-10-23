package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * HU-43 y HU-44 (Sprint 2). Una solicitud no valida stock: guarda el stockSistema del Almacén
 * aunque sea positivo (inventario fantasma). La misma clave con los mismos datos responde 200;
 * con otros datos, 409. Anular exige motivo y repetir la anulación es idempotente (200).
 */
@Tag("funcional")
@Tag("happy-path")
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
    @DisplayName("CP-001 — Registrar una solicitud guarda el stock del sistema aunque sea positivo")
    void cpHu43001_registrarConInventarioFantasma() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU43-001");
        Variante variante = crearVariante(tokenAdmin, "HU43-001");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 4);
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu43-001", "VENDEDOR");
        asignarTienda(tokenAdmin, vendedor.id(), tienda.id());

        registrar(vendedor.token(), tienda.id(), variante.id(), 2, unico("clave"))
                .then().statusCode(201)
                .body("stockSistema", equalTo(4))
                .body("cantidad", equalTo(2))
                .body("estado", equalTo("ACTIVA"))
                .body("registradoPor", equalTo(vendedor.id()));

        org.junit.jupiter.api.Assertions.assertEquals(4, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-009 — Anular una solicitud exige motivo y la deja ANULADA")
    void cpHu44002_anularConMotivo() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU44-002");
        Variante variante = crearVariante(tokenAdmin, "HU44-002");
        String id = registrar(tokenAdmin, tienda.id(), variante.id(), 1, unico("clave"))
                .then().statusCode(201).extract().path("id");

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json").body("{}")
                .when().post("/solicitudes-no-satisfechas/" + id + "/anular")
                .then().statusCode(400).body("mensaje", notNullValue());

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json").body("{\"motivo\": \"Registro duplicado\"}")
                .when().post("/solicitudes-no-satisfechas/" + id + "/anular")
                .then().statusCode(200)
                .body("estado", equalTo("ANULADA"))
                .body("motivoAnulacion", equalTo("Registro duplicado"))
                .body("anuladaEn", notNullValue());
    }

    @Test
    @DisplayName("CP-012 — Filtrar solicitudes por estado")
    void cpHu44005_filtrarPorEstado() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU44-005");
        Variante variante = crearVariante(tokenAdmin, "HU44-005");
        String anulada = registrar(tokenAdmin, tienda.id(), variante.id(), 1, unico("clave"))
                .then().statusCode(201).extract().path("id");
        registrar(tokenAdmin, tienda.id(), variante.id(), 2, unico("clave")).then().statusCode(201);
        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json").body("{\"motivo\": \"Filtro\"}")
                .when().post("/solicitudes-no-satisfechas/" + anulada + "/anular")
                .then().statusCode(200);

        given().header("Authorization", bearer(tokenAdmin))
                .queryParam("tiendaId", tienda.id())
                .queryParam("estado", "ACTIVA")
                .when().get("/solicitudes-no-satisfechas")
                .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].cantidad", equalTo(2));
    }
}
