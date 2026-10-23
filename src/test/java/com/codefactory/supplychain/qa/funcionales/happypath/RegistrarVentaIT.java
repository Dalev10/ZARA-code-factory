package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-40 (Sprint 2). Venta por lote, todo o nada. El vendedor y la hora los pone el servidor. Sin
 * stock responde 409, salvo forzarSinStock, que puede dejar el Almacén en negativo y marca la
 * venta. Reintentar con el mismo transaccionId y los mismos datos devuelve 200 con la venta
 * original; con otros datos, 409.
 */
@Tag("funcional")
@Tag("happy-path")
class RegistrarVentaIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-001 — Registrar una venta válida descuenta el Almacén y la firma el servidor")
    void cpHu40001_registrarVentaValida() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU40-001");
        Variante variante = crearVariante(tokenAdmin, "HU40-001");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 10);
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu40-001", "VENDEDOR");
        asignarTienda(tokenAdmin, vendedor.id(), tienda.id());
        String transaccionId = UUID.randomUUID().toString();

        registrarVenta(vendedor.token(), cuerpoVenta(transaccionId, tienda.id(), false, linea(variante.sku(), 3)))
                .then().statusCode(201)
                .body("transaccionId", equalTo(transaccionId))
                .body("vendedorId", equalTo(vendedor.id()))
                .body("fecha", endsWith("Z"))
                .body("total", equalTo(269700.0f))
                .body("lineas", hasSize(1))
                .body("lineas[0].estado", equalTo("REGISTRADA"))
                .body("lineas[0].forzada", equalTo(false));

        assertEquals(7, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }
}
