package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.inventario.api.MovimientoKardex;
import com.codefactory.supplychain.inventario.api.TipoMovimientoKardex;
import com.codefactory.supplychain.inventario.application.dto.MovimientoKardexConsulta;
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
                .referenciaId(movimiento.referenciaId())
                .saldoResultante(movimiento.saldoResultante())
                .ocurridoEn(movimiento.ocurridoEn())
                .build();
    }

    public MovimientoKardexConsulta toConsulta(MovimientoInventarioEntity entity) {
        return new MovimientoKardexConsulta(
                entity.getId(),
                entity.getVarianteId(),
                entity.getTipo(),
                entity.getCantidad(),
                entity.getMotivo(),
                entity.getComentario(),
                entity.isForzado(),
                entity.getUsuarioId(),
                entity.getOcurridoEn());
    }

    public MovimientoKardex toKardex(MovimientoInventarioEntity entity) {
        return new MovimientoKardex(
                entity.getId(),
                entity.getNodoId(),
                entity.getVarianteId(),
                TipoMovimientoKardex.valueOf(entity.getTipo().name()),
                entity.getCantidad(),
                entity.getSaldoResultante(),
                entity.getOcurridoEn());
    }
}
