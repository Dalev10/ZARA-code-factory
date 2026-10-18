package com.codefactory.supplychain.ventas.application.port.out;

import com.codefactory.supplychain.ventas.domain.model.EstadoSolicitud;
import com.codefactory.supplychain.ventas.domain.model.SolicitudNoSatisfecha;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/** Persistencia propia de Ventas y demanda. Consultar y anular nunca pasan por Inventario. */
public interface SolicitudNoSatisfechaRepositoryPort {

    /** {@code tiendasPermitidas} nulo = sin restricción; vacío = ninguna tienda. */
    record Filtro(Collection<UUID> tiendasPermitidas, UUID tiendaId, UUID varianteId, EstadoSolicitud estado,
                  Instant desde, Instant hasta) {
    }

    Optional<SolicitudNoSatisfecha> buscarPorId(UUID id);

    Optional<SolicitudNoSatisfecha> buscarPorClave(String claveIdempotencia);

    /** INSERT ... ON CONFLICT DO NOTHING sobre la clave de idempotencia. Devuelve false si ya existía. */
    boolean insertarSiAusente(SolicitudNoSatisfecha solicitud);

    SolicitudNoSatisfecha guardar(SolicitudNoSatisfecha solicitud);

    Page<SolicitudNoSatisfecha> buscar(Filtro filtro, Pageable pageable);
}
