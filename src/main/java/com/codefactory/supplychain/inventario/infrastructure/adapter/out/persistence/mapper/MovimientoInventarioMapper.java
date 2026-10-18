package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.MovimientoInventarioEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Traduce movimientos del dominio a filas del kardex.
 */
@Component
public class MovimientoInventarioMapper {

    public MovimientoInventarioEntity toEntity(
            UUID nodoId,
            UUID varianteId,
            MovimientoInventario movimiento) {
        return MovimientoInventarioEntity.builder()
                .id(movimiento.id())
                .nodoId(nodoId)
                .varianteId(varianteId)
                .tipo(movimiento.tipo())
                .cantidad(movimiento.cantidad())
                .motivo(movimiento.motivo())
                .comentario(movimiento.comentario())
                .forzado(movimiento.forzado())
                .usuarioId(movimiento.usuarioId())
                .ocurridoEn(movimiento.ocurridoEn())
                .build();
    }
}
