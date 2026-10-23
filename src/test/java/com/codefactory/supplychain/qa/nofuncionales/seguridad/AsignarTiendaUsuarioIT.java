package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * HU-32 (Sprint 2). Salvo ADMIN, cada usuario solo opera en sus tiendas asignadas. La asignación
 * se resuelve en cada request: no hace falta un token nuevo.
 */
@Tag("no-funcional")
@Tag("seguridad")
class AsignarTiendaUsuarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-002 — Un vendedor sin la tienda asignada no registra ventas en ella (403)")
    void cpHu32002_vendedorSinTiendaAsignadaRecibe403() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU32-002");
        Variante variante = crearVariante(tokenAdmin, "HU32-002");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 5);
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu32-002", "VENDEDOR");

        registrarVenta(vendedor.token(),
                cuerpoVenta(UUID.randomUUID().toString(), tienda.id(), false, linea(variante.sku(), 1)))
                .then().statusCode(403);

        org.junit.jupiter.api.Assertions.assertEquals(5, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-003 — La asignación aplica de inmediato sin pedir un token nuevo")
    void cpHu32003_asignacionAplicaConElMismoToken() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU32-003");
        Variante variante = crearVariante(tokenAdmin, "HU32-003");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 5);
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu32-003", "VENDEDOR");

        asignarTienda(tokenAdmin, vendedor.id(), tienda.id());

        vender(vendedor.token(), tienda.id(), variante.sku(), 1);
    }

    @Test
    @DisplayName("CP-004 — Quitar la tienda corta la operación del usuario en ella")
    void cpHu32004_quitarTiendaCortaLaOperacion() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU32-004");
        Variante variante = crearVariante(tokenAdmin, "HU32-004");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 5);
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu32-004", "VENDEDOR");
        asignarTienda(tokenAdmin, vendedor.id(), tienda.id());

        given().header("Authorization", bearer(tokenAdmin))
                .when().delete("/usuarios/" + vendedor.id() + "/tiendas/" + tienda.id())
                .then().statusCode(204);

        registrarVenta(vendedor.token(),
                cuerpoVenta(UUID.randomUUID().toString(), tienda.id(), false, linea(variante.sku(), 1)))
                .then().statusCode(403);
    }

    @Test
    @DisplayName("CP-005 — ADMIN opera en cualquier tienda sin tenerla asignada (tiendas:operar-todas)")
    void cpHu32005_adminOperaEnTodasLasTiendas() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU32-005");
        Variante variante = crearVariante(tokenAdmin, "HU32-005");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 5);

        vender(tokenAdmin, tienda.id(), variante.sku(), 1);
    }
}
