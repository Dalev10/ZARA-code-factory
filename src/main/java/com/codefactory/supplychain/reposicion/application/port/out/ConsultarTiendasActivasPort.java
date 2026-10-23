package com.codefactory.supplychain.reposicion.application.port.out;

import java.util.List;
import java.util.UUID;

/**
 * Tiendas activas que reposicion puede reponer.
 *
 * Módulo: reposicion — FEAT-09.
 */
public interface ConsultarTiendasActivasPort {

    List<UUID> listarTiendasActivas();

    boolean existe(UUID tiendaId);
}
