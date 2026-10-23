package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-50 (Sprint 2). La evaluación masiva revisa todas las tiendas activas, o una, y ejecuta los
 * traspasos que correspondan. Sirve para reponer después de subir un mínimo.
 */
@Tag("funcional")
@Tag("happy-path")
class EvaluacionMasivaIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-001 — Evaluar una tienda tras subir el mínimo ejecuta el traspaso")
    void cpHu50001_evaluarTiendaTrasSubirMinimo() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU50-001");
        Variante variante = crearVariante(tokenAdmin, "HU50-001");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 4);
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 20);
        String umbralId = crearUmbralVariante(tokenAdmin, variante.id(), 2, 5);
        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"cantidadMinima": 6, "cantidadObjetivo": 10}
                        """)
                .when().put("/umbrales-reposicion/" + umbralId)
                .then().statusCode(200);
        assertEquals(4, stock(tokenAdmin, tienda.almacenId(), variante.id()), "cambiar el umbral no repone");

        given().header("Authorization", bearer(tokenAdmin))
                .when().post("/reposicion-interna/tiendas/" + tienda.id() + "/evaluar")
                .then().statusCode(200)
                .body("traspasoEjecutados", equalTo(1))
                .body("traspasos[0].origen", equalTo("AUTOMATICO"))
                .body("traspasos[0].cantidad", equalTo(6));

        assertEquals(10, stock(tokenAdmin, tienda.almacenId(), variante.id()));
        assertEquals(14, stock(tokenAdmin, tienda.bodegaId(), variante.id()));
    }

    @Test
    @DisplayName("CP-003 — Evaluar todas las tiendas activas repone cada una")
    void cpHu50003_evaluarTodasLasTiendas() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda t1 = crearTienda(tokenAdmin, "Tienda HU50-003A");
        Tienda t2 = crearTienda(tokenAdmin, "Tienda HU50-003B");
        Variante variante = crearVariante(tokenAdmin, "HU50-003");
        for (Tienda t : new Tienda[] {t1, t2}) {
            cargar(tokenAdmin, t.almacenId(), variante.sku(), 1);
            cargar(tokenAdmin, t.bodegaId(), variante.sku(), 10);
        }
        crearUmbralVariante(tokenAdmin, variante.id(), 3, 5);

        given().header("Authorization", bearer(tokenAdmin))
                .when().post("/reposicion-interna/evaluar")
                .then().statusCode(200)
                .body("traspasoEjecutados", greaterThanOrEqualTo(2));

        assertEquals(5, stock(tokenAdmin, t1.almacenId(), variante.id()));
        assertEquals(5, stock(tokenAdmin, t2.almacenId(), variante.id()));
    }
}
