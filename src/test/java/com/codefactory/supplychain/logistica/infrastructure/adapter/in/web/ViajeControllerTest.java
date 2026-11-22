package com.codefactory.supplychain.logistica.infrastructure.adapter.in.web;

import com.codefactory.supplychain.soporte.AbastecimientoDePrueba;
import com.codefactory.supplychain.soporte.UsuariosDePrueba;
import com.codefactory.supplychain.soporte.UsuariosDePrueba.UsuarioDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-47 de extremo a extremo: los ciclos se crean con HU-45 y HU-46 reales.
 *
 * Módulo: logistica.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import({UsuariosDePrueba.class, AbastecimientoDePrueba.class})
@DisplayName("Armar viaje (HU-47)")
class ViajeControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuariosDePrueba usuarios;
    @Autowired private AbastecimientoDePrueba red;

    private UUID varianteId;
    private UUID cdId;
    private UsuarioDePrueba coordinador;

    @BeforeEach
    void setUp() {
        varianteId = red.crearVariante();
        cdId = red.crearCd(varianteId, 100);
        coordinador = usuarios.conRol("COORDINADOR_TRANSPORTE");
    }

    @Test
    @DisplayName("""
            Escenario: crear viaje
              Dados dos ciclos listos del mismo CD para dos tiendas
              Cuando creo un viaje con los dos
              Entonces el viaje queda ARMADO
              Y cada parada queda PENDIENTE, asociada a la tienda de su orden y a su despacho""")
    void crearViaje() throws Exception {
        UUID tiendaA = red.crearTienda(cdId);
        UUID tiendaB = red.crearTienda(cdId);
        UUID cicloA = red.cicloListo(tiendaA, varianteId, 5);
        UUID cicloB = red.cicloListo(tiendaB, varianteId, 7);

        armar(cdId, coordinador, cicloA, cicloB)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ARMADO"))
                .andExpect(jsonPath("$.cdId").value(cdId.toString()))
                .andExpect(jsonPath("$.paradas.length()").value(2))
                .andExpect(jsonPath("$.paradas[0].cicloDespachoId").value(cicloA.toString()))
                .andExpect(jsonPath("$.paradas[0].tiendaId").value(tiendaA.toString()))
                .andExpect(jsonPath("$.paradas[0].ordenParada").value(1))
                .andExpect(jsonPath("$.paradas[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.paradas[1].tiendaId").value(tiendaB.toString()));

        mockMvc.perform(get("/api/v1/ciclos-despacho").param("sinViaje", "true")
                        .param("ordenId", red.ordenDelCiclo(cicloA).toString())
                        .header(HttpHeaders.AUTHORIZATION, coordinador.bearer()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("""
            Escenario: despacho ya asignado
              Dado un ciclo que ya está en otro viaje
              Cuando intento agregarlo a un nuevo viaje
              Entonces responde 409""")
    void cicloYaAsignado() throws Exception {
        UUID ciclo = red.cicloListo(red.crearTienda(cdId), varianteId, 5);
        armar(cdId, coordinador, ciclo).andExpect(status().isCreated());

        armar(cdId, coordinador, ciclo).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("""
            Escenario: viaje sin paradas, o con vehículo vacío o un ciclo repetido
              Cuando intento guardarlo
              Entonces responde 400""")
    void viajeInvalido() throws Exception {
        UUID ciclo = red.cicloListo(red.crearTienda(cdId), varianteId, 5);

        enviar("{\"cdId\": \"%s\", \"vehiculo\": \"Camión\", \"paradas\": []}".formatted(cdId))
                .andExpect(status().isBadRequest());
        enviar("{\"cdId\": \"%s\", \"vehiculo\": \" \", \"paradas\": [{\"cicloDespachoId\": \"%s\"}]}"
                .formatted(cdId, ciclo)).andExpect(status().isBadRequest());
        armar(cdId, coordinador, ciclo, ciclo).andExpect(status().isBadRequest());
        assertThat(viajesDelCd()).isZero();
    }

    @Test
    @DisplayName("""
            Escenario: despacho de otro CD
              Dado un ciclo de una orden de otro CD
              Cuando intento agregarlo al viaje
              Entonces responde 422
              Y un CD o un ciclo inexistentes responden 404""")
    void cicloDeOtroCd() throws Exception {
        UUID otroCd = red.crearCd(varianteId, 50);
        UUID ciclo = red.cicloListo(red.crearTienda(otroCd), varianteId, 5);

        armar(cdId, coordinador, ciclo).andExpect(status().isUnprocessableContent());
        armar(UUID.randomUUID(), coordinador, ciclo).andExpect(status().isNotFound());
        armar(cdId, coordinador, UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Dos viajes simultáneos con el mismo ciclo: uno se arma y el otro responde 409")
    void dosViajesConElMismoCiclo() throws Exception {
        UUID ciclo = red.cicloListo(red.crearTienda(cdId), varianteId, 5);
        CountDownLatch salida = new CountDownLatch(1);
        Callable<Integer> intento = () -> {
            salida.await();
            return armar(cdId, coordinador, ciclo).andReturn().getResponse().getStatus();
        };
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        var resultados = List.of(hilos.submit(intento), hilos.submit(intento));
        salida.countDown();
        List<Integer> estados = List.of(resultados.get(0).get(30, TimeUnit.SECONDS),
                resultados.get(1).get(30, TimeUnit.SECONDS));
        hilos.shutdown();

        assertThat(estados).containsExactlyInAnyOrder(201, 409);
        assertThat(viajesDelCd()).isEqualTo(1);
    }

    @Test
    @DisplayName("Consultar: por id con sus paradas, listado por estado y 404")
    void consultar() throws Exception {
        UUID ciclo = red.cicloListo(red.crearTienda(cdId), varianteId, 5);
        String viajeId = com.jayway.jsonpath.JsonPath.read(
                armar(cdId, coordinador, ciclo).andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/viajes/{id}", viajeId).header(HttpHeaders.AUTHORIZATION, coordinador.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paradas[0].cicloDespachoId").value(ciclo.toString()));
        mockMvc.perform(get("/api/v1/viajes").param("estado", "ARMADO").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, coordinador.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '%s')].estado".formatted(viajeId)).value("ARMADO"));
        mockMvc.perform(get("/api/v1/viajes/{id}", UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, coordinador.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sin token 401; sin transporte:operar, 403")
    void seguridad() throws Exception {
        UUID ciclo = red.cicloListo(red.crearTienda(cdId), varianteId, 5);
        mockMvc.perform(get("/api/v1/viajes")).andExpect(status().isUnauthorized());
        armar(cdId, usuarios.conRol("OPERADOR_CD"), ciclo).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/viajes").header(HttpHeaders.AUTHORIZATION, usuarios.conScopes(Set.of()).bearer()))
                .andExpect(status().isForbidden());
    }

    private ResultActions armar(UUID cd, UsuarioDePrueba usuario, UUID... ciclos) throws Exception {
        StringBuilder paradas = new StringBuilder();
        for (UUID ciclo : ciclos) {
            paradas.append(paradas.isEmpty() ? "" : ", ").append("{\"cicloDespachoId\": \"").append(ciclo).append("\"}");
        }
        return mockMvc.perform(post("/api/v1/viajes").header(HttpHeaders.AUTHORIZATION, usuario.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cdId\": \"%s\", \"vehiculo\": \"Camión ABC-123\", \"paradas\": [%s]}".formatted(cd, paradas)));
    }

    private ResultActions enviar(String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/v1/viajes").header(HttpHeaders.AUTHORIZATION, coordinador.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private int viajesDelCd() {
        return jdbc.queryForObject("SELECT count(*) FROM viaje WHERE cd_id = ?", Integer.class, cdId);
    }
}
