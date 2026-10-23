package com.codefactory.supplychain.qa.funcionales.exceptionalpath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-48 (Sprint 2). El traspaso automático Bodega → Almacén se dispara cuando el Almacén queda por
 * debajo del mínimo o cuando aumenta la Bodega, y lleva el Almacén al objetivo:
 * MIN(objetivo − MAX(almacén, 0), bodega). Si la Bodega no alcanza, el traspaso es parcial. El
 * efecto ya se ve en la misma respuesta ("tiempo real").
 */
@Tag("funcional")
@Tag("exceptional-path")
class TraspasoAutomaticoIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-002 — Quedar exactamente en el mínimo no dispara traspaso (límite)")
    void cpHu48002_quedarEnElMinimoNoRepone() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU48-002");
        Variante variante = crearVariante(tokenAdmin, "HU48-002");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 4);
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 10);
        crearUmbralVariante(tokenAdmin, variante.id(), 3, 6);

        vender(tokenAdmin, tienda.id(), variante.sku(), 1);

        assertEquals(3, stock(tokenAdmin, tienda.almacenId(), variante.id()));
        assertEquals(10, stock(tokenAdmin, tienda.bodegaId(), variante.id()));
        assertEquals(0, traspasos(tokenAdmin, tienda.id()).size());
    }

    @Test
    @DisplayName("CP-003 — Si la Bodega no alcanza, el traspaso es parcial y la deja en 0")
    void cpHu48003_traspasoParcial() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU48-003");
        Variante variante = crearVariante(tokenAdmin, "HU48-003");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 3);
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 2);
        crearUmbralVariante(tokenAdmin, variante.id(), 3, 6);

        vender(tokenAdmin, tienda.id(), variante.sku(), 1);

        assertEquals(4, stock(tokenAdmin, tienda.almacenId(), variante.id()));
        assertEquals(0, stock(tokenAdmin, tienda.bodegaId(), variante.id()));
        assertEquals(2, traspasos(tokenAdmin, tienda.id()).get(0).get("cantidad"));
    }

    @Test
    @DisplayName("CP-004 — Con Almacén negativo la reposición usa MAX(almacén, 0)")
    void cpHu48004_almacenNegativoSeTrataComoCero() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU48-004");
        Variante variante = crearVariante(tokenAdmin, "HU48-004");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 1);
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 20);
        crearUmbralVariante(tokenAdmin, variante.id(), 3, 6);

        venderForzado(tokenAdmin, tienda.id(), variante.sku(), 3);

        // Almacén queda en -2; traspaso = MIN(6 - MAX(-2, 0), 20) = 6 → Almacén = 4
        assertEquals(4, stock(tokenAdmin, tienda.almacenId(), variante.id()));
        assertEquals(14, stock(tokenAdmin, tienda.bodegaId(), variante.id()));
        assertEquals(6, traspasos(tokenAdmin, tienda.id()).get(0).get("cantidad"));
    }

    @Test
    @DisplayName("CP-005 — El umbral de la variante gana sobre el de su categoría")
    void cpHu48005_umbralDeVarianteGanaSobreCategoria() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU48-005");
        Variante variante = crearVariante(tokenAdmin, "HU48-005");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 3);
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 30);
        crearUmbralCategoria(tokenAdmin, variante.categoriaId(), 3, 12);
        crearUmbralVariante(tokenAdmin, variante.id(), 3, 6);

        vender(tokenAdmin, tienda.id(), variante.sku(), 1);

        assertEquals(6, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-009 — Sin umbral configurado una venta no dispara traspaso")
    void cpHu48009_sinUmbralNoHayTraspaso() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU48-009");
        Variante variante = crearVariante(tokenAdmin, "HU48-009");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 2);
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 10);

        vender(tokenAdmin, tienda.id(), variante.sku(), 2);

        assertEquals(0, stock(tokenAdmin, tienda.almacenId(), variante.id()));
        assertEquals(10, stock(tokenAdmin, tienda.bodegaId(), variante.id()));
        assertEquals(0, traspasos(tokenAdmin, tienda.id()).size());
    }
}
