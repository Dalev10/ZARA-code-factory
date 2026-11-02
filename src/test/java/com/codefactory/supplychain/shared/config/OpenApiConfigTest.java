package com.codefactory.supplychain.shared.config;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El contrato publicado en /v3/api-docs: identidad, cuerpo de error y respuestas comunes. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("OpenApiConfig: contrato publicado")
class OpenApiConfigTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String REF_ERROR = "#/components/schemas/ErrorResponse";

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("publica título, versión y el esquema de error sin autenticación")
    void identidadYEsquemaDeError() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Supply Chain MVP API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.mensaje").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    @DisplayName("un endpoint con scope y cuerpo documenta 400, 401 y 403 con ErrorResponse")
    void endpointConScopeYCuerpo() throws Exception {
        String respuestas = "$.paths['/api/v1/ventas'].post.responses";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(respuestas + "['400'].content['application/json'].schema['$ref']").value(REF_ERROR))
                .andExpect(jsonPath(respuestas + "['401'].content['application/json'].schema['$ref']").value(REF_ERROR))
                .andExpect(jsonPath(respuestas + "['403'].content['application/json'].schema['$ref']").value(REF_ERROR));
    }

    @Test
    @DisplayName("sin cuerpo no documenta 400; sin scope no documenta 403")
    void soloLoQueAplica() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/v1/ventas/{id}'].get.responses['400']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/ventas/{id}'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/mfa/activar'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/mfa/activar'].post.responses['403']").doesNotExist());
    }

    @Test
    @DisplayName("login es público, documenta su 401 con ErrorResponse y trae los dos ejemplos de respuesta")
    void loginPublico() throws Exception {
        String login = "$.paths['/api/v1/auth/login'].post";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(login + ".tags[0]").value("Autenticación"))
                .andExpect(jsonPath(login + ".security").isEmpty())
                .andExpect(jsonPath(login + ".responses['401'].content['application/json'].schema['$ref']")
                        .value(REF_ERROR))
                .andExpect(jsonPath(login + ".responses['200'].content['application/json'].examples.length()")
                        .value(2));
    }

    // Declarar solo respuestas de error con @ApiResponse hace que springdoc omita la de éxito.
    @Test
    @DisplayName("toda operación documenta su respuesta de éxito")
    void todaOperacionTieneRespuestaDeExito() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> respuestas = JsonPath.read(json, "$.paths.*.*.responses");

        assertThat(respuestas).isNotEmpty().allSatisfy(porCodigo ->
                assertThat(porCodigo.keySet()).anyMatch(codigo -> codigo.startsWith("2")));
    }

    // Un endpoint nuevo sin @Operation ni @Tag aparece en Swagger UI con el nombre del método y del controlador.
    @Test
    @DisplayName("toda operación tiene resumen, descripción y un grupo documentado")
    void todaOperacionEstaDocumentada() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> operaciones = JsonPath.read(json, "$.paths.*.*");
        List<String> gruposDocumentados = JsonPath.read(json, "$.tags[?(@.description)].name");

        assertThat(operaciones).isNotEmpty().allSatisfy(operacion -> {
            assertThat(operacion.get("summary")).as("summary de %s", operacion.get("operationId")).isNotNull();
            assertThat(operacion.get("description")).as("description de %s", operacion.get("operationId"))
                    .isNotNull();
            assertThat((List<?>) operacion.get("tags")).as("tags de %s", operacion.get("operationId"))
                    .hasSize(1).allMatch(gruposDocumentados::contains);
        });
    }

    @Test
    @DisplayName("los listados paginados publican page, size y sort como parámetros sueltos")
    void paginacionComoParametrosSueltos() throws Exception {
        String parametros = "$.paths['/api/v1/ventas'].get.parameters[*].name";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(parametros).value(hasItems("page", "size", "sort")))
                .andExpect(jsonPath(parametros).value(not(hasItem("pageable"))));
    }

    @Test
    @DisplayName("un 404 declarado sin contenido se documenta con ErrorResponse")
    void errorDeclaradoSinContenido() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/v1/usuarios/{id}'].get.responses['404'].content"
                        + "['application/json'].schema['$ref']").value(REF_ERROR));
    }
}
