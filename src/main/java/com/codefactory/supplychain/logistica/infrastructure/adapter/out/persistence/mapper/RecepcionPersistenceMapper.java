package com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.logistica.domain.model.Recepcion;
import com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.entity.RecepcionEntity;

/**
 * Traducción explícita entre {@link Recepcion} y {@link RecepcionEntity}.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
public final class RecepcionPersistenceMapper {

    private RecepcionPersistenceMapper() {
    }

    public static RecepcionEntity aEntidad(Recepcion recepcion) {
        return new RecepcionEntity(recepcion.getId(), recepcion.getViajeParadaId(), recepcion.getCicloDespachoId(),
                recepcion.getCantidadRecibida(), recepcion.getDiscrepancia(), recepcion.getRegistradoPor(),
                recepcion.getFechaRecepcion());
    }

    public static Recepcion aDominio(RecepcionEntity entidad) {
        return Recepcion.reconstruir(entidad.getId(), entidad.getViajeParadaId(), entidad.getCicloDespachoId(),
                entidad.getCantidadRecibida(), entidad.getDiscrepancia(), entidad.getRegistradoPor(),
                entidad.getTimestampRecepcion());
    }
}
