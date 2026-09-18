package com.codefactory.supplychain.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sin esto, Swagger UI no muestra el botón "Authorize": springdoc solo lo expone
 * cuando el documento OpenAPI declara al menos un esquema de seguridad. Los
 * endpoints que exigen autenticación (ver JwtAuthenticationFilter/SecurityConfig)
 * ya lo requieren igual sin esta clase — esto es únicamente para poder probarlos
 * desde Swagger UI pegando el access token.
 *
 * Componentes transversales de tipo 'config', compartidos por todos los módulos.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_BEARER = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
    }
}
