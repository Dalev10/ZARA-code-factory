package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * HU-37 (Sprint 2). Carga inicial por lote de 1 a 500 ítems, todo o nada. Cantidad ≥ 0 (un 0
 * cuenta como cargado). SKU inexistente o repetido rechaza el lote; una variante que ya tiene
 * fila responde 409.
 */
@Tag("funcional")
@Tag("negative-path")
class CargaInicialInventarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-003 — Un SKU inexistente rechaza todo el lote (400) y no carga nada")
    void cpHu37003_skuInexistenteRechazaElLote() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU37-003");
        Variante valida = crearVariante(tokenAdmin, "HU37-003");

        cargaInicial(tokenAdmin, tienda.almacenId(), item(valida.sku(), 10) + "," + item("SKU-NO-EXISTE-HU37", 3))
                .then().statusCode(400)
                .body("mensaje", notNullValue());

        assertFalse(tieneFila(tokenAdmin, tienda.almacenId(), valida.id()), "el lote no es todo o nada");
    }

    @Test
    @DisplayName("CP-004 — Un SKU repetido en el lote lo rechaza (400)")
    void cpHu37004_skuRepetidoRechazaElLote() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU37-004");
        Variante variante = crearVariante(tokenAdmin, "HU37-004");

        cargaInicial(tokenAdmin, tienda.almacenId(), item(variante.sku(), 2) + "," + item(variante.sku(), 3))
                .then().statusCode(400)
                .body("mensaje", notNullValue());

        assertFalse(tieneFila(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-005 — Una variante que ya tiene fila en el nodo responde 409")
    void cpHu37005_varianteConFilaResponde409() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU37-005");
        Variante variante = crearVariante(tokenAdmin, "HU37-005");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 4);

        cargaInicial(tokenAdmin, tienda.almacenId(), item(variante.sku(), 9))
                .then().statusCode(409)
                .body("mensaje", notNullValue());

        assertEquals(4, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-006 — Rechazar una cantidad negativa (400)")
    void cpHu37006_rechazarCantidadNegativa() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU37-006");
        Variante variante = crearVariante(tokenAdmin, "HU37-006");

        cargaInicial(tokenAdmin, tienda.almacenId(), item(variante.sku(), -1))
                .then().statusCode(400)
                .body("mensaje", notNullValue());
    }

    @Test
    @DisplayName("CP-007 — Rechazar un lote vacío y uno de más de 500 ítems (400)")
    void cpHu37007_rechazarLoteVacioOExcedido() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU37-007");

        cargaInicial(tokenAdmin, tienda.almacenId(), "").then().statusCode(400);

        List<String> items = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            items.add(item("SKU-HU37-007-" + i, 1));
        }
        cargaInicial(tokenAdmin, tienda.almacenId(), String.join(",", items))
                .then().statusCode(400)
                .body("mensaje", notNullValue());
    }

    @Test
    @DisplayName("CP-011 — Cargar en un nodo inexistente responde 404")
    void cpHu37011_nodoInexistente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Variante variante = crearVariante(tokenAdmin, "HU37-011");

        cargaInicial(tokenAdmin, UUID.randomUUID().toString(), item(variante.sku(), 3)).then().statusCode(404);
    }
}
