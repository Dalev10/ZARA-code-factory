package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-38 (Sprint 2). Ajuste {"sku","delta","motivo","comentario"}: CORRECCION exige comentario,
 * nunca deja negativo (400) y un ajuste positivo sobre una variante sin fila la crea.
 */
@Tag("funcional")
@Tag("happy-path")
class AjusteInventarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-001 — Ajuste positivo por conteo físico suma al stock y devuelve el saldo")
    void cpHu38001_ajustePositivo() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU38-001");
        Variante variante = crearVariante(tokenAdmin, "HU38-001");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 10);

        ajustar(tokenAdmin, tienda.almacenId(), variante.sku(), 3, "CONTEO_FISICO", null)
                .then().statusCode(201)
                .body("delta", equalTo(3))
                .body("motivo", equalTo("CONTEO_FISICO"))
                .body("aLaMano", equalTo(13));

        assertEquals(13, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-002 — Ajuste negativo por merma resta del stock")
    void cpHu38002_ajusteNegativoPorMerma() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU38-002");
        Variante variante = crearVariante(tokenAdmin, "HU38-002");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 10);

        ajustar(tokenAdmin, tienda.almacenId(), variante.sku(), -4, "MERMA", "Prendas dañadas")
                .then().statusCode(201)
                .body("aLaMano", equalTo(6));
    }
}
