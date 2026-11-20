package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.EstadoRecomendacion;
import com.codefactory.supplychain.reposicion.domain.model.Recomendacion;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Consulta de recomendaciones, limitada a las tiendas del usuario.
 *
 * Módulo: reposicion.
 */
public interface ConsultarRecomendacionesUseCase {

    /**
     * @throws com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException
     *         si se filtra por una tienda fuera del alcance del usuario
     */
    Page<Recomendacion> listar(UsuarioActual usuario, EstadoRecomendacion estado, UUID tiendaId,
                               UUID varianteId, Pageable pageable);

    /**
     * @throws com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException
     *         si no existe o es de una tienda fuera del alcance
     */
    Recomendacion obtener(UsuarioActual usuario, UUID id);
}
