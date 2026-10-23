package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * HU-38 (Sprint 2). Ajuste {"sku","delta","motivo","comentario"}: CORRECCION exige comentario,
 * nunca deja negativo (400) y un ajuste positivo sobre una variante sin fila la crea.
 */
@Tag("funcional")
@Tag("negative-path")
class AjusteInventarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-003 — CORRECCION sin comentario se rechaza (400)")
    void cpHu38003_correccionExigeComentario() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU38-003");
        Variante variante = crearVariante(tokenAdmin, "HU38-003");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 10);

        ajustar(tokenAdmin, tienda.almacenId(), variante.sku(), -1, "CORRECCION", null)
                .then().statusCode(400)
                .body("mensaje", notNullValue());
        ajustar(tokenAdmin, tienda.almacenId(), variante.sku(), -1, "CORRECCION", "   ")
                .then().statusCode(400);

        assertEquals(10, stock(tokenAdmin, tienda.almacenId(), variante.id()));
        ajustar(tokenAdmin, tienda.almacenId(), variante.sku(), -1, "CORRECCION", "Error de digitación")
                .then().statusCode(201);
    }

    @Test
    @DisplayName("CP-004 — Un ajuste que dejaría el stock negativo se rechaza (400) sin cambiar el saldo")
    void cpHu38004_ajusteNoDejaNegativo() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU38-004");
        Variante variante = crearVariante(tokenAdmin, "HU38-004");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 2);

        ajustar(tokenAdmin, tienda.almacenId(), variante.sku(), -3, "MERMA", null)
                .then().statusCode(400)
                .body("mensaje", notNullValue());

        assertEquals(2, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-006 — Un ajuste negativo sobre una variante sin fila se rechaza (400) y no crea fila")
    void cpHu38006_ajusteNegativoSinFila() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU38-006");
        Variante variante = crearVariante(tokenAdmin, "HU38-006");

        ajustar(tokenAdmin, tienda.almacenId(), variante.sku(), -1, "MERMA", null)
                .then().statusCode(400);

        assertFalse(tieneFila(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-007 — Rechazar un motivo fuera de la lista (400)")
    void cpHu38007_rechazarMotivoInvalido() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU38-007");
        Variante variante = crearVariante(tokenAdmin, "HU38-007");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 5);

        ajustar(tokenAdmin, tienda.almacenId(), variante.sku(), 1, "REGALO", null)
                .then().statusCode(400)
                .body("mensaje", notNullValue());
    }

    @Test
    @DisplayName("CP-009 — Ajustar un SKU inexistente se rechaza")
    void cpHu38009_ajusteSkuInexistente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU38-009");

        ajustar(tokenAdmin, tienda.almacenId(), "SKU-NO-EXISTE-HU38", 1, "INGRESO_MANUAL", null)
                .then().statusCode(org.hamcrest.Matchers.anyOf(equalTo(400), equalTo(404)))
                .body("mensaje", notNullValue());
    }
}
