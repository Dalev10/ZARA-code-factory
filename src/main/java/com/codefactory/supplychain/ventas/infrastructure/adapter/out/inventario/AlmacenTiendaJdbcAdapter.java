package com.codefactory.supplychain.ventas.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.ventas.application.port.out.AlmacenTiendaPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * TEMPORAL. Inventario no publica en su api/ cómo resolver el nodo ALMACEN de una tienda (ni el
 * inverso), por lo que se lee la tabla {@code nodo} (V1) en solo lectura. Datos reales, sin simulación.
 * Reemplazar por el contrato de Inventario cuando exista (ver CONTRATOS_PENDIENTES.md, punto 1).
 */
@Component
public class AlmacenTiendaJdbcAdapter implements AlmacenTiendaPort {

    private final JdbcClient jdbc;

    public AlmacenTiendaJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> almacenDeTienda(UUID tiendaId) {
        return jdbc.sql("SELECT id FROM nodo WHERE tienda_id = :tiendaId AND tipo = 'ALMACEN'")
                .param("tiendaId", tiendaId).query(UUID.class).optional();
    }

    @Override
    public Optional<UUID> tiendaDeAlmacen(UUID nodoId) {
        return jdbc.sql("SELECT tienda_id FROM nodo WHERE id = :nodoId AND tipo = 'ALMACEN'")
                .param("nodoId", nodoId).query(UUID.class).optional();
    }
}
