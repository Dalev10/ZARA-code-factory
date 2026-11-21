package com.codefactory.supplychain.logistica.infrastructure.adapter.in.web;

import com.codefactory.supplychain.soporte.AbastecimientoDePrueba;
import com.codefactory.supplychain.soporte.UsuariosDePrueba;
import com.codefactory.supplychain.soporte.UsuariosDePrueba.UsuarioDePrueba;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
 * HU-48 de extremo a extremo: viajes armados con HU-47 sobre ciclos reales de HU-46.
 *
 * Módulo: logistica.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import({UsuariosDePrueba.class, AbastecimientoDePrueba.class})
@DisplayName("Salida y llegada de un viaje (HU-48)")
class TrayectoViajeControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuariosDePrueba usuarios;
    @Autowired private AbastecimientoDePrueba red;

    private UUID varianteId;
    private UUID cdId;
    private UsuarioDePrueba coordinador;
    private UUID cicloA;
    private UUID cicloB;

    @BeforeEach
    void setUp() {
        varianteId = red.crearVariante();
        cdId = red.crearCd(varianteId, 100);
        coordinador = usuarios.conRol("COORDINADOR_TRANSPORTE");
        cicloA = red.cicloListo(red.crearTienda(cdId), varianteId, 5);
        cicloB = red.cicloListo(red.crearTienda(cdId), varianteId, 7);
    }

    @Test
    @DisplayName("""
            Escenario: registrar salida
              Dado un viaje ARMADO con dos paradas
              Cuando registro la salida con una hora
              Entonces se guarda esa hora, el viaje queda EN_CURSO
              Y las paradas y las órdenes del viaje pasan a EN_TRANSITO""")
    void registrarSalida() throws Exception {
        String viaje = armar();
        // Un segundo después del despacho (dentro del margen de 2 minutos), en segundos para comparar el JSON.
        Instant salida = Instant.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);

        salida(viaje, "{\"fechaSalida\": \"" + salida + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.fechaSalida").value(salida.toString()))
                .andExpect(jsonPath("$.paradas[0].estado").value("EN_TRANSITO"))
                .andExpect(jsonPath("$.paradas[1].estado").value("EN_TRANSITO"));

        assertThat(red.estadoOrden(red.ordenDelCiclo(cicloA))).isEqualTo("EN_TRANSITO");
        assertThat(red.estadoOrden(red.ordenDelCiclo(cicloB))).isEqualTo("EN_TRANSITO");
    }

    @Test
    @DisplayName("""
            Escenario: registrar llegada por parada
              Dado un viaje EN_CURSO
              Cuando registro la llegada a una tienda, esa parada queda ENTREGADO y el viaje sigue EN_CURSO
              Y cuando llega a la última, el viaje pasa a CERRADO con fecha de cierre""")
    void registrarLlegadas() throws Exception {
        String viaje = armar();
        salida(viaje, null).andExpect(status().isOk());

        llegada(viaje, parada(viaje, 0), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paradas[0].estado").value("ENTREGADO"))
                .andExpect(jsonPath("$.paradas[0].fechaLlegada").exists())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"));
        llegada(viaje, parada(viaje, 1), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"))
                .andExpect(jsonPath("$.fechaCierre").exists());
        // La orden sigue EN_TRANSITO hasta la recepción (HU-49).
        assertThat(red.estadoOrden(red.ordenDelCiclo(cicloA))).isEqualTo("EN_TRANSITO");
    }

    @Test
    @DisplayName("""
            Escenario: cronología inválida (422) y hora futura (400)
              Una salida anterior al despacho de sus ciclos responde 422
              Una llegada anterior a la salida responde 422
              Una hora más de 2 minutos en el futuro responde 400; dentro del margen se acepta""")
    void cronologiaYHoraFutura() throws Exception {
        String viaje = armar();
        Instant ahora = Instant.now();

        salida(viaje, "{\"fechaSalida\": \"" + ahora.minus(1, ChronoUnit.HOURS) + "\"}")
                .andExpect(status().isUnprocessableContent());
        salida(viaje, "{\"fechaSalida\": \"" + ahora.plus(10, ChronoUnit.MINUTES) + "\"}")
                .andExpect(status().isBadRequest());
        salida(viaje, "{\"fechaSalida\": \"" + ahora.plus(1, ChronoUnit.MINUTES) + "\"}")
                .andExpect(status().isOk());

        llegada(viaje, parada(viaje, 0), "{\"fechaLlegada\": \"" + ahora + "\"}")
                .andExpect(status().isUnprocessableContent());
        llegada(viaje, parada(viaje, 0), "{\"fechaLlegada\": \"" + ahora.plus(1, ChronoUnit.HOURS) + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("""
            Escenario: estado inválido (409) y recursos inexistentes (404)
              Salir dos veces, llegar antes de salir o llegar dos veces a la misma parada responde 409
              Una parada de otro viaje o un viaje inexistente responde 404""")
    void estadosInvalidos() throws Exception {
        String viaje = armar();
        llegada(viaje, parada(viaje, 0), null).andExpect(status().isConflict());
        salida(viaje, null).andExpect(status().isOk());
        salida(viaje, null).andExpect(status().isConflict());
        llegada(viaje, parada(viaje, 0), null).andExpect(status().isOk());
        llegada(viaje, parada(viaje, 0), null).andExpect(status().isConflict());

        llegada(viaje, UUID.randomUUID().toString(), null).andExpect(status().isNotFound());
        salida(UUID.randomUUID().toString(), null).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Dos llegadas simultáneas a las dos paradas: ambas se registran y el viaje queda CERRADO")
    void dosLlegadasSimultaneasCierranElViaje() throws Exception {
        String viaje = armar();
        salida(viaje, null).andExpect(status().isOk());
        List<String> paradas = List.of(parada(viaje, 0), parada(viaje, 1));
        CountDownLatch salidaHilos = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        var resultados = paradas.stream().map(parada -> hilos.submit((Callable<Integer>) () -> {
            salidaHilos.await();
            return llegada(viaje, parada, null).andReturn().getResponse().getStatus();
        })).toList();
        salidaHilos.countDown();
        List<Integer> estados = List.of(resultados.get(0).get(30, TimeUnit.SECONDS),
                resultados.get(1).get(30, TimeUnit.SECONDS));
        hilos.shutdown();

        assertThat(estados).containsExactly(200, 200);
        consultar(viaje).andExpect(jsonPath("$.estado").value("CERRADO"));
    }

    @Test
    @DisplayName("Sin transporte:operar, registrar la salida responde 403")
    void seguridad() throws Exception {
        String viaje = armar();
        mockMvc.perform(post("/api/v1/viajes/{id}/salida", viaje)
                        .header(HttpHeaders.AUTHORIZATION, usuarios.conRol("OPERADOR_CD").bearer()))
                .andExpect(status().isForbidden());
        consultar(viaje).andExpect(jsonPath("$.estado").value("ARMADO"));
    }

    private String armar() throws Exception {
        String cuerpo = """
                {"cdId": "%s", "vehiculo": "Camión ABC-123",
                 "paradas": [{"cicloDespachoId": "%s"}, {"cicloDespachoId": "%s"}]}""".formatted(cdId, cicloA, cicloB);
        return JsonPath.read(mockMvc.perform(post("/api/v1/viajes").header(HttpHeaders.AUTHORIZATION, coordinador.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String parada(String viaje, int indice) throws Exception {
        return JsonPath.read(consultar(viaje).andReturn().getResponse().getContentAsString(),
                "$.paradas[" + indice + "].id");
    }

    private ResultActions consultar(String viaje) throws Exception {
        return mockMvc.perform(get("/api/v1/viajes/{id}", viaje).header(HttpHeaders.AUTHORIZATION, coordinador.bearer()));
    }

    private ResultActions salida(String viaje, String cuerpo) throws Exception {
        var peticion = post("/api/v1/viajes/{id}/salida", viaje).header(HttpHeaders.AUTHORIZATION, coordinador.bearer());
        return mockMvc.perform(cuerpo == null ? peticion : peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions llegada(String viaje, String parada, String cuerpo) throws Exception {
        var peticion = post("/api/v1/viajes/{id}/paradas/{paradaId}/llegada", viaje, parada)
                .header(HttpHeaders.AUTHORIZATION, coordinador.bearer());
        return mockMvc.perform(cuerpo == null ? peticion : peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}
