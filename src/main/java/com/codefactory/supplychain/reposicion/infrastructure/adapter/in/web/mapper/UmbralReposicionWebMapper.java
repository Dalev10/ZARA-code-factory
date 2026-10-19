package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper;

import com.codefactory.supplychain.reposicion.domain.model.UmbralReposicion;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.UmbralReposicionResponse;
import org.springframework.stereotype.Component;

/**
 * Convierte el dominio {@link UmbralReposicion} a su respuesta HTTP.
 */
@Component
public class UmbralReposicionWebMapper {

    public UmbralReposicionResponse toResponse(UmbralReposicion umbral) {
        return new UmbralReposicionResponse(umbral.getId(), umbral.getVarianteId(), umbral.getCategoriaId(),
                umbral.getCantidadMinima(), umbral.getCantidadObjetivo());
    }
}
