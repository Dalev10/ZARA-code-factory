package com.codefactory.supplychain.ventas.application.port.out;

import com.codefactory.supplychain.ventas.domain.model.Venta;
import com.codefactory.supplychain.ventas.domain.model.EstadoVenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VentaRepositoryPort {

    record Filtro(Collection<UUID> tiendasPermitidas, UUID tiendaId, UUID varianteId,
                  UUID transaccionId, EstadoVenta estado, Instant desde, Instant hasta) {
    }

    List<Venta> guardarTodas(List<Venta> ventas);

    void bloquearTransaccion(UUID transaccionId);

    List<Venta> buscarPorTransaccion(UUID transaccionId);

    List<Venta> buscarPorTransaccionParaActualizar(UUID transaccionId);

    Optional<Venta> buscarPorId(UUID id);

    Optional<Venta> buscarPorIdParaActualizar(UUID id);

    Venta guardar(Venta venta);

    Page<Venta> buscar(Filtro filtro, Pageable pageable);
}
