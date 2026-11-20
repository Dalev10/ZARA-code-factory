package com.codefactory.supplychain.reposicion.application.port.out;

import com.codefactory.supplychain.reposicion.domain.model.EstadoRecomendacion;
import com.codefactory.supplychain.reposicion.domain.model.Recomendacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de las recomendaciones de reposición al CD.
 *
 * Módulo: reposicion.
 */
public interface RecomendacionRepository {

    Recomendacion guardar(Recomendacion recomendacion);

    Optional<Recomendacion> buscarPorId(UUID id);

    boolean existeBorrador(UUID tiendaId, UUID varianteId);

    boolean existeOrdenGeneradaSinCd(UUID tiendaId, UUID varianteId);

    /**
     * Recomendaciones filtradas, la más reciente primero.
     */
    Page<Recomendacion> listar(EstadoRecomendacion estado, UUID tiendaId, UUID varianteId,
                               Collection<UUID> tiendas, Pageable pageable);
}
