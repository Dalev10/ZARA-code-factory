package com.codefactory.supplychain.identity.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.out.PasswordComprometidaPort;
import com.codefactory.supplychain.identity.application.port.out.PasswordHasherPort;
import com.codefactory.supplychain.identity.application.port.out.RolRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.RolScopeRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.ScopeRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.Password;
import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioRolEntity;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioRolId;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.UsuarioRolJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de extremo a extremo del registro de usuario y de la gestión de sus
 * roles (HU-10): HTTP -> caso de uso -> Postgres real (Testcontainers) ->
 * validación de esquema vía Flyway. El chequeo de HaveIBeenPwned se reemplaza
 * por un stub determinista para no depender de la red real ni de un tercero
 * en los tests.
 *
 * El cuerpo de las peticiones se arma a mano (sin ObjectMapper inyectado) porque Spring
 * Boot 4.1.1 registra por defecto un ObjectMapper de Jackson 3.x (tools.jackson.databind),
 * no el com.fasterxml.jackson.databind clásico — evita acoplar el test a esa decisión interna.
 *
 * @ActiveProfiles("test") carga application-test.yml (secreto JWT fijo de test),
 * necesario porque el contexto completo también instancia JjwtAccessTokenAdapter.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class UsuarioControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepositoryPort usuarioRepositoryPort;

    @Autowired
    private PasswordHasherPort passwordHasherPort;

    @Autowired
    private RolRepositoryPort rolRepositoryPort;

    @Autowired
    private UsuarioRolJpaRepository usuarioRolJpaRepository;

    @Autowired
    private ScopeRepositoryPort scopeRepositoryPort;

    @Autowired
    private RolScopeRepositoryPort rolScopeRepositoryPort;

    @TestConfiguration
    static class FakeHibpConfig {

        @Bean
        @Primary
        PasswordComprometidaPort passwordComprometidaPortFake() {
            return password -> false;
        }
    }

    private String obtenerAccessToken(String email, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = new ObjectMapper().readTree(login.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private Usuario crearUsuario(String email) {
        Usuario usuario = Usuario.reconstruir(UUID.randomUUID(), Email.de(email), "Usuario de Prueba",
                passwordHasherPort.hashear(Password.de("contraseñaSegura123")), EstadoUsuario.ACTIVO, 0, null,
                false, null, null, Instant.now(), Instant.now());
        return usuarioRepositoryPort.guardar(usuario);
    }

    private String loguearComoAdmin(String email) throws Exception {
        Usuario usuario = crearUsuario(email);
        UUID rolAdminId = rolRepositoryPort.buscarPorNombre("ADMIN").orElseThrow().getId();
        usuarioRolJpaRepository.save(new UsuarioRolEntity(new UsuarioRolId(usuario.getId(), rolAdminId)));
        return obtenerAccessToken(email, "contraseñaSegura123");
    }

    private String loguearComoUsuarioComun(String email) throws Exception {
        crearUsuario(email);
        return obtenerAccessToken(email, "contraseñaSegura123");
    }

    @Test
    void registrarSinAutenticacionSeRechaza() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrarUsuarioJson("sin-auth@ejemplo.com", "Sin Auth", "contraseñaSegura123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrarComoUsuarioComunSeRechazaCon403() throws Exception {
        String token = loguearComoUsuarioComun("no-admin-registra@ejemplo.com");

        mockMvc.perform(post("/api/v1/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrarUsuarioJson("victima-registro@ejemplo.com", "Alguien", "contraseñaSegura123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void registraUnUsuarioNuevoYDevuelve201ConSusDatos() throws Exception {
        String token = loguearComoAdmin("admin-registra@ejemplo.com");

        mockMvc.perform(post("/api/v1/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrarUsuarioJson("nuevo@ejemplo.com", "Nuevo Usuario", "contraseñaSegura123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("nuevo@ejemplo.com"))
                .andExpect(jsonPath("$.nombreCompleto").value("Nuevo Usuario"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void rechazaPasswordMasCortaQueLaPoliticaCon400() throws Exception {
        String token = loguearComoAdmin("admin-rechaza-password@ejemplo.com");

        mockMvc.perform(post("/api/v1/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrarUsuarioJson("otro@ejemplo.com", "Otro Usuario", "corta123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaEmailConFormatoInvalidoCon400() throws Exception {
        String token = loguearComoAdmin("admin-rechaza-email@ejemplo.com");

        mockMvc.perform(post("/api/v1/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrarUsuarioJson("no-es-un-email", "Alguien", "contraseñaSegura123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaEmailDuplicadoCon409() throws Exception {
        String token = loguearComoAdmin("admin-rechaza-duplicado@ejemplo.com");
        String cuerpo = registrarUsuarioJson("duplicado@ejemplo.com", "Persona Uno", "contraseñaSegura123");

        mockMvc.perform(post("/api/v1/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isConflict());
    }

    @Test
    void listarComoAdminIncluyeLosUsuariosRegistrados() throws Exception {
        String token = loguearComoAdmin("admin-lista-usuarios@ejemplo.com");
        crearUsuario("listado-http@ejemplo.com");

        mockMvc.perform(get("/api/v1/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'listado-http@ejemplo.com')]").exists());
    }

    @Test
    void listarComoUsuarioComunSeRechazaCon403() throws Exception {
        String token = loguearComoUsuarioComun("no-admin-lista-usuarios@ejemplo.com");

        mockMvc.perform(get("/api/v1/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void obtenerUnUsuarioInexistenteDevuelve404() throws Exception {
        String token = loguearComoAdmin("admin-obtiene-usuario-404@ejemplo.com");

        mockMvc.perform(get("/api/v1/usuarios/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void asignaYQuitaUnRolDeUnUsuario() throws Exception {
        String token = loguearComoAdmin("admin-asigna-rol@ejemplo.com");
        Usuario usuario = crearUsuario("recibe-rol@ejemplo.com");
        Rol rol = rolRepositoryPort.guardar(Rol.crear("ROL_HTTP_ASIGNABLE", null));

        mockMvc.perform(post("/api/v1/usuarios/" + usuario.getId() + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rolId": "%s"}
                                """.formatted(rol.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/usuarios/" + usuario.getId() + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("ROL_HTTP_ASIGNABLE"));

        mockMvc.perform(delete("/api/v1/usuarios/" + usuario.getId() + "/roles/" + rol.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/usuarios/" + usuario.getId() + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void asignarRolComoUsuarioComunSeRechazaCon403() throws Exception {
        String token = loguearComoUsuarioComun("no-admin-asigna-rol@ejemplo.com");
        Usuario usuario = crearUsuario("victima-asignacion@ejemplo.com");
        Rol rol = rolRepositoryPort.guardar(Rol.crear("ROL_HTTP_RECHAZADO", null));

        mockMvc.perform(post("/api/v1/usuarios/" + usuario.getId() + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rolId": "%s"}
                                """.formatted(rol.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void quitarElRolAdminAlUnicoAdministradorSeRechazaCon400() throws Exception {
        String email = "unico-admin-http@ejemplo.com";
        String token = loguearComoAdmin(email);
        Usuario admin = usuarioRepositoryPort.buscarPorEmail(Email.de(email)).orElseThrow();
        UUID rolAdminId = rolRepositoryPort.buscarPorNombre("ADMIN").orElseThrow().getId();

        // Deja a este usuario como el ÚNICO admin del sistema para que el escenario sea
        // determinista, sin depender de cuántos otros admins crearon otros tests de esta clase.
        List<UsuarioRolEntity> otrasAsignacionesAdmin = usuarioRolJpaRepository.findAll().stream()
                .filter(e -> e.getId().getRolId().equals(rolAdminId) && !e.getId().getUsuarioId().equals(admin.getId()))
                .toList();
        usuarioRolJpaRepository.deleteAll(otrasAsignacionesAdmin);

        mockMvc.perform(delete("/api/v1/usuarios/" + admin.getId() + "/roles/" + rolAdminId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("La operación dejaría al sistema sin ningún usuario ADMIN activo"));
    }

    @Test
    void desactivarCortaLaSesionYReactivarNoResucitaLosRefreshTokensViejos() throws Exception {
        String tokenAdmin = loguearComoAdmin("admin-desactiva@ejemplo.com");
        Usuario objetivo = crearUsuario("objetivo-desactivar@ejemplo.com");
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "objetivo-desactivar@ejemplo.com", "password": "contraseñaSegura123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String accessDelObjetivo = new ObjectMapper().readTree(login.getResponse().getContentAsString())
                .get("accessToken").asText();
        Cookie refreshViejo = login.getResponse().getCookie("refresh_token");

        mockMvc.perform(put("/api/v1/usuarios/" + objetivo.getId() + "/desactivar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));

        // Su access token vigente deja de autenticar (HU-30) y su refresh token ya no sirve.
        mockMvc.perform(get("/api/v1/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessDelObjetivo))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshViejo))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/usuarios/" + objetivo.getId() + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        // Reactivado, el refresh token de antes sigue revocado; tiene que volver a loguearse.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshViejo))
                .andExpect(status().isUnauthorized());
        obtenerAccessToken("objetivo-desactivar@ejemplo.com", "contraseñaSegura123");
    }

    @Test
    void unUsuarioNoPuedeDesactivarseASiMismo() throws Exception {
        String email = "admin-autodesactivacion@ejemplo.com";
        String token = loguearComoAdmin(email);
        Usuario admin = usuarioRepositoryPort.buscarPorEmail(Email.de(email)).orElseThrow();

        mockMvc.perform(put("/api/v1/usuarios/" + admin.getId() + "/desactivar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Un usuario no puede desactivar su propia cuenta"));
    }

    @Test
    void desactivarAlUnicoAdministradorActivoSeRechazaCon400() throws Exception {
        UUID rolAdminId = rolRepositoryPort.buscarPorNombre("ADMIN").orElseThrow().getId();
        Usuario unicoAdmin = crearUsuario("unico-admin-desactivar@ejemplo.com");
        usuarioRolJpaRepository.save(new UsuarioRolEntity(new UsuarioRolId(unicoAdmin.getId(), rolAdminId)));
        // Lo deja como el ÚNICO admin, igual que el test de quitar el rol ADMIN.
        usuarioRolJpaRepository.deleteAll(usuarioRolJpaRepository.findAll().stream()
                .filter(e -> e.getId().getRolId().equals(rolAdminId)
                        && !e.getId().getUsuarioId().equals(unicoAdmin.getId()))
                .toList());
        // Quien lo intenta administra usuarios sin ser ADMIN.
        Usuario gestor = crearUsuario("gestor-usuarios@ejemplo.com");
        Rol rolGestor = rolRepositoryPort.guardar(Rol.crear("GESTOR_USUARIOS_HU31", null));
        rolScopeRepositoryPort.asignar(rolGestor.getId(),
                scopeRepositoryPort.buscarPorCodigo("usuarios:administrar").orElseThrow().getId());
        usuarioRolJpaRepository.save(new UsuarioRolEntity(new UsuarioRolId(gestor.getId(), rolGestor.getId())));
        String tokenGestor = obtenerAccessToken("gestor-usuarios@ejemplo.com", "contraseñaSegura123");

        mockMvc.perform(put("/api/v1/usuarios/" + unicoAdmin.getId() + "/desactivar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGestor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La operación dejaría al sistema sin ningún usuario ADMIN activo"));
    }

    @Test
    void asignaListaYQuitaTiendasDeUnUsuarioConDatosQueProveeInventario() throws Exception {
        String tokenAdmin = loguearComoAdmin("admin-tiendas@ejemplo.com");
        Usuario vendedor = crearUsuario("vendedor-tiendas@ejemplo.com");
        String tiendaActiva = crearTienda(tokenAdmin, "Tienda Norte HU32");
        String tiendaInactiva = crearTienda(tokenAdmin, "Tienda Cerrada HU32");
        mockMvc.perform(delete("/api/v1/tiendas/" + tiendaInactiva)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());
        String rutaTiendas = "/api/v1/usuarios/" + vendedor.getId() + "/tiendas";

        for (String tienda : List.of(tiendaActiva, tiendaInactiva, tiendaActiva)) {
            mockMvc.perform(post(rutaTiendas)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tiendaId\": \"" + tienda + "\"}"))
                    .andExpect(status().isNoContent());
        }

        // La segunda asignación de la misma tienda no la duplica; la inactiva se puede asignar (D-70).
        mockMvc.perform(get(rutaTiendas).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == '" + tiendaActiva + "')].nombre").value("Tienda Norte HU32"))
                .andExpect(jsonPath("$[?(@.id == '" + tiendaActiva + "')].activa").value(true))
                .andExpect(jsonPath("$[?(@.id == '" + tiendaInactiva + "')].activa").value(false));

        mockMvc.perform(delete(rutaTiendas + "/" + tiendaActiva)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(rutaTiendas).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(tiendaInactiva));
    }

    @Test
    void asignarUnaTiendaQueNoExisteSeRechazaCon404() throws Exception {
        String tokenAdmin = loguearComoAdmin("admin-tienda-inexistente@ejemplo.com");
        Usuario vendedor = crearUsuario("vendedor-sin-tienda@ejemplo.com");

        mockMvc.perform(post("/api/v1/usuarios/" + vendedor.getId() + "/tiendas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tiendaId\": \"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("La tienda solicitada no existe"));
    }

    private String crearTienda(String tokenAdmin, String nombre) throws Exception {
        MvcResult creada = mockMvc.perform(post("/api/v1/tiendas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"" + nombre + "\", \"ubicacion\": \"Medellín\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return new ObjectMapper().readTree(creada.getResponse().getContentAsString()).get("id").asText();
    }

    private static String registrarUsuarioJson(String email, String nombreCompleto, String password) {
        return """
                {
                  "email": "%s",
                  "nombreCompleto": "%s",
                  "password": "%s"
                }
                """.formatted(email, nombreCompleto, password);
    }
}
