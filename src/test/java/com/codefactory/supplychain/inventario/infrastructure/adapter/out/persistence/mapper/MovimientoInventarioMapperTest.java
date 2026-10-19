package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.inventario.domain.model.Inventario;
import com.codefactory.supplychain.inventario.domain.model.MotivoAjuste;
import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;
import com.codefactory.supplychain.inventario.domain.model.TipoMovimientoInventario;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.MovimientoInventarioEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MovimientoInventarioMapperTest {

    @Test
    void mapeaMovimientoConSuNodoVarianteYFecha() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        UUID usuarioId = UUID.randomUUID();
        MovimientoInventario movimiento = inventario.cargar(7, usuarioId);

        MovimientoInventarioEntity entity = new MovimientoInventarioMapper()
                .toEntity(nodoId, varianteId, movimiento);

        assertThat(entity.getId()).isEqualTo(movimiento.id());
        assertThat(entity.getNodoId()).isEqualTo(nodoId);
        assertThat(entity.getVarianteId()).isEqualTo(varianteId);
        assertThat(entity.getTipo()).isEqualTo(TipoMovimientoInventario.CARGA_INICIAL);
        assertThat(entity.getCantidad()).isEqualTo(7);
        assertThat(entity.getOcurridoEn()).isEqualTo(movimiento.ocurridoEn());
        assertThat(entity.isForzado()).isFalse();
        assertThat(entity.getUsuarioId()).isEqualTo(usuarioId);
    }

    @Test
    void mapeaMotivoYComentarioDeUnAjuste() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(3, null);
        MovimientoInventario movimiento = inventario.aplicarAjuste(
                -1, MotivoAjuste.CORRECCION, "Conteo mal digitado", null);

        MovimientoInventarioEntity entity = new MovimientoInventarioMapper()
                .toEntity(nodoId, varianteId, movimiento);

        assertThat(entity.getMotivo()).isEqualTo("CORRECCION");
        assertThat(entity.getComentario()).isEqualTo("Conteo mal digitado");
    }

    @Test
    void mapeaVentaForzada() {
        UUID nodoId = UUID.randomUUID();
        UUID varianteId = UUID.randomUUID();
        MovimientoInventario movimiento = Inventario.crear(nodoId, varianteId).aplicarVenta(2, true);

        MovimientoInventarioEntity entity = new MovimientoInventarioMapper()
                .toEntity(nodoId, varianteId, movimiento);

        assertThat(entity.getTipo()).isEqualTo(TipoMovimientoInventario.VENTA);
        assertThat(entity.isForzado()).isTrue();
    }
}
