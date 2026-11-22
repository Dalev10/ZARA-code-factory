package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.out.NivelReposicionCdRepository;
import com.codefactory.supplychain.reposicion.application.port.out.ResolverNivelPort;
import com.codefactory.supplychain.reposicion.application.port.out.VarianteConsultaPort;
import com.codefactory.supplychain.reposicion.domain.model.NivelReposicionCd;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Nivel de reposición al CD constante (D-74), con la precedencia del umbral (D-58): variante
 * primero, categoría después. La tienda se ignora hoy; es el punto de extensión de HU-51.
 */
@Service
@Transactional(readOnly = true)
public class ResolverNivelService implements ResolverNivelPort {

    private final NivelReposicionCdRepository nivelRepository;
    private final VarianteConsultaPort varianteConsulta;

    public ResolverNivelService(NivelReposicionCdRepository nivelRepository, VarianteConsultaPort varianteConsulta) {
        this.nivelRepository = nivelRepository;
        this.varianteConsulta = varianteConsulta;
    }

    @Override
    public Optional<NivelAplicable> resolver(UUID tiendaId, UUID varianteId) {
        return nivelRepository.buscarPorVariante(varianteId)
                .or(() -> varianteConsulta.buscar(varianteId)
                        .flatMap(variante -> nivelRepository.buscarPorCategoria(variante.categoriaId())))
                .map(ResolverNivelService::aAplicable);
    }

    private static NivelAplicable aAplicable(NivelReposicionCd nivel) {
        return new NivelAplicable(nivel.getCantidadMinima(), nivel.getCantidadObjetivo());
    }
}
