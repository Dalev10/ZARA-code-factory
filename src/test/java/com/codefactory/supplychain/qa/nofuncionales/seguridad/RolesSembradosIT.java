package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

/**
 * HU-33 (Sprint 2). Los roles VENDEDOR, SUPERVISOR_TIENDA y OPERADOR_CD vienen sembrados con los
 * scopes de la guía; sus nombres son reservados. El campo {@code sensible} de un scope es opcional.
 */
@Tag("no-funcional")
@Tag("seguridad")
class RolesSembradosIT extends AbstractCasoPruebaSprint2IT {

    private java.util.List<String> scopesDeRol(String tokenAdmin, String rol) {
        return given().header("Authorization", bearer(tokenAdmin))
                .when().get("/roles/" + idDeRol(tokenAdmin, rol) + "/scopes")
                .then().statusCode(200)
                .extract().path("codigo");
    }

    @Test
    @DisplayName("CP-001 — El rol VENDEDOR viene sembrado con sus cuatro scopes")
    void cpHu33001_rolVendedorSembrado() {
        String tokenAdmin = iniciarSesionComoAdmin();

        org.hamcrest.MatcherAssert.assertThat(scopesDeRol(tokenAdmin, "VENDEDOR"), containsInAnyOrder(
                "ventas:registrar", "ventas:consultar", "inventario:consultar", "ventas:forzar-sin-stock"));
    }

    @Test
    @DisplayName("CP-002 — El rol SUPERVISOR_TIENDA tiene lo del vendedor más anular y administrar inventario")
    void cpHu33002_rolSupervisorSembrado() {
        String tokenAdmin = iniciarSesionComoAdmin();

        org.hamcrest.MatcherAssert.assertThat(scopesDeRol(tokenAdmin, "SUPERVISOR_TIENDA"), containsInAnyOrder(
                "ventas:registrar", "ventas:consultar", "inventario:consultar", "ventas:forzar-sin-stock",
                "ventas:anular", "inventario:administrar"));
    }

    @Test
    @DisplayName("CP-003 — El rol OPERADOR_CD solo administra y consulta inventario de CD")
    void cpHu33003_rolOperadorCdSembrado() {
        String tokenAdmin = iniciarSesionComoAdmin();

        org.hamcrest.MatcherAssert.assertThat(scopesDeRol(tokenAdmin, "OPERADOR_CD"), containsInAnyOrder(
                "inventario-cd:administrar", "inventario:consultar"));
    }

    @Test
    @DisplayName("CP-004 — ADMIN incluye tiendas:operar-todas y reposicion:administrar")
    void cpHu33004_rolAdminConScopesDelSprint2() {
        String tokenAdmin = iniciarSesionComoAdmin();

        org.hamcrest.MatcherAssert.assertThat(scopesDeRol(tokenAdmin, "ADMIN"), hasItems(
                "tiendas:operar-todas", "reposicion:administrar", "inventario:administrar",
                "inventario-cd:administrar", "ventas:anular"));
    }

    @Test
    @DisplayName("CP-007 — Un VENDEDOR no puede anular ventas (403, falta ventas:anular)")
    void cpHu33007_vendedorSinScopeDeAnular() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu33-007", "VENDEDOR");

        given().header("Authorization", bearer(vendedor.token()))
                .contentType("application/json")
                .body("""
                        {"motivo": "Intento sin permiso"}
                        """)
                .when().post("/ventas/" + java.util.UUID.randomUUID() + "/anular")
                .then().statusCode(403)
                .body("mensaje", equalTo("No tiene permisos para realizar esta acción"));
    }
}
