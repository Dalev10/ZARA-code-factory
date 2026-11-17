package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.Recomendacion;
import com.codefactory.supplychain.shared.security.UsuarioActual;

import java.util.List;
import java.util.UUID;

/**
 * Evaluación manual de recomendaciones.
 *
 * Módulo: reposicion.
 */
public interface EvaluacionRecomendacionesUseCase {

    /**
     * Evalúa la tienda indicada o, si {@code tiendaId} es nulo, todas las tiendas
     * activas del usuario. Una variante que falla se registra y no detiene al resto.
     *
     * @return las recomendaciones creadas en esta evaluación
     * @throws com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException
     *         si la tienda está fuera del alcance del usuario
     */
    List<Recomendacion> evaluar(UsuarioActual usuario, UUID tiendaId);
}
