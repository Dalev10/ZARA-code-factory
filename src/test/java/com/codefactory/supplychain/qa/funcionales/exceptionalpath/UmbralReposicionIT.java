package com.codefactory.supplychain.qa.funcionales.exceptionalpath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-47 (Sprint 2). Umbral por variante o por categoría, nunca ambas ni ninguna. Mínimo > 0 y
 * objetivo > mínimo. Uno por alcance (409 si se repite). Crear o cambiar un umbral no repone por
 * sí solo.
 */
@Tag("funcional")
@Tag("exceptional-path")
class UmbralReposicionIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-007 — Crear un umbral no repone por sí solo")
    void cpHu47007_crearUmbralNoRepone() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU47-007");
        Variante variante = crearVariante(tokenAdmin, "HU47-007");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 1);
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 20);

        crearUmbralVariante(tokenAdmin, variante.id(), 3, 6);

        assertEquals(1, stock(tokenAdmin, tienda.almacenId(), variante.id()));
        assertEquals(20, stock(tokenAdmin, tienda.bodegaId(), variante.id()));
        assertEquals(0, traspasos(tokenAdmin, tienda.id()).size());
    }
}
