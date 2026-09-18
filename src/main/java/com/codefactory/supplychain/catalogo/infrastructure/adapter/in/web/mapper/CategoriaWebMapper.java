package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.mapper;

import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto.CategoriaResponse;
import org.springframework.stereotype.Component;

/**
 * Conversión entre el dominio {@link Categoria} y los DTOs REST de
 * Categoria. No contiene lógica de negocio: solo traduce datos.
 */
@Component
public class CategoriaWebMapper {

    public CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre());
    }
}
