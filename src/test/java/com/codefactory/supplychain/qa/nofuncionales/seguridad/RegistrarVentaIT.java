package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * HU-40 (Sprint 2). Venta por lote, todo o nada. El vendedor y la hora los pone el servidor. Sin
 * stock responde 409, salvo forzarSinStock, que puede dejar el Almacén en negativo y marca la
 * venta. Reintentar con el mismo transaccionId y los mismos datos devuelve 200 con la venta
 * original; con otros datos, 409.
 */
@Tag("no-funcional")
@Tag("seguridad")
class RegistrarVentaIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-012 — Forzar sin stock exige el scope ventas:forzar-sin-stock (403)")
    void cpHu40012_forzarExigeScope() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU40-012");
        Variante variante = crearVariante(tokenAdmin, "HU40-012");
        Usuario usuario = usuarioConScopes(tokenAdmin, "hu40-012", "ventas:registrar");
        asignarTienda(tokenAdmin, usuario.id(), tienda.id());

        registrarVenta(usuario.token(), cuerpoVenta(UUID.randomUUID().toString(), tienda.id(), true,
                linea(variante.sku(), 1)))
                .then().statusCode(403);
        assertFalse(tieneFila(tokenAdmin, tienda.almacenId(), variante.id()));
    }
}
