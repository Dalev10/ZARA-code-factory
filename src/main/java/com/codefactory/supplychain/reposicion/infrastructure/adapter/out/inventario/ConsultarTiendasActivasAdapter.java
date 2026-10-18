package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.inventario.api.ConsultaTiendas;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarTiendasActivasPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * ACL hacia inventario.api: obtiene los ids de todas las tiendas activas para la
 * evaluación masiva (HU-41). Usa {@link ConsultaTiendas} que ya existe desde HU-32
 * (sin cambios pendientes).
 *
 * Módulo: reposicion — FEAT-09.
 */
@Component
public class ConsultarTiendasActivasAdapter implements ConsultarTiendasActivasPort {

    private final ConsultaTiendas consultaTiendas;

    public ConsultarTiendasActivasAdapter(ConsultaTiendas consultaTiendas) {
        this.consultaTiendas = consultaTiendas;
    }

    @Override
    public List<UUID> listarTiendasActivas() {
        // TODO pendiente #4: ConsultaTiendas no expone buscarTodasActivas().
        return List.of();
    }
}
