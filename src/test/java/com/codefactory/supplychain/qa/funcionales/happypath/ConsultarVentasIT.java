package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;

/**
 * HU-42 (Sprint 2). Historial con filtros tiendaId, varianteId, transaccionId, estado, desde y
 * hasta. Una venta de otra tienda responde 404; filtrar por una tienda ajena devuelve una página
 * vacía.
 */
@Tag("funcional")
@Tag("happy-path")
class ConsultarVentasIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-001 — Consultar ventas filtradas por tienda y por variante")
    void cpHu42001_filtrarPorTiendaYVariante() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU42-001");
        Variante a = crearVariante(tokenAdmin, "HU42-001A");
        Variante b = crearVariante(tokenAdmin, "HU42-001B");
        cargar(tokenAdmin, tienda.almacenId(), a.sku(), 10);
        cargar(tokenAdmin, tienda.almacenId(), b.sku(), 10);
        vender(tokenAdmin, tienda.id(), a.sku(), 1);
        vender(tokenAdmin, tienda.id(), b.sku(), 1);

        given().header("Authorization", bearer(tokenAdmin))
                .queryParam("tiendaId", tienda.id())
                .when().get("/ventas")
                .then().statusCode(200)
                .body("content", hasSize(2))
                .body("content.tiendaId", everyItem(equalTo(tienda.id())));

        given().header("Authorization", bearer(tokenAdmin))
                .queryParam("tiendaId", tienda.id())
                .queryParam("varianteId", a.id())
                .when().get("/ventas")
                .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].lineas[0].varianteId", equalTo(a.id()));
    }

    @Test
    @DisplayName("CP-002 — Consultar una venta por id")
    void cpHu42002_consultarPorId() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU42-002");
        Variante variante = crearVariante(tokenAdmin, "HU42-002");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 10);
        String lineaId = vender(tokenAdmin, tienda.id(), variante.sku(), 2).path("lineas[0].id");

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/ventas/" + lineaId)
                .then().statusCode(200)
                .body("lineas[0].id", equalTo(lineaId))
                .body("lineas[0].cantidad", equalTo(2));
    }

    @Test
    @DisplayName("CP-006 — Filtrar por estado ANULADA y por rango de fechas UTC")
    void cpHu42006_filtrarPorEstadoYFechas() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU42-006");
        Variante variante = crearVariante(tokenAdmin, "HU42-006");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 10);
        String anulada = vender(tokenAdmin, tienda.id(), variante.sku(), 1).path("lineas[0].id");
        vender(tokenAdmin, tienda.id(), variante.sku(), 1);
        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("{\"motivo\": \"Prueba de filtro\"}")
                .when().post("/ventas/" + anulada + "/anular")
                .then().statusCode(200);

        given().header("Authorization", bearer(tokenAdmin))
                .queryParam("tiendaId", tienda.id())
                .queryParam("estado", "ANULADA")
                .when().get("/ventas")
                .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].lineas[0].id", equalTo(anulada));

        String futuro = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        given().header("Authorization", bearer(tokenAdmin))
                .queryParam("tiendaId", tienda.id())
                .queryParam("desde", futuro)
                .when().get("/ventas")
                .then().statusCode(200)
                .body("content", hasSize(0));
    }

    @Test
    @DisplayName("CP-007 — Consultar por transaccionId devuelve la transacción completa")
    void cpHu42007_filtrarPorTransaccion() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU42-007");
        Variante a = crearVariante(tokenAdmin, "HU42-007A");
        Variante b = crearVariante(tokenAdmin, "HU42-007B");
        cargar(tokenAdmin, tienda.almacenId(), a.sku(), 5);
        cargar(tokenAdmin, tienda.almacenId(), b.sku(), 5);
        String transaccionId = java.util.UUID.randomUUID().toString();
        registrarVenta(tokenAdmin, cuerpoVenta(transaccionId, tienda.id(), false, linea(a.sku(), 1), linea(b.sku(), 1)))
                .then().statusCode(201);

        given().header("Authorization", bearer(tokenAdmin))
                .queryParam("transaccionId", transaccionId)
                .when().get("/ventas")
                .then().statusCode(200)
                .body("content.transaccionId", everyItem(equalTo(transaccionId)))
                .body("content.lineas.flatten()", hasSize(2));
    }
}
