package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.mapper;

import com.codefactory.supplychain.catalogo.domain.model.Template;
import com.codefactory.supplychain.catalogo.domain.model.Variante;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto.VarianteResponse;
import org.springframework.stereotype.Component;

/**
 * Conversión entre el dominio {@link Variante} y los DTOs REST de
 * Variante. No contiene lógica de negocio: solo traduce datos.
 */
@Component
public class VarianteWebMapper {

    public VarianteResponse toResponse(Variante variante) {
        Template template = variante.getTemplate();
        Long templateId = template != null ? template.getId() : null;

        return new VarianteResponse(
                variante.getId(),
                variante.getSku(),
                variante.getTalla(),
                variante.getColor(),
                templateId
        );
    }
}
