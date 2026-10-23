package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-40 (Sprint 2). Venta por lote, todo o nada. El vendedor y la hora los pone el servidor. Sin
 * stock responde 409, salvo forzarSinStock, que puede dejar el Almacén en negativo y marca la
 * venta. Reintentar con el mismo transaccionId y los mismos datos devuelve 200 con la venta
 * original; con otros datos, 409.
 */
@Tag("funcional")
@Tag("negative-path")
class RegistrarVentaIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-002 — Una venta sin stock suficiente responde 409 y no descuenta")
    void cpHu40002_ventaSinStockResponde409() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU40-002");
        Variante variante = crearVariante(tokenAdmin, "HU40-002");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 2);

        registrarVenta(tokenAdmin, cuerpoVenta(UUID.randomUUID().toString(), tienda.id(), false,
                linea(variante.sku(), 3)))
                .then().statusCode(409)
                .body("mensaje", notNullValue());

        assertEquals(2, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-003 — Un lote con una línea sin stock no registra ninguna línea (todo o nada)")
    void cpHu40003_loteTodoONada() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU40-003");
        Variante conStock = crearVariante(tokenAdmin, "HU40-003A");
        Variante sinStock = crearVariante(tokenAdmin, "HU40-003B");
        cargar(tokenAdmin, tienda.almacenId(), conStock.sku(), 10);
        cargar(tokenAdmin, tienda.almacenId(), sinStock.sku(), 1);
        String transaccionId = UUID.randomUUID().toString();

        registrarVenta(tokenAdmin, cuerpoVenta(transaccionId, tienda.id(), false,
                linea(conStock.sku(), 2), linea(sinStock.sku(), 5)))
                .then().statusCode(409);

        assertEquals(10, stock(tokenAdmin, tienda.almacenId(), conStock.id()));
        assertEquals(1, stock(tokenAdmin, tienda.almacenId(), sinStock.id()));
        given().header("Authorization", bearer(tokenAdmin))
                .queryParam("transaccionId", transaccionId)
                .when().get("/ventas")
                .then().statusCode(200)
                .body("content", hasSize(0));
    }

    @Test
    @DisplayName("CP-006 — Reutilizar un transaccionId con otros datos responde 409")
    void cpHu40006_transaccionReutilizadaConOtrosDatos() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU40-006");
        Variante variante = crearVariante(tokenAdmin, "HU40-006");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 10);
        String transaccionId = UUID.randomUUID().toString();

        registrarVenta(tokenAdmin, cuerpoVenta(transaccionId, tienda.id(), false, linea(variante.sku(), 2)))
                .then().statusCode(201);
        registrarVenta(tokenAdmin, cuerpoVenta(transaccionId, tienda.id(), false, linea(variante.sku(), 5)))
                .then().statusCode(409)
                .body("mensaje", notNullValue());

        assertEquals(8, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-007 — Un SKU inexistente responde 404")
    void cpHu40007_skuInexistente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU40-007");

        registrarVenta(tokenAdmin, cuerpoVenta(UUID.randomUUID().toString(), tienda.id(), false,
                linea("SKU-NO-EXISTE-HU40", 1)))
                .then().statusCode(404)
                .body("mensaje", notNullValue());
    }

    @Test
    @DisplayName("CP-008 — Rechazar datos inválidos: cantidad 0, precio negativo o sin ítems (400)")
    void cpHu40008_rechazarDatosInvalidos() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU40-008");
        Variante variante = crearVariante(tokenAdmin, "HU40-008");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 10);

        registrarVenta(tokenAdmin, cuerpoVenta(UUID.randomUUID().toString(), tienda.id(), false,
                linea(variante.sku(), 0)))
                .then().statusCode(400).body("mensaje", notNullValue());
        registrarVenta(tokenAdmin, cuerpoVenta(UUID.randomUUID().toString(), tienda.id(), false,
                """
                {"sku": "%s", "cantidad": 1, "precioUnitario": -1}""".formatted(variante.sku())))
                .then().statusCode(400);
        registrarVenta(tokenAdmin, cuerpoVenta(UUID.randomUUID().toString(), tienda.id(), false))
                .then().statusCode(400);

        assertEquals(10, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-013 — No se registran ventas en una tienda desactivada")
    void cpHu40013_tiendaDesactivada() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU40-013");
        Variante variante = crearVariante(tokenAdmin, "HU40-013");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 5);
        given().header("Authorization", bearer(tokenAdmin))
                .when().delete("/tiendas/" + tienda.id())
                .then().statusCode(204);

        Response r = registrarVenta(tokenAdmin, cuerpoVenta(UUID.randomUUID().toString(), tienda.id(), false,
                linea(variante.sku(), 1)));

        org.junit.jupiter.api.Assertions.assertTrue(r.statusCode() >= 400 && r.statusCode() < 500,
                "esperado 4xx, fue " + r.statusCode());
        assertEquals(5, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }
}
