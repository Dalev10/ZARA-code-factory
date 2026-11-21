package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.reposicion.domain.model.IntentoCd;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity.IntentoCdEntity;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity.OrdenReabastecimientoEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Convierte entre {@link OrdenReabastecimiento} (dominio) y su entidad JPA, con los intentos de CD.
 *
 * Módulo: reposicion.
 */
@Component
public class OrdenReabastecimientoPersistenceMapper {

    public OrdenReabastecimientoEntity toEntity(OrdenReabastecimiento orden) {
        return OrdenReabastecimientoEntity.builder()
                .id(orden.getId())
                .recomendacionId(orden.getRecomendacionId())
                .tiendaId(orden.getTiendaId())
                .varianteId(orden.getVarianteId())
                .cdAsignadoId(orden.getCdAsignadoId())
                .motivoAsignacion(orden.getMotivoAsignacion())
                .cantidadTotalSolicitada(orden.getCantidadTotalSolicitada())
                .cantidadPendiente(orden.getCantidadPendiente())
                .cantidadDespachadaAcumulada(orden.getCantidadDespachadaAcumulada())
                .estado(orden.getEstado())
                .fechaCreacion(orden.getFechaCreacion())
                .motivoCierre(orden.getMotivoCierre())
                .cerradaPor(orden.getCerradaPor())
                .fechaCierre(orden.getFechaCierre())
                .version(orden.getVersion())
                .intentos(new ArrayList<>(orden.getIntentos().stream().map(this::toEntity).toList()))
                .build();
    }

    public OrdenReabastecimiento toDomain(OrdenReabastecimientoEntity entity) {
        return OrdenReabastecimiento.reconstruir(
                entity.getId(),
                entity.getRecomendacionId(),
                entity.getTiendaId(),
                entity.getVarianteId(),
                entity.getCdAsignadoId(),
                entity.getMotivoAsignacion(),
                entity.getCantidadTotalSolicitada(),
                entity.getCantidadPendiente(),
                entity.getCantidadDespachadaAcumulada(),
                entity.getEstado(),
                entity.getFechaCreacion(),
                entity.getVersion(),
                entity.getIntentos().stream().map(this::toDomain).toList(),
                entity.getMotivoCierre() == null ? null
                        : new OrdenReabastecimiento.Cierre(entity.getMotivoCierre(), entity.getCerradaPor(),
                                entity.getFechaCierre()));
    }

    private IntentoCdEntity toEntity(IntentoCd intento) {
        return IntentoCdEntity.builder()
                .id(intento.id())
                .cdId(intento.cdId())
                .ordenEvaluado(intento.ordenEvaluado())
                .resultado(intento.resultado())
                .motivo(intento.motivo())
                .timestampIntento(intento.timestamp())
                .build();
    }

    private IntentoCd toDomain(IntentoCdEntity entity) {
        return new IntentoCd(entity.getId(), entity.getCdId(), entity.getOrdenEvaluado(), entity.getResultado(),
                entity.getMotivo(), entity.getTimestampIntento());
    }
}
