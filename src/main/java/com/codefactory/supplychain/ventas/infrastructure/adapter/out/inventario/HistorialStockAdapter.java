package com.codefactory.supplychain.ventas.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.ventas.application.port.out.HistorialStockPort;
import com.codefactory.supplychain.ventas.domain.exception.IntegracionInventarioPendienteException;
import com.codefactory.supplychain.inventario.api.ConsultaKardex;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Capa anticorrupción hacia {@code inventario.api.ConsultaKardex}. Sin implementación en Inventario: falla con 503. */
@Component
public class HistorialStockAdapter implements HistorialStockPort {

    private final ObjectProvider<ConsultaKardex> consultaKardex;

    public HistorialStockAdapter(ObjectProvider<ConsultaKardex> consultaKardex) {
        this.consultaKardex = consultaKardex;
    }

    @Override
    public List<CambioStock> cambios(UUID almacenId, UUID varianteId, Instant desde, Instant hasta) {
        ConsultaKardex kardex = consultaKardex.getIfAvailable();
        if (kardex == null) {
            throw new IntegracionInventarioPendienteException("inventario.api.ConsultaKardex");
        }
        return kardex.consultarMovimientos(almacenId, varianteId, desde, hasta).stream()
                .map(m -> new CambioStock(m.id(), m.tipo().name(), m.cantidad(), m.saldoResultante(), m.fecha()))
                .toList();
    }
}
