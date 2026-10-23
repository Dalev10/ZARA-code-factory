package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Tag("funcional")
@Tag("happy-path")
class ConsultarCentroDistribucionIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-001 — Consultar Centro de Distribución existente")
    void cpHu12001_consultarCentroDistribucionExistente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String nombre = unico("CD HU12-001");
        String cdId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Sabaneta, Antioquia"}
                        """.formatted(nombre))
                .when().post("/centros-distribucion")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/centros-distribucion/" + cdId)
                .then().statusCode(200)
                .body("nombre", equalTo(nombre))
                .body("nodo.tipo", equalTo("CD"));
    }

    /**
     * En el Sprint 1 no había endpoint de inventario de CD. Desde HU-39 el inventario del CD se
     * consulta por su nodo ({@code nodo.id}), lo que vuelve ejecutable este caso.
     */
    @Test
    @DisplayName("CP-002 — Consultar la información de inventario asociada al CD (HU-39)")
    void cpHu12002_consultarInventarioAsociado() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String cdId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Itagüí, Antioquia"}
                        """.formatted(unico("CD HU12-002")))
                .when().post("/centros-distribucion")
                .then().statusCode(201)
                .extract().path("id");
        String nodoCd = given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/centros-distribucion/" + cdId)
                .then().statusCode(200)
                .extract().path("nodo.id");
        Variante variante = crearVariante(tokenAdmin, "HU12-002");
        cargar(tokenAdmin, nodoCd, variante.sku(), 40);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/nodos/" + nodoCd + "/inventario")
                .then().statusCode(200)
                .body("content.find { it.varianteId == '" + variante.id() + "' }.aLaMano", equalTo(40));
    }
}
