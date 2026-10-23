package com.codefactory.supplychain.qa.funcionales.exceptionalpath;

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
@Tag("exceptional-path")
class AjusteInventarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-005 — Un ajuste positivo sobre una variante sin fila crea la fila")
    void cpHu38005_ajustePositivoCreaFila() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU38-005");
        Variante variante = crearVariante(tokenAdmin, "HU38-005");

        ajustar(tokenAdmin, tienda.almacenId(), variante.sku(), 7, "INGRESO_MANUAL", null)
                .then().statusCode(201)
                .body("aLaMano", equalTo(7));

        assertEquals(7, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }
}
