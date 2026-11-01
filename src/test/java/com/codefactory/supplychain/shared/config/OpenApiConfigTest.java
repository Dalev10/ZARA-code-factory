package com.codefactory.supplychain.shared.config;

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
                .andExpect(jsonPath("$.paths['/api/v1/ventas'].get.responses['400']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/ventas'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/mfa/activar'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/mfa/activar'].post.responses['403']").doesNotExist());
    }
}
