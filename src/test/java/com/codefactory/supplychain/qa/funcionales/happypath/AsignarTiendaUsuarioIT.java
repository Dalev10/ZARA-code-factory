package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

/**
 * HU-32 (Sprint 2). Salvo ADMIN, cada usuario solo opera en sus tiendas asignadas. La asignación
 * se resuelve en cada request: no hace falta un token nuevo.
 */
@Tag("funcional")
@Tag("happy-path")
class AsignarTiendaUsuarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-001 — Asignar una tienda a un usuario y consultarla en sus tiendas")
    void cpHu32001_asignarTiendaYListarla() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU32-001");
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu32-001", "VENDEDOR");

        asignarTienda(tokenAdmin, vendedor.id(), tienda.id());

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/usuarios/" + vendedor.id() + "/tiendas")
                .then().statusCode(200)
                .body("id", hasItem(tienda.id()))
                .body("find { it.id == '" + tienda.id() + "' }.activa", equalTo(true));
    }
}
