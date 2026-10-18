package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.out.PasswordComprometidaPort;
import com.codefactory.supplychain.identity.application.port.out.PasswordHasherPort;
import com.codefactory.supplychain.identity.application.port.out.RolRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.Password;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioRolEntity;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioRolId;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.UsuarioRolJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-42 (mapa HU-51): prioridad de CDs por tienda. Prueba extremo a extremo:
 * seguridad, reemplazo completo de la lista, orden, CD en uso y tienda sin prioridades.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PrioridadTiendaCdControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepositoryPort usuarioRepositoryPort;
    @Autowired private PasswordHasherPort passwordHasherPort;
    @Autowired private RolRepositoryPort rolRepositoryPort;
    @Autowired private UsuarioRolJpaRepository usuarioRolJpaRepository;

    private final ObjectMapper json = new ObjectMapper();

    @TestConfiguration
    static class FakeHibpConfig {
        @Bean @Primary
        PasswordComprometidaPort fake() { return p -> false; }
    }

    // ---- helpers ------------------------------------------------------------

    private String obtenerToken(String email, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private Usuario crearUsuario(String email) {
        return usuarioRepositoryPort.guardar(Usuario.reconstruir(
                UUID.randomUUID(), Email.de(email), "Test",
                passwordHasherPort.hashear(Password.de("contraseñaSegura123")),
                EstadoUsuario.ACTIVO, 0, null, false, null, null,
                Instant.now(), Instant.now()));
    }

    private String loguearComoAdmin(String email) throws Exception {
        Usuario u = crearUsuario(email);
        UUID rolAdmin = rolRepositoryPort.buscarPorNombre("ADMIN").orElseThrow().getId();
        usuarioRolJpaRepository.save(new UsuarioRolEntity(new UsuarioRolId(u.getId(), rolAdmin)));
        return obtenerToken(email, "contraseñaSegura123");
    }

    private String loguearComoUsuarioComun(String email) throws Exception {
        crearUsuario(email);
        return obtenerToken(email, "contraseñaSegura123");
    }

    private String crearTienda(String token, String nombre) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/tiendas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","ubicacion":"Medellín"}
                                """.formatted(nombre)))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }

    private String crearCd(String token, String nombre) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","ubicacion":"Bogotá"}
                                """.formatted(nombre)))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }

    private String rutaPrioridad(String tiendaId) {
        return "/api/v1/tiendas/" + tiendaId + "/prioridad-cd";
    }

    private void putPrioridad(String token, String tiendaId, String... cdIds) throws Exception {
        String lista = String.join(",", java.util.Arrays.stream(cdIds)
                .map(id -> "\"" + id + "\"").toList());
        mockMvc.perform(put(rutaPrioridad(tiendaId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cdIds\":[" + lista + "]}"))
                .andExpect(status().isOk());
    }

    // ---- escenarios ---------------------------------------------------------

    @Test
    void putAsignaLoscdIdsEnOrdenYGetLosDevuelveOrdenados() throws Exception {
        String token = loguearComoAdmin("prioridad-orden@ejemplo.com");
        String tienda = crearTienda(token, "Tienda Orden");
        String cd1 = crearCd(token, "CD Norte");
        String cd2 = crearCd(token, "CD Sur");
        String cd3 = crearCd(token, "CD Centro");

        mockMvc.perform(put(rutaPrioridad(tienda))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cdIds\":[\"%s\",\"%s\",\"%s\"]}".formatted(cd1, cd2, cd3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].cdId").value(cd1))
                .andExpect(jsonPath("$[0].ordenPrioridad").value(1))
                .andExpect(jsonPath("$[1].cdId").value(cd2))
                .andExpect(jsonPath("$[1].ordenPrioridad").value(2))
                .andExpect(jsonPath("$[2].cdId").value(cd3))
                .andExpect(jsonPath("$[2].ordenPrioridad").value(3));

        mockMvc.perform(get(rutaPrioridad(tienda))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].cdId").value(cd1));
    }

    @Test
    void putReemplazaListaCompletaYReordena() throws Exception {
        String token = loguearComoAdmin("prioridad-reemplaza@ejemplo.com");
        String tienda = crearTienda(token, "Tienda Reemplaza");
        String cd1 = crearCd(token, "CD Reemplaza 1");
        String cd2 = crearCd(token, "CD Reemplaza 2");

        putPrioridad(token, tienda, cd1, cd2);

        // Reordenar: cd2 primero
        mockMvc.perform(put(rutaPrioridad(tienda))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cdIds\":[\"%s\",\"%s\"]}".formatted(cd2, cd1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cdId").value(cd2))
                .andExpect(jsonPath("$[0].ordenPrioridad").value(1))
                .andExpect(jsonPath("$[1].cdId").value(cd1))
                .andExpect(jsonPath("$[1].ordenPrioridad").value(2));
    }

    @Test
    void putConListaVaciaEliminaTodasLasPrioridades() throws Exception {
        String token = loguearComoAdmin("prioridad-vacia@ejemplo.com");
        String tienda = crearTienda(token, "Tienda Vacia");
        String cd = crearCd(token, "CD Vacia");

        putPrioridad(token, tienda, cd);

        mockMvc.perform(put(rutaPrioridad(tienda))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cdIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get(rutaPrioridad(tienda))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getTiendaSinPrioridadesDevuelveListaVacia() throws Exception {
        String token = loguearComoAdmin("prioridad-sin@ejemplo.com");
        String tienda = crearTienda(token, "Tienda Sin Prioridades");

        mockMvc.perform(get(rutaPrioridad(tienda))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void eliminarCdEnUsoDevuelve409() throws Exception {
        String token = loguearComoAdmin("prioridad-cd-en-uso@ejemplo.com");
        String tienda = crearTienda(token, "Tienda CD En Uso");
        String cd = crearCd(token, "CD En Uso");

        putPrioridad(token, tienda, cd);

        mockMvc.perform(delete("/api/v1/centros-distribucion/" + cd)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminarCdSinPrioridadesPermiteEliminar() throws Exception {
        String token = loguearComoAdmin("prioridad-cd-libre@ejemplo.com");
        String cd = crearCd(token, "CD Libre");

        mockMvc.perform(delete("/api/v1/centros-distribucion/" + cd)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void usuarioSinScopeRecibe403() throws Exception {
        String tokenAdmin = loguearComoAdmin("prioridad-scope-admin@ejemplo.com");
        String tienda = crearTienda(tokenAdmin, "Tienda Scope");
        String tokenComun = loguearComoUsuarioComun("prioridad-scope-comun@ejemplo.com");

        mockMvc.perform(get(rutaPrioridad(tienda))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenComun))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(rutaPrioridad(tienda))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenComun)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cdIds\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinAutenticacionRecibe401() throws Exception {
        mockMvc.perform(get("/api/v1/tiendas/" + UUID.randomUUID() + "/prioridad-cd"))
                .andExpect(status().isUnauthorized());
    }
}
