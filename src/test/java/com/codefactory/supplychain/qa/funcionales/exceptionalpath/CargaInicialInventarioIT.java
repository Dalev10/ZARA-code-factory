package com.codefactory.supplychain.qa.funcionales.exceptionalpath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

/**
 * HU-37 (Sprint 2). Carga inicial por lote de 1 a 500 ítems, todo o nada. Cantidad ≥ 0 (un 0
 * cuenta como cargado). SKU inexistente o repetido rechaza el lote; una variante que ya tiene
 * fila responde 409.
 */
@Tag("funcional")
@Tag("exceptional-path")
class CargaInicialInventarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-002 — Una cantidad en 0 cuenta como cargada y crea la fila")
    void cpHu37002_cantidadCeroCuentaComoCargada() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU37-002");
        Variante variante = crearVariante(tokenAdmin, "HU37-002");

        cargaInicial(tokenAdmin, tienda.almacenId(), item(variante.sku(), 0))
                .then().statusCode(201)
                .body("filasCreadas", equalTo(1));

        org.junit.jupiter.api.Assertions.assertTrue(tieneFila(tokenAdmin, tienda.almacenId(), variante.id()));
        cargaInicial(tokenAdmin, tienda.almacenId(), item(variante.sku(), 5)).then().statusCode(409);
    }
}
