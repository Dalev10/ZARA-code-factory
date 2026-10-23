package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * HU-42 (Sprint 2). Historial con filtros tiendaId, varianteId, transaccionId, estado, desde y
 * hasta. Una venta de otra tienda responde 404; filtrar por una tienda ajena devuelve una página
 * vacía.
 */
@Tag("no-funcional")
@Tag("seguridad")
class ConsultarVentasIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-003 — Una venta de otra tienda responde 404 para un vendedor sin acceso")
    void cpHu42003_ventaDeOtraTiendaResponde404() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda propia = crearTienda(tokenAdmin, "Tienda HU42-003A");
        Tienda ajena = crearTienda(tokenAdmin, "Tienda HU42-003B");
        Variante variante = crearVariante(tokenAdmin, "HU42-003");
        cargar(tokenAdmin, ajena.almacenId(), variante.sku(), 5);
        String lineaAjena = vender(tokenAdmin, ajena.id(), variante.sku(), 1).path("lineas[0].id");
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu42-003", "VENDEDOR");
        asignarTienda(tokenAdmin, vendedor.id(), propia.id());

        given().header("Authorization", bearer(vendedor.token()))
                .when().get("/ventas/" + lineaAjena)
                .then().statusCode(404);
    }

    @Test
    @DisplayName("CP-004 — Filtrar por una tienda ajena devuelve una página vacía")
    void cpHu42004_filtroPorTiendaAjenaPaginaVacia() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda propia = crearTienda(tokenAdmin, "Tienda HU42-004A");
        Tienda ajena = crearTienda(tokenAdmin, "Tienda HU42-004B");
        Variante variante = crearVariante(tokenAdmin, "HU42-004");
        cargar(tokenAdmin, ajena.almacenId(), variante.sku(), 5);
        vender(tokenAdmin, ajena.id(), variante.sku(), 1);
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu42-004", "VENDEDOR");
        asignarTienda(tokenAdmin, vendedor.id(), propia.id());

        given().header("Authorization", bearer(vendedor.token()))
                .queryParam("tiendaId", ajena.id())
                .when().get("/ventas")
                .then().statusCode(200)
                .body("content", hasSize(0));
    }

    @Test
    @DisplayName("CP-005 — Sin filtro de tienda, un vendedor solo ve las ventas de sus tiendas")
    void cpHu42005_sinFiltroSoloSusTiendas() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda propia = crearTienda(tokenAdmin, "Tienda HU42-005A");
        Tienda ajena = crearTienda(tokenAdmin, "Tienda HU42-005B");
        Variante variante = crearVariante(tokenAdmin, "HU42-005");
        cargar(tokenAdmin, propia.almacenId(), variante.sku(), 5);
        cargar(tokenAdmin, ajena.almacenId(), variante.sku(), 5);
        vender(tokenAdmin, propia.id(), variante.sku(), 1);
        vender(tokenAdmin, ajena.id(), variante.sku(), 1);
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu42-005", "VENDEDOR");
        asignarTienda(tokenAdmin, vendedor.id(), propia.id());

        given().header("Authorization", bearer(vendedor.token()))
                .queryParam("varianteId", variante.id())
                .when().get("/ventas")
                .then().statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].tiendaId", equalTo(propia.id()));
    }
}
