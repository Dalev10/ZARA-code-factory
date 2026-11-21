package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import com.codefactory.supplychain.reposicion.domain.model.Recomendacion;
import com.codefactory.supplychain.shared.security.UsuarioActual;

import java.util.UUID;

/**
 * Aprobar o rechazar una recomendación en BORRADOR. Solo sobre tiendas del usuario:
 * fuera de su alcance, 404. Reintenta por {@code @Version} fuera de la transacción.
 *
 * Módulo: reposicion.
 */
public interface ResolverRecomendacionUseCase {

    /**
     * Aprueba con la cantidad indicada o, si es nula, con la sugerida, y en la misma transacción
     * crea la orden con fallback de CD. La orden queda APROBADA con CD o GENERADA sin CD.
     */
    Aprobacion aprobar(UsuarioActual usuario, UUID recomendacionId, Integer cantidadAprobada);

    /** Rechaza: no genera orden y conserva el registro. */
    Recomendacion rechazar(UsuarioActual usuario, UUID recomendacionId);

    /** La recomendación aprobada y la orden que creó. */
    record Aprobacion(Recomendacion recomendacion, OrdenReabastecimiento orden) {
    }
}
