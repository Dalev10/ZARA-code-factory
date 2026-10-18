package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.EvaluacionMasivaUseCase;
import com.codefactory.supplychain.reposicion.application.port.in.TraspasoAutomaticoUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarTiendasActivasPort;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarExistenciasPort;


import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * HU-41: evaluación masiva de reposición interna.
 * <p>
 * Reutiliza {@link TraspasoAutomaticoUseCase#evaluar} por cada combinación tienda-variante:
 * la lógica de si dispara y cuánto traspasar ya vive ahí. Cada evaluación
 * individual es su propia transacción (REQUIRES_NEW en el servicio de destino); si una
 * falla, las demás siguen.
 */
@Service
@Transactional(readOnly = true)
public class EvaluacionMasivaService implements EvaluacionMasivaUseCase {

    private final TraspasoAutomaticoUseCase traspasoAutomatico;
    private final ConsultarTiendasActivasPort tiendasActivas;
    private final ConsultarExistenciasPort variantesPorTienda;

    public EvaluacionMasivaService(TraspasoAutomaticoUseCase traspasoAutomatico,
                                   ConsultarTiendasActivasPort tiendasActivas,
                                   ConsultarExistenciasPort variantesPorTienda) {
        this.traspasoAutomatico = traspasoAutomatico;
        this.tiendasActivas = tiendasActivas;
        this.variantesPorTienda = variantesPorTienda;
    }

    @Override
    public List<TraspasoInterno> evaluarTodas() {
        List<TraspasoInterno> resultado = new ArrayList<>();
        for (UUID tiendaId : tiendasActivas.listarTiendasActivas()) {
            resultado.addAll(evaluarTienda(tiendaId));
        }
        return resultado;
    }

    @Override
    public List<TraspasoInterno> evaluarTienda(UUID tiendaId) {
        List<TraspasoInterno> resultado = new ArrayList<>();
        for (UUID varianteId : variantesPorTienda.variantesConInventarioEnAlmacen(tiendaId)) {
            traspasoAutomatico.evaluar(tiendaId, varianteId).ifPresent(resultado::add);
        }
        return resultado;
    }
}
