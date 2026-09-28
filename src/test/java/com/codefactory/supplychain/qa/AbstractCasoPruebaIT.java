package com.codefactory.supplychain.qa;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Base común de los 56 casos de prueba de Sprint 1 (HU-01 a HU-18), tal como
 * están documentados en «Casos_de_Prueba_-_Sprint__1_EAP01FE.docx». Cada
 * subclase de este paquete {@code qa} representa una historia de usuario
 * (una clase = una sección del documento) y cada método {@code @Test}
 * representa exactamente un CP-ID de ese documento — el nombre del método y
 * el {@code @DisplayName} citan el identificador tal cual aparece ahí, para
 * que el reporte final sea trazable caso por caso.
 *
 * <p>A diferencia de los *ControllerTest de MockMvc del resto del proyecto
 * (pruebas de integración en el sentido del Plan de Aseguramiento §5.5),
 * estas pruebas usan REST Assured contra un servidor HTTP real
 * (RANDOM_PORT), porque documentan específicamente las «Pruebas
 * funcionales/API» que ese mismo plan describe como una categoría separada.
 *
 * <p>El contenedor de PostgreSQL se levanta una sola vez para todas las
 * subclases (patrón "singleton container" de Testcontainers: se arranca a
 * mano en un bloque estático y nunca se detiene explícitamente, en vez de
 * usar {@code @Container}, que reiniciaría uno nuevo por cada clase). Esto
 * es intencional: varios casos dependen de datos creados por casos
 * anteriores dentro de la misma historia (p. ej. HU-04 consulta la Tienda
 * que HU-03 registró), así que todas las clases comparten una única base de
 * datos con las migraciones de Flyway ya aplicadas (incluye el usuario
 * administrador semilla de V4 y los scopes de V8/V10/V11).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractCasoPruebaIT {

    protected static final String ADMIN_EMAIL = "admin@supplychain.local";
    protected static final String ADMIN_PASSWORD = "CambiarInmediatamente2026!";

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    private int puerto;

    @BeforeEach
    void configurarRestAssured() {
        RestAssured.port = puerto;
        RestAssured.baseURI = "http://localhost";
        RestAssured.basePath = "/api/v1";
    }

    /** Genera un valor legible pero único, para no chocar con datos creados por otras clases de este paquete. */
    protected static String unico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected String iniciarSesion(String email, String password) {
        return given()
                .contentType("application/json")
                .body("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().path("accessToken");
    }

    protected String iniciarSesionComoAdmin() {
        return iniciarSesion(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    /** Registra un usuario nuevo (sin rol) como el admin, y devuelve su id. */
    protected String registrarUsuario(String tokenAdmin, String email, String nombreCompleto, String password) {
        return given()
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"email": "%s", "nombreCompleto": "%s", "password": "%s"}
                        """.formatted(email, nombreCompleto, password))
                .when().post("/usuarios")
                .then().statusCode(201)
                .body("estado", equalTo("ACTIVO"))
                .extract().path("id");
    }

    /** Crea un rol nuevo (vacío, sin scopes) como el admin, y devuelve su id. */
    protected String crearRol(String tokenAdmin, String nombre) {
        return given()
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "descripcion": "Rol de prueba para casos de Sprint 1"}
                        """.formatted(nombre))
                .when().post("/roles")
                .then().statusCode(201)
                .extract().path("id");
    }

    /** Busca el id de un scope existente por su código (p. ej. "cd:administrar"). */
    protected String idDeScope(String tokenAdmin, String codigo) {
        return given()
                .header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/scopes")
                .then().statusCode(200)
                .extract().path("find { it.codigo == '" + codigo + "' }.id");
    }

    protected void asignarScopeARol(String tokenAdmin, String rolId, String scopeId) {
        given()
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"scopeId": "%s"}
                        """.formatted(scopeId))
                .when().post("/roles/" + rolId + "/scopes")
                .then().statusCode(204);
    }

    protected void asignarRolAUsuario(String tokenAdmin, String usuarioId, String rolId) {
        given()
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"rolId": "%s"}
                        """.formatted(rolId))
                .when().post("/usuarios/" + usuarioId + "/roles")
                .then().statusCode(204);
    }

    /**
     * Crea un rol con un único scope y un usuario habilitado con ese rol —
     * atajo usado por HU-02 para tener "un usuario cuyo rol solo permite una
     * función concreta".
     */
    protected String[] crearUsuarioConUnScope(String tokenAdmin, String prefijo, String codigoScope,
                                               String password) {
        String rolId = crearRol(tokenAdmin, unico(prefijo + "-rol"));
        String scopeId = idDeScope(tokenAdmin, codigoScope);
        asignarScopeARol(tokenAdmin, rolId, scopeId);
        String email = unico(prefijo) + "@supplychain-test.local";
        String usuarioId = registrarUsuario(tokenAdmin, email, "Usuario QA " + prefijo, password);
        asignarRolAUsuario(tokenAdmin, usuarioId, rolId);
        return new String[] {email, usuarioId, rolId};
    }
}
