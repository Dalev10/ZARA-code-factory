package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-38 (Sprint 2). Ajuste {"sku","delta","motivo","comentario"}: CORRECCION exige comentario,
 * nunca deja negativo (400) y un ajuste positivo sobre una variante sin fila la crea.
 */
@Tag("no-funcional")
@Tag("seguridad")
class AjusteInventarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-008 — Un VENDEDOR no puede ajustar inventario (403)")
    void cpHu38008_vendedorNoAjusta() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU38-008");
        Variante variante = crearVariante(tokenAdmin, "HU38-008");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 5);
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu38-008", "VENDEDOR");
        asignarTienda(tokenAdmin, vendedor.id(), tienda.id());

        ajustar(vendedor.token(), tienda.almacenId(), variante.sku(), 1, "CONTEO_FISICO", null)
                .then().statusCode(403);
        assertEquals(5, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }
}
