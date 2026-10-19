package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.reposicion.application.port.out.ConsultarNodosPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Resuelve nodos de una tienda leyendo la tabla {@code nodo} (V1) directamente con JDBC,
 * igual que {@code AlmacenTiendaJdbcAdapter} en ventas.
 *
 * Módulo: reposicion.
 */
@Component
public class NodoTiendaJdbcAdapter implements ConsultarNodosPort {

    private final JdbcClient jdbc;

    public NodoTiendaJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> buscarNodoAlmacen(UUID tiendaId) {
        return jdbc.sql("SELECT id FROM nodo WHERE tienda_id = :tiendaId AND tipo = 'ALMACEN'")
                .param("tiendaId", tiendaId)
                .query(UUID.class)
                .optional();
    }

    @Override
    public Optional<UUID> buscarNodoBodega(UUID tiendaId) {
        return jdbc.sql("SELECT id FROM nodo WHERE tienda_id = :tiendaId AND tipo = 'BODEGA_TIENDA'")
                .param("tiendaId", tiendaId)
                .query(UUID.class)
                .optional();
    }

    @Override
    public Optional<UUID> buscarTiendaPorNodoAlmacen(UUID nodoId) {
        return jdbc.sql("SELECT tienda_id FROM nodo WHERE id = :nodoId AND tipo = 'ALMACEN'")
                .param("nodoId", nodoId)
                .query(UUID.class)
                .optional();
    }
}
