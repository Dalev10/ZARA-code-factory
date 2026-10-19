package com.codefactory.supplychain.qa;

import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractCasoPruebaIT {

    protected static final String ADMIN_EMAIL = "admin@supplychain.local";
    protected static final String ADMIN_PASSWORD = "CambiarInmediatamente2026!";

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
        RestAssured.replaceFiltersWith(java.util.List.of(
                new RequestLoggingFilter(LogDetail.ALL),
                new ResponseLoggingFilter(LogDetail.ALL)));
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

    @BeforeEach
    void marcarInicioDeCaso(TestInfo info) {
        System.out.println("###CASO-INICIO### " + nombreDelMetodo(info));
    }

    @AfterEach
    void marcarFinDeCaso(TestInfo info) {
        System.out.println("###CASO-FIN### " + nombreDelMetodo(info));
    }

    private static String nombreDelMetodo(TestInfo info) {
        return info.getTestMethod().map(java.lang.reflect.Method::getName).orElse("desconocido");
    }

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

    /** La Bodega_Tienda se crea con su tienda (D-55): se consulta, no se registra. */
    protected String bodegaDeTienda(String tokenAdmin, String tiendaId) {
        return given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/tienda/" + tiendaId)
                .then().statusCode(200)
                .extract().path("id");
    }

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
