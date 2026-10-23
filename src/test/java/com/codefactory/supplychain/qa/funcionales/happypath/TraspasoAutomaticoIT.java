package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import java.util.List;
import java.util.Map;
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
@Tag("happy-path")
class TraspasoAutomaticoIT extends AbstractCasoPruebaSprint2IT {

    private static long automaticos(List<Map<String, Object>> traspasos) {
        return traspasos.stream().filter(t -> "AUTOMATICO".equals(t.get("origen"))).count();
    }

    /** Escenario de referencia de la guía, sección 4. */
    @Test
    @DisplayName("CP-001 — Una venta deja el Almacén bajo el mínimo y se repone en la misma respuesta")
    void cpHu48001_escenarioDeReferenciaTiempoReal() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU48-001");
        Variante variante = crearVariante(tokenAdmin, "HU48-001");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 3);
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 10);
        crearUmbralVariante(tokenAdmin, variante.id(), 3, 6);
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu48-001", "VENDEDOR");
        asignarTienda(tokenAdmin, vendedor.id(), tienda.id());

        vender(vendedor.token(), tienda.id(), variante.sku(), 1);

        assertEquals(6, stock(tokenAdmin, tienda.almacenId(), variante.id()), "Almacén");
        assertEquals(6, stock(tokenAdmin, tienda.bodegaId(), variante.id()), "Bodega");
        List<Map<String, Object>> traspasos = traspasos(tokenAdmin, tienda.id());
        assertEquals(1, traspasos.size());
        assertEquals("AUTOMATICO", traspasos.get(0).get("origen"));
        assertEquals(4, traspasos.get(0).get("cantidad"));
        assertEquals(variante.id(), traspasos.get(0).get("varianteId"));
    }

    @Test
    @DisplayName("CP-006 — Sin umbral de variante aplica el de su categoría")
    void cpHu48006_aplicaUmbralDeCategoria() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU48-006");
        Variante variante = crearVariante(tokenAdmin, "HU48-006");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 3);
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 30);
        crearUmbralCategoria(tokenAdmin, variante.categoriaId(), 3, 10);

        vender(tokenAdmin, tienda.id(), variante.sku(), 1);

        assertEquals(10, stock(tokenAdmin, tienda.almacenId(), variante.id()));
        assertEquals(22, stock(tokenAdmin, tienda.bodegaId(), variante.id()));
    }

    @Test
    @DisplayName("CP-007 — Un aumento de la Bodega con el Almacén bajo el mínimo dispara el traspaso")
    void cpHu48007_aumentoDeBodegaDisparaTraspaso() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU48-007");
        Variante variante = crearVariante(tokenAdmin, "HU48-007");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 1);
        crearUmbralVariante(tokenAdmin, variante.id(), 3, 6);

        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 10);

        assertEquals(6, stock(tokenAdmin, tienda.almacenId(), variante.id()));
        assertEquals(5, stock(tokenAdmin, tienda.bodegaId(), variante.id()));
        assertEquals(1, automaticos(traspasos(tokenAdmin, tienda.id())));
    }

    @Test
    @DisplayName("CP-008 — Un ajuste positivo de la Bodega también dispara el traspaso")
    void cpHu48008_ajusteDeBodegaDisparaTraspaso() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU48-008");
        Variante variante = crearVariante(tokenAdmin, "HU48-008");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 0);
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 0);
        crearUmbralVariante(tokenAdmin, variante.id(), 2, 5);

        ajustar(tokenAdmin, tienda.bodegaId(), variante.sku(), 8, "INGRESO_MANUAL", null).then().statusCode(201);

        assertEquals(5, stock(tokenAdmin, tienda.almacenId(), variante.id()));
        assertEquals(3, stock(tokenAdmin, tienda.bodegaId(), variante.id()));
    }

    @Test
    @DisplayName("CP-010 — El traspaso automático que repone el Almacén cierra la ventana de quiebre")
    void cpHu48010_traspasoCierraVentana() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU48-010");
        Variante variante = crearVariante(tokenAdmin, "HU48-010");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 1);
        crearUmbralVariante(tokenAdmin, variante.id(), 1, 4);
        vender(tokenAdmin, tienda.id(), variante.sku(), 1);
        assertEquals("ABIERTA", ventanas(tokenAdmin, tienda.id(), variante.id()).get(0).get("estado"));

        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 10);

        assertEquals(4, stock(tokenAdmin, tienda.almacenId(), variante.id()));
        Map<String, Object> ventana = ventanas(tokenAdmin, tienda.id(), variante.id()).get(0);
        assertEquals("CERRADA", ventana.get("estado"));
        assertEquals("REPOSICION", ventana.get("motivoCierre"));
    }
}
