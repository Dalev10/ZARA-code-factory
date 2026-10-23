package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;

/**
 * HU-39 (Sprint 2). Consultas de stock por nodo y por variante y kardex con filtros
 * tipo/desde/hasta en UTC. Sin acceso global solo se ven los nodos de las tiendas asignadas (un
 * nodo ajeno responde 404); un CD lo ven ADMIN y OPERADOR_CD.
 */
@Tag("funcional")
@Tag("happy-path")
class ConsultarInventarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-001 — Consultar el stock de un nodo")
    void cpHu39001_consultarStockDeNodo() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU39-001");
        Variante variante = crearVariante(tokenAdmin, "HU39-001");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 9);

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/nodos/" + tienda.almacenId() + "/inventario")
                .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].varianteId", equalTo(variante.id()))
                .body("content[0].aLaMano", equalTo(9));
    }

    @Test
    @DisplayName("CP-002 — Consultar el stock de una variante en todos sus nodos")
    void cpHu39002_consultarStockDeVariante() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU39-002");
        Variante variante = crearVariante(tokenAdmin, "HU39-002");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 4);
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 11);

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/variantes/" + variante.id() + "/inventario")
                .then().statusCode(200)
                .body("$", hasSize(2))
                .body("find { it.nodoId == '" + tienda.almacenId() + "' }.aLaMano", equalTo(4))
                .body("find { it.nodoId == '" + tienda.bodegaId() + "' }.aLaMano", equalTo(11));
    }

    @Test
    @DisplayName("CP-003 — El kardex muestra cada movimiento en orden, con fechas en UTC")
    void cpHu39003_kardexEnOrden() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU39-003");
        Variante variante = crearVariante(tokenAdmin, "HU39-003");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 10);
        ajustar(tokenAdmin, tienda.almacenId(), variante.sku(), -2, "MERMA", null).then().statusCode(201);
        vender(tokenAdmin, tienda.id(), variante.sku(), 1);

        List<String> tipos = given().header("Authorization", bearer(tokenAdmin))
                .when().get("/nodos/" + tienda.almacenId() + "/inventario/movimientos")
                .then().statusCode(200)
                .body("content.ocurridoEn", everyItem(endsWith("Z")))
                .extract().path("content.tipo");

        org.hamcrest.MatcherAssert.assertThat(tipos, contains("CARGA_INICIAL", "AJUSTE", "VENTA"));
    }

    @Test
    @DisplayName("CP-004 — Filtrar el kardex por tipo y por rango de fechas UTC")
    void cpHu39004_filtrarKardex() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU39-004");
        Variante variante = crearVariante(tokenAdmin, "HU39-004");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 10);
        ajustar(tokenAdmin, tienda.almacenId(), variante.sku(), 1, "CONTEO_FISICO", null).then().statusCode(201);

        given().header("Authorization", bearer(tokenAdmin))
                .queryParam("tipo", "AJUSTE")
                .when().get("/nodos/" + tienda.almacenId() + "/inventario/movimientos")
                .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].tipo", equalTo("AJUSTE"))
                .body("content[0].motivo", equalTo("CONTEO_FISICO"));

        String futuro = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        given().header("Authorization", bearer(tokenAdmin))
                .queryParam("desde", futuro)
                .when().get("/nodos/" + tienda.almacenId() + "/inventario/movimientos")
                .then().statusCode(200)
                .body("content", hasSize(0));

        String pasado = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        given().header("Authorization", bearer(tokenAdmin))
                .queryParam("desde", pasado)
                .queryParam("hasta", futuro)
                .when().get("/nodos/" + tienda.almacenId() + "/inventario/movimientos")
                .then().statusCode(200)
                .body("content", hasSize(2));
    }
}
