package com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.logistica.application.port.out.ParadasRecepcionPort;
import com.codefactory.supplychain.logistica.domain.model.ParadaRecepcion;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Lectura de {@code viaje_parada} y {@code ciclo_despacho} para la recepción (HU-49).
 * <p>
 * Consulta las tablas con SQL de solo lectura, sin entidades JPA propias: las entidades de la
 * parada, el viaje y el ciclo las crean HU-46 y HU-47 en este mismo módulo, y así no se
 * duplican. Solo usa columnas que existen desde V1. Cuando esas entidades lleguen, este
 * adaptador puede pasar a usar sus repositorios sin que cambie el puerto.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
@Repository
@RequiredArgsConstructor
public class ParadasRecepcionPersistenceAdapter implements ParadasRecepcionPort {

    private static final String SELECT_PARADA = """
            SELECT vp.id, vp.viaje_id, vp.tienda_id, vp.ciclo_despacho_id, cd.orden_id,
                   cd.cantidad_despachada, vp.estado
            FROM viaje_parada vp
            JOIN ciclo_despacho cd ON cd.id = vp.ciclo_despacho_id
            """;

    private static final String POR_RECIBIR = """
            WHERE vp.estado = 'ENTREGADO'
              AND NOT EXISTS (SELECT 1 FROM recepcion r WHERE r.viaje_parada_id = vp.id)
            """;

    private static final String ORDEN = " ORDER BY vp.viaje_id, vp.orden_parada";

    private static final RowMapper<ParadaRecepcion> MAPPER = (rs, fila) -> new ParadaRecepcion(
            rs.getObject("id", UUID.class),
            rs.getObject("viaje_id", UUID.class),
            rs.getObject("tienda_id", UUID.class),
            rs.getObject("ciclo_despacho_id", UUID.class),
            rs.getObject("orden_id", UUID.class),
            rs.getInt("cantidad_despachada"),
            "ENTREGADO".equals(rs.getString("estado")));

    private final JdbcClient jdbc;

    @Override
    public Optional<ParadaRecepcion> buscar(UUID paradaId) {
        return jdbc.sql(SELECT_PARADA + " WHERE vp.id = :id")
                .param("id", paradaId)
                .query(MAPPER)
                .optional();
    }

    @Override
    public List<ParadaRecepcion> listarPorRecibir(Set<UUID> tiendas) {
        if (tiendas.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(SELECT_PARADA + POR_RECIBIR + " AND vp.tienda_id IN (:tiendas)" + ORDEN)
                .param("tiendas", tiendas)
                .query(MAPPER)
                .list();
    }

    @Override
    public List<ParadaRecepcion> listarTodasPorRecibir() {
        return jdbc.sql(SELECT_PARADA + POR_RECIBIR + ORDEN)
                .query(MAPPER)
                .list();
    }
}
