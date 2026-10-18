package com.codefactory.supplychain.ventas.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.ventas.application.port.out.VarianteCatalogoPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * TEMPORAL. Catálogo todavía no tiene {@code api/} (ARQUITECTURA.md §5, fila HU-37), así que la
 * existencia de la variante se comprueba leyendo {@code variante} (V1) en solo lectura.
 * Reemplazar por la consulta de variantes de catalogo/api cuando se publique.
 */
@Component
public class VarianteCatalogoJdbcAdapter implements VarianteCatalogoPort {

    private final JdbcClient jdbc;

    public VarianteCatalogoJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existe(UUID varianteId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM variante WHERE id = :id)")
                .param("id", varianteId).query(Boolean.class).single();
    }
}
