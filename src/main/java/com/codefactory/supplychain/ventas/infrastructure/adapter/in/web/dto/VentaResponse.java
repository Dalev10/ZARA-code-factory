package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.ventas.domain.model.Venta;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VentaResponse(UUID transaccionId, UUID tiendaId, UUID vendedorId, Instant fecha,
                            BigDecimal total, List<Linea> lineas) {

    public record Linea(UUID id, UUID varianteId, int cantidad, BigDecimal precioUnitario, String canal,
                        boolean forzada, String estado, UUID anuladaPor, Instant anuladaEn,
                        String motivoAnulacion) {
    }

    public static VentaResponse from(List<Venta> ventas) {
        Venta primera = ventas.get(0);
        BigDecimal total = ventas.stream()
                .map(v -> v.precio().multiply(BigDecimal.valueOf(v.cantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new VentaResponse(primera.transaccionId(), primera.tiendaId(), primera.vendedorId(),
                primera.timestampVenta(), total,
                ventas.stream().map(v -> new Linea(v.id(), v.varianteId(), v.cantidad(), v.precio(), v.canal(),
                        v.forzada(), v.estado().name(), v.anuladaPor(), v.anuladaEn(), v.motivoAnulacion())).toList());
    }
}
