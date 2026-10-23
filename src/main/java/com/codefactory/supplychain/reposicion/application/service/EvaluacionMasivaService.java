package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.EvaluacionMasivaUseCase;
import com.codefactory.supplychain.reposicion.application.port.in.TraspasoAutomaticoUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarTiendasActivasPort;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarExistenciasPort;


import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * HU-50: evaluación masiva de reposición interna (D-57).
 * <p>
 * Reutiliza {@link TraspasoAutomaticoUseCase#evaluar} por cada combinación tienda-variante:
 * la lógica de si dispara y cuánto traspasar ya vive ahí. Este servicio no abre
 * transacción: cada evaluación confirma la suya, y si una falla se registra y las
 * demás siguen.
 */
@Service
public class EvaluacionMasivaService implements EvaluacionMasivaUseCase {

    private static final Logger log = LoggerFactory.getLogger(EvaluacionMasivaService.class);

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
        if (!tiendasActivas.existe(tiendaId)) {
            throw ReposicionRecursoNoEncontradoException.tienda(tiendaId);
        }
        List<TraspasoInterno> resultado = new ArrayList<>();
        for (UUID varianteId : variantesPorTienda.variantesConInventario(tiendaId)) {
            try {
                traspasoAutomatico.evaluar(tiendaId, varianteId).ifPresent(resultado::add);
            } catch (RuntimeException e) {
                log.warn("No se pudo evaluar la reposición de la variante {} en la tienda {}", varianteId, tiendaId, e);
            }
        }
        return resultado;
    }
}
