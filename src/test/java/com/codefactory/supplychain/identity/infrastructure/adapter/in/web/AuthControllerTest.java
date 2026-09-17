package com.codefactory.supplychain.identity.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.out.PasswordHasherPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.Password;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de extremo a extremo del login: HTTP -> caso de uso -> emisión de JWT ->
 * persistencia del refresh token en Postgres real (Testcontainers) -> cookie HttpOnly.
 *
 * @ActiveProfiles("test") carga application-test.yml (secreto JWT fijo de test).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private UsuarioRepositoryPort usuarioRepositoryPort;

    @Autowired
    private PasswordHasherPort passwordHasherPort;

    private Usuario crearUsuario(String email, String passwordCruda, EstadoUsuario estado) {
        Usuario usuario = Usuario.reconstruir(
                UUID.randomUUID(),
                Email.de(email),
                "Usuario de Prueba",
                passwordHasherPort.hashear(Password.de(passwordCruda)),
                estado,
                0,
                null,
                false,
                null,
                null,
                Instant.now(),
                Instant.now());
        return usuarioRepositoryPort.guardar(usuario);
    }

    @Test
    void loginExitosoDevuelveAccessTokenYCookieDeRefreshToken() throws Exception {
        crearUsuario("login-ok@ejemplo.com", "contraseñaSegura123", EstadoUsuario.ACTIVO);

        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("login-ok@ejemplo.com", "contraseñaSegura123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.usuario.email").value("login-ok@ejemplo.com"))
                .andReturn();

        String setCookie = resultado.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("refresh_token=");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=Strict");
        assertThat(setCookie).contains("Path=/api/v1/auth");

        // El refresh token no debe filtrarse en el cuerpo de la respuesta.
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("refreshToken");
    }

    @Test
    void rechazaPasswordIncorrectaCon401() throws Exception {
        crearUsuario("login-mal@ejemplo.com", "contraseñaSegura123", EstadoUsuario.ACTIVO);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("login-mal@ejemplo.com", "contraseñaIncorrecta")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    void rechazaEmailInexistenteCon401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("no-existe@ejemplo.com", "cualquierContraseña123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaCuentaBloqueadaCon401YMensajeGenerico() throws Exception {
        crearUsuario("bloqueado@ejemplo.com", "contraseñaSegura123", EstadoUsuario.BLOQUEADO);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("bloqueado@ejemplo.com", "contraseñaSegura123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    void alTercerIntentoFallidoQuedaBloqueadaYRechazaAunConLaPasswordCorrecta() throws Exception {
        crearUsuario("fuerza-bruta@ejemplo.com", "contraseñaSegura123", EstadoUsuario.ACTIVO);

        for (int intento = 1; intento <= 3; intento++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("fuerza-bruta@ejemplo.com", "incorrecta")))
                    .andExpect(status().isUnauthorized());
        }

        // Al 3er fallo ya debería estar bloqueada temporalmente (curva: 3 -> 1 minuto),
        // así que ni siquiera la contraseña correcta debería dejarla entrar ahora.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("fuerza-bruta@ejemplo.com", "contraseñaSegura123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    void unLoginExitosoReseteaElContadorTrasIntentosFallidosPreviosSinLlegarAlUmbral() throws Exception {
        crearUsuario("recupera@ejemplo.com", "contraseñaSegura123", EstadoUsuario.ACTIVO);

        // 2 fallos consecutivos: todavía por debajo del umbral de bloqueo (3).
        for (int intento = 1; intento <= 2; intento++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("recupera@ejemplo.com", "incorrecta")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("recupera@ejemplo.com", "contraseñaSegura123")))
                .andExpect(status().isOk());
    }

    @Test
    void refrescarConElTokenVigenteDevuelveAccessNuevoYRotaLaCookie() throws Exception {
        crearUsuario("refresca@ejemplo.com", "contraseñaSegura123", EstadoUsuario.ACTIVO);
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("refresca@ejemplo.com", "contraseñaSegura123")))
                .andExpect(status().isOk())
                .andReturn();
        String refreshTokenOriginal = extraerValorCookie(login);

        MvcResult refresh = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshTokenOriginal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        String refreshTokenNuevo = extraerValorCookie(refresh);
        assertThat(refreshTokenNuevo).isNotEqualTo(refreshTokenOriginal);
    }

    @Test
    void reusarUnRefreshTokenYaRotadoSeRechazaYRevocaLaFamiliaCompleta() throws Exception {
        crearUsuario("robado@ejemplo.com", "contraseñaSegura123", EstadoUsuario.ACTIVO);
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("robado@ejemplo.com", "contraseñaSegura123")))
                .andExpect(status().isOk())
                .andReturn();
        String refreshTokenOriginal = extraerValorCookie(login);

        // Primer refresh: legítimo, rota el token.
        MvcResult primerRefresh = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshTokenOriginal)))
                .andExpect(status().isOk())
                .andReturn();
        String refreshTokenRotado = extraerValorCookie(primerRefresh);

        // Alguien vuelve a presentar el token YA rotado (ej. interceptado antes de la
        // rotación legítima) -> se detecta el reuse y se rechaza.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshTokenOriginal)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Sesión inválida o expirada"));

        // La detección de reuse revoca TODA la familia: incluso el token rotado
        // legítimamente (refreshTokenRotado) queda invalidado.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshTokenRotado)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refrescarSinCookieSeRechaza() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    private static String extraerValorCookie(MvcResult resultado) {
        String setCookie = resultado.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        String parteValor = setCookie.split(";", 2)[0];
        return parteValor.substring(parteValor.indexOf('=') + 1);
    }

    private static String loginJson(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }
}
