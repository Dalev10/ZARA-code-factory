package com.codefactory.supplychain.qa.funcionales.negativepath;

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
@Tag("funcional")
@Tag("negative-path")
class AsignarTiendaUsuarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-006 — Rechazar asignar una tienda inexistente")
    void cpHu32006_rechazarTiendaInexistente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu32-006", "VENDEDOR");

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(UUID.randomUUID()))
                .when().post("/usuarios/" + vendedor.id() + "/tiendas")
                .then().statusCode(404);
    }
}
