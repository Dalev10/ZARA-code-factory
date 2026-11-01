package com.codefactory.supplychain.shared.config;

import com.codefactory.supplychain.shared.dto.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;

/**
 * Contrato OpenAPI de la API, publicado en /v3/api-docs y Swagger UI (también en producción).
 *
 * Declara el esquema Bearer (sin él Swagger UI no muestra el botón "Authorize") y el cuerpo
 * de error común. Las respuestas de error que dependen solo de cómo está protegido el
 * endpoint (401, 403 y 400 por cuerpo inválido) las agrega {@link #respuestasDeErrorComunes()};
 * cada controlador documenta solo las propias de su operación (201, 404, 409...).
 *
 * Componentes transversales de tipo 'config', compartidos por todos los módulos.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_BEARER = "bearerAuth";
    private static final String ESQUEMA_ERROR = "ErrorResponse";

    private static final String DESCRIPCION = """
            Sistema de planificación de cadena de suministro retail: catálogo, red de tiendas y \
            centros de distribución, inventario por nodo, ventas, demanda perdida y reposición \
            interna. Monolito modular; cada grupo de endpoints pertenece a un módulo.

            **Autenticación.** `POST /api/v1/auth/login` devuelve un access token (JWT, 15 \
            minutos) y deja el refresh token en una cookie `HttpOnly`. Si la cuenta tiene MFA, \
            devuelve un token de desafío que se completa en `POST /api/v1/auth/login/mfa`. Para \
            probar en esta página: hacer login, copiar el `accessToken` y pegarlo en **Authorize**.

            **Autorización.** Cada operación exige un scope (se indica en su descripción). Las \
            operaciones sobre una tienda exigen además que el usuario pueda operar en ella; si \
            no puede, el recurso se trata como inexistente (404).

            **Errores.** Toda respuesta de error tiene el cuerpo `ErrorResponse`, con un \
            `mensaje` en español para la persona usuaria. 401: sin token, token inválido o \
            vencido, o usuario no activo. 403: sin el scope requerido.

            **Paginación.** Los listados paginados reciben `page` (desde 0), `size` y `sort` \
            (`campo,asc|desc`), y responden con `content`, `totalElements` y `totalPages`.

            **Idempotencia.** Registrar una venta o una solicitud no satisfecha con la misma \
            clave devuelve 200 con el registro original en lugar de duplicarlo.
            """;

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Supply Chain MVP API")
                        .version("v1")
                        .description(DESCRIPCION))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token obtenido en /api/v1/auth/login."))
                        .schemas(ModelConverters.getInstance().read(ErrorResponse.class)))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
    }

    /**
     * 401 en todo endpoint que no se declare público con {@code @SecurityRequirements()},
     * 403 en los que exigen un scope y 400 en los que reciben cuerpo. Solo agrega lo que el
     * endpoint no haya documentado ya.
     */
    @Bean
    public OperationCustomizer respuestasDeErrorComunes() {
        return (operation, handlerMethod) -> {
            boolean publico = operation.getSecurity() != null && operation.getSecurity().isEmpty();
            if (!publico) {
                agregarError(operation, "401", "No autenticado: sin token, token inválido o vencido, o usuario no activo");
            }
            if (exigeScope(handlerMethod)) {
                agregarError(operation, "403", "El usuario no tiene el scope que exige la operación");
            }
            if (recibeCuerpo(handlerMethod)) {
                agregarError(operation, "400", "Cuerpo ilegible o con campos que no cumplen las validaciones");
            }
            return operation;
        };
    }

    private static boolean exigeScope(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), PreAuthorize.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), PreAuthorize.class);
    }

    private static boolean recibeCuerpo(HandlerMethod handlerMethod) {
        return Arrays.stream(handlerMethod.getMethodParameters())
                .anyMatch(parametro -> parametro.hasParameterAnnotation(RequestBody.class));
    }

    private static void agregarError(Operation operation, String codigo, String descripcion) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        operation.getResponses().putIfAbsent(codigo, new ApiResponse()
                .description(descripcion)
                .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                        new MediaType().schema(new Schema<>().$ref(ESQUEMA_ERROR)))));
    }
}
