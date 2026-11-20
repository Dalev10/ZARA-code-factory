package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.reposicion.domain.model.NivelReposicionCd;

import java.util.UUID;

/**
 * Representación de un nivel de reposición al CD. Una de {@code varianteId} y {@code categoriaId}
 * es {@code null}.
 */
public record NivelReposicionCdResponse(
        UUID id,
        UUID varianteId,
        UUID categoriaId,
        int cantidadMinima,
        int cantidadObjetivo
) {

    public static NivelReposicionCdResponse desde(NivelReposicionCd nivel) {
        return new NivelReposicionCdResponse(nivel.getId(), nivel.getVarianteId(), nivel.getCategoriaId(),
                nivel.getCantidadMinima(), nivel.getCantidadObjetivo());
    }
}
