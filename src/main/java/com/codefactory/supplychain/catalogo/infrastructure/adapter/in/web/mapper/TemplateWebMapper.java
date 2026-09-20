package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.mapper;

import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.catalogo.domain.model.Template;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto.TemplateResponse;
import org.springframework.stereotype.Component;

/**
 * Conversión entre el dominio {@link Template} y los DTOs REST de
 * Template. No contiene lógica de negocio: solo traduce datos.
 */
@Component
public class TemplateWebMapper {

    public TemplateResponse toResponse(Template template) {
        Categoria categoria = template.getCategoria();
        Long categoriaId = categoria != null ? categoria.getId() : null;

        return new TemplateResponse(
                template.getId(),
                template.getNombre(),
                template.getTemporada(),
                template.getProveedor(),
                template.getPrecioBase(),
                categoriaId
        );
    }
}
