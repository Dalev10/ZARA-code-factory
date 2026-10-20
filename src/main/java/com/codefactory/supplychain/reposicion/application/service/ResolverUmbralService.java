package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.out.ResolverUmbralPort;
import com.codefactory.supplychain.reposicion.application.port.out.UmbralReposicionRepository;
import com.codefactory.supplychain.reposicion.application.port.out.VarianteConsultaPort;
import com.codefactory.supplychain.reposicion.domain.model.UmbralReposicion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Umbral constante (D-58): variante primero, categoría después. La precedencia es regla
 * de negocio, por eso vive en la capa de aplicación y no en un adaptador. La tienda se
 * ignora hoy; el Sprint 4 la usará para hacerlo dinámico.
 */
@Service
@Transactional(readOnly = true)
public class ResolverUmbralService implements ResolverUmbralPort {

    private final UmbralReposicionRepository umbralRepository;
    private final VarianteConsultaPort varianteConsulta;

    public ResolverUmbralService(UmbralReposicionRepository umbralRepository,
                                 VarianteConsultaPort varianteConsulta) {
        this.umbralRepository = umbralRepository;
        this.varianteConsulta = varianteConsulta;
    }

    @Override
    public Optional<UmbralAplicable> resolver(UUID tiendaId, UUID varianteId) {
        return umbralRepository.buscarPorVariante(varianteId)
                .or(() -> buscarPorCategoriaDeLaVariante(varianteId))
                .map(ResolverUmbralService::aAplicable);
    }

    private Optional<UmbralReposicion> buscarPorCategoriaDeLaVariante(UUID varianteId) {
        return varianteConsulta.buscar(varianteId)
                .flatMap(variante -> umbralRepository.buscarPorCategoria(variante.categoriaId()));
    }

    private static UmbralAplicable aAplicable(UmbralReposicion umbral) {
        return new UmbralAplicable(umbral.getCantidadMinima(), umbral.getCantidadObjetivo());
    }
}
