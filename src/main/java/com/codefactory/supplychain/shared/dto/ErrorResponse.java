package com.codefactory.supplychain.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Componentes transversales de tipo 'dto', compartidos por todos los módulos.
 */
@Schema(description = "Cuerpo de toda respuesta de error de la API")
public record ErrorResponse(
        @Schema(description = "Motivo del error, en español, para la persona usuaria",
                example = "Recurso no encontrado")
        String mensaje) {
}
