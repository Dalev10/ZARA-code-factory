package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.EvaluacionRecomendacionesUseCase;
import com.codefactory.supplychain.reposicion.application.port.in.EvaluarRecomendacionUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarExistenciasPort;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarTiendasActivasPort;
import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.reposicion.domain.model.Recomendacion;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Evaluación manual de recomendaciones. Reutiliza
 * {@link EvaluarRecomendacionUseCase#evaluar} por cada variante con inventario de la
 * tienda. No abre transacción: cada evaluación confirma la suya, y si una falla se
 * registra y las demás siguen (mismo patrón que la evaluación masiva de traspasos).
 */
@Service
public class EvaluacionRecomendacionesService implements EvaluacionRecomendacionesUseCase {

    private static final Logger log = LoggerFactory.getLogger(EvaluacionRecomendacionesService.class);

    private final EvaluarRecomendacionUseCase evaluarRecomendacion;
    private final ConsultarTiendasActivasPort tiendasActivas;
    private final ConsultarExistenciasPort existencias;

    public EvaluacionRecomendacionesService(EvaluarRecomendacionUseCase evaluarRecomendacion,
                                            ConsultarTiendasActivasPort tiendasActivas,
                                            ConsultarExistenciasPort existencias) {
        this.evaluarRecomendacion = evaluarRecomendacion;
        this.tiendasActivas = tiendasActivas;
        this.existencias = existencias;
    }

    @Override
    public List<Recomendacion> evaluar(UsuarioActual usuario, UUID tiendaId) {
        if (tiendaId != null) {
            if (!usuario.puedeOperarEn(tiendaId)) {
                throw ReposicionRecursoNoEncontradoException.tienda(tiendaId);
            }
            return evaluarTienda(tiendaId);
        }
        List<Recomendacion> resultado = new ArrayList<>();
        for (UUID tienda : tiendasActivas.listarTiendasActivas()) {
            if (usuario.puedeOperarEn(tienda)) {
                resultado.addAll(evaluarTienda(tienda));
            }
        }
        return resultado;
    }

    private List<Recomendacion> evaluarTienda(UUID tiendaId) {
        List<Recomendacion> resultado = new ArrayList<>();
        for (UUID varianteId : existencias.variantesConInventario(tiendaId)) {
            try {
                evaluarRecomendacion.evaluar(tiendaId, varianteId).ifPresent(resultado::add);
            } catch (RuntimeException e) {
                log.warn("No se pudo evaluar la recomendación de la variante {} en la tienda {}",
                        varianteId, tiendaId, e);
            }
        }
        return resultado;
    }
}
