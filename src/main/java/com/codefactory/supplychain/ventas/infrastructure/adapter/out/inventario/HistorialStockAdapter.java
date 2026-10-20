package com.codefactory.supplychain.ventas.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.inventario.api.ConsultaKardex;
import com.codefactory.supplychain.ventas.application.port.out.HistorialStockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Capa anticorrupción hacia {@code inventario.api.ConsultaKardex}. */
@Component
@RequiredArgsConstructor
class HistorialStockAdapter implements HistorialStockPort {

    private final ConsultaKardex consultaKardex;

    @Override
    public List<CambioStock> cambios(UUID almacenId, UUID varianteId, Instant desde, Instant hasta) {
        return consultaKardex.consultarMovimientos(almacenId, varianteId, desde, hasta).stream()
                .map(m -> new CambioStock(m.id(), m.tipo().name(), m.cantidad(), m.saldoResultante(), m.fecha()))
                .toList();
    }
}
