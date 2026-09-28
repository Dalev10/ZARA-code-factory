package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * HU-02 — Iniciar Sesión. Casos CP-HU02-001 a CP-HU02-004.
 *
 * <p>Fixture compartida: un usuario cuyo único rol tiene el scope
 * "cd:administrar" (y ningún otro) — sirve tanto para el caso positivo
 * (AC-4: accede a una función permitida) como para el negativo (AC-4:
 * no accede a una función NO permitida, CP-HU02-004). Se crea de forma
 * perezosa en el primer test que la necesita (no se puede usar
 * {@code @BeforeAll} porque los helpers de fixtures son de instancia y
 * dependen de RestAssured ya configurado por {@code @BeforeEach}).
 */
class Hu02IniciarSesionIT extends AbstractCasoPruebaIT {

    private static String email;
    private static final String PASSWORD = "ClaveSegura2026!";

    private void asegurarFixture(String tokenAdmin) {
        if (email == null) {
            String[] datos = crearUsuarioConUnScope(tokenAdmin, "hu02", "cd:administrar", PASSWORD);
            email = datos[0];
        }
    }

    @Test
    @DisplayName("CP-HU02-001 — Iniciar sesión con credenciales válidas y acceder a funciones permitidas")
    void cpHu02001_iniciarSesionValidaYAccederFuncionPermitida() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        String token = iniciarSesion(email, PASSWORD);

        given().header("Authorization", "Bearer " + token)
                .when().get("/centros-distribucion")
                .then().statusCode(200);
    }

    @Test
    @DisplayName("CP-HU02-002 — Rechazar inicio de sesión con contraseña incorrecta")
    void cpHu02002_rechazarPasswordIncorrecta() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        given().contentType("application/json")
                .body("""
                        {"email": "%s", "password": "ClaveIncorrecta999!"}
                        """.formatted(email))
                .when().post("/auth/login")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("CP-HU02-003 — Rechazar inicio de sesión de un usuario no registrado")
    void cpHu02003_rechazarUsuarioNoRegistrado() {
        given().contentType("application/json")
                .body("""
                        {"email": "%s", "password": "ClaveCualquiera2026!"}
                        """.formatted(unico("hu02-003-inexistente") + "@supplychain-test.local"))
                .when().post("/auth/login")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("CP-HU02-004 — Impedir el acceso a una función no permitida para el rol del usuario")
    void cpHu02004_impedirAccesoFuncionNoPermitida() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);
        String token = iniciarSesion(email, PASSWORD);

        // El rol de este usuario solo tiene "cd:administrar"; "tiendas:administrar" le
        // debe quedar prohibido.
        given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {"nombre": "Tienda No Autorizada QA", "ubicacion": "No debería crearse"}
                        """)
                .when().post("/tiendas")
                .then().statusCode(403);
    }
}
