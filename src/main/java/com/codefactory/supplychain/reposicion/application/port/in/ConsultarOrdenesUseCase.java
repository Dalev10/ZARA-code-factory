package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.EstadoOrden;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Consulta de órdenes de reabastecimiento, limitada a las tiendas del usuario.
 *
 * Módulo: reposicion.
 */
public interface ConsultarOrdenesUseCase {

    /**
     * @throws com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException
     *         si se filtra por una tienda fuera del alcance del usuario
     */
    Page<OrdenReabastecimiento> listar(UsuarioActual usuario, EstadoOrden estado, UUID tiendaId, Pageable pageable);

    /**
     * La orden con sus intentos de CD.
     *
     * @throws com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException
     *         si no existe o es de una tienda fuera del alcance
     */
    OrdenReabastecimiento obtener(UsuarioActual usuario, UUID id);

    /**
     * Búsqueda sin alcance de usuario, para la consulta entre módulos ({@code reposicion/api}).
     * El alcance por tienda lo aplica el módulo consumidor en su propia operación.
     */
    java.util.Optional<OrdenReabastecimiento> buscar(UUID id);
}
