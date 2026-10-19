package com.codefactory.supplychain.ventas.application.port.out;

import com.codefactory.supplychain.ventas.domain.model.VentanaQuiebre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VentanaQuiebreRepositoryPort {

    /** {@code tiendasPermitidas} nulo = sin restricción; vacío = ninguna tienda. */
    record Filtro(Collection<UUID> tiendasPermitidas, UUID tiendaId, UUID varianteId, Boolean abierta,
                  Instant desde, Instant hasta) {
    }

    Optional<VentanaQuiebre> buscarPorId(UUID id);

    Optional<VentanaQuiebre> buscarAbierta(UUID tiendaId, UUID varianteId);

    /** INSERT ... ON CONFLICT DO NOTHING contra el índice parcial único. Devuelve false si ya había una abierta. */
    boolean abrirSiAusente(VentanaQuiebre ventana);

    VentanaQuiebre guardar(VentanaQuiebre ventana);

    List<VentanaQuiebre> listarPorTiendaYVariante(UUID tiendaId, UUID varianteId);

    List<VentanaQuiebre> listarAbiertasPorTienda(UUID tiendaId);

    List<UUID> variantesConVentanas(UUID tiendaId);

    Page<VentanaQuiebre> buscar(Filtro filtro, Pageable pageable);
}
