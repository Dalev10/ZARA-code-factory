package com.codefactory.supplychain.qa.funcionales.exceptionalpath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * HU-40 (Sprint 2). Venta por lote, todo o nada. El vendedor y la hora los pone el servidor. Sin
 * stock responde 409, salvo forzarSinStock, que puede dejar el Almacén en negativo y marca la
 * venta. Reintentar con el mismo transaccionId y los mismos datos devuelve 200 con la venta
 * original; con otros datos, 409.
 */
@Tag("funcional")
@Tag("exceptional-path")
class RegistrarVentaIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-004 — Una venta forzada sin stock deja el Almacén en negativo y queda marcada")
    void cpHu40004_ventaForzadaSinStock() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU40-004");
        Variante variante = crearVariante(tokenAdmin, "HU40-004");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 1);
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu40-004", "VENDEDOR");
        asignarTienda(tokenAdmin, vendedor.id(), tienda.id());

        venderForzado(vendedor.token(), tienda.id(), variante.sku(), 3)
                .then().body("lineas[0].forzada", equalTo(true));

        assertEquals(-2, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-005 — Reintentar con el mismo transaccionId y los mismos datos devuelve 200 sin duplicar")
    void cpHu40005_reintentoIdempotente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU40-005");
        Variante variante = crearVariante(tokenAdmin, "HU40-005");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 10);
        String cuerpo = cuerpoVenta(UUID.randomUUID().toString(), tienda.id(), false, linea(variante.sku(), 2));

        String lineaOriginal = registrarVenta(tokenAdmin, cuerpo).then().statusCode(201)
                .extract().path("lineas[0].id");
        registrarVenta(tokenAdmin, cuerpo).then().statusCode(200)
                .body("lineas[0].id", equalTo(lineaOriginal));

        assertEquals(8, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-009 — canal y forzarSinStock son opcionales")
    void cpHu40009_camposOpcionales() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU40-009");
        Variante variante = crearVariante(tokenAdmin, "HU40-009");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 5);

        registrarVenta(tokenAdmin, """
                {"transaccionId": "%s", "tiendaId": "%s", "items": [%s]}
                """.formatted(UUID.randomUUID(), tienda.id(), linea(variante.sku(), 1)))
                .then().statusCode(201)
                .body("lineas[0].forzada", equalTo(false));
    }

    @Test
    @DisplayName("CP-010 — Sin fila la variante tiene stock 0: venta normal 409, forzada crea la fila en negativo")
    void cpHu40010_filasPorDemanda() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU40-010");
        Variante variante = crearVariante(tokenAdmin, "HU40-010");

        registrarVenta(tokenAdmin, cuerpoVenta(UUID.randomUUID().toString(), tienda.id(), false,
                linea(variante.sku(), 1)))
                .then().statusCode(409);
        assertFalse(tieneFila(tokenAdmin, tienda.almacenId(), variante.id()));

        venderForzado(tokenAdmin, tienda.id(), variante.sku(), 2);
        assertEquals(-2, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-011 — Si una venta forzada ya creó la fila, la carga inicial responde 409 y el ajuste sí aplica")
    void cpHu40011_cargaInicialTrasVentaForzada() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU40-011");
        Variante variante = crearVariante(tokenAdmin, "HU40-011");
        venderForzado(tokenAdmin, tienda.id(), variante.sku(), 1);

        cargaInicial(tokenAdmin, tienda.almacenId(), item(variante.sku(), 10)).then().statusCode(409);
        ajustar(tokenAdmin, tienda.almacenId(), variante.sku(), 10, "INGRESO_MANUAL", null)
                .then().statusCode(201)
                .body("aLaMano", equalTo(9));
    }
}
