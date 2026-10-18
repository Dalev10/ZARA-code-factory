package com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.ventas.domain.model.Venta;
import com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.entity.VentaEntity;

public final class VentaMapper {

    private VentaMapper() {
    }

    public static VentaEntity toEntity(Venta v) {
        return VentaEntity.builder().id(v.id()).transaccionId(v.transaccionId()).varianteId(v.varianteId())
                .tiendaId(v.tiendaId()).cantidad(v.cantidad()).precio(v.precio()).canal(v.canal())
                .origen(v.origen()).vendedorId(v.vendedorId()).forzada(v.forzada()).estado(v.estado())
                .timestampVenta(v.timestampVenta()).anuladaPor(v.anuladaPor()).anuladaEn(v.anuladaEn())
                .motivoAnulacion(v.motivoAnulacion()).build();
    }

    public static Venta toDomain(VentaEntity e) {
        return new Venta(e.getId(), e.getTransaccionId(), e.getVarianteId(), e.getTiendaId(), e.getCantidad(),
                e.getPrecio(), e.getCanal(), e.getOrigen(), e.getVendedorId(), e.isForzada(), e.getEstado(),
                e.getTimestampVenta(), e.getAnuladaPor(), e.getAnuladaEn(), e.getMotivoAnulacion());
    }
}
