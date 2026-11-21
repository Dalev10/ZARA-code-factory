package com.codefactory.supplychain.soporte;

import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.logistica.application.port.in.DespacharOrdenUseCase;
import com.codefactory.supplychain.reposicion.application.port.in.CrearOrdenUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/**
 * Arma la red del abastecimiento CD → tienda para las pruebas de logística (HU-47, HU-48, HD-06):
 * variante, CD con stock, tienda con Almacén y Bodega y prioridad hacia el CD, órdenes creadas
 * con HU-45 y despachos con HU-46. Uso: {@code @Import(AbastecimientoDePrueba.class)}.
 */
@TestComponent
@RequiredArgsConstructor
public class AbastecimientoDePrueba {

    private final JdbcTemplate jdbc;
    private final InventarioUseCase inventario;
    private final CrearOrdenUseCase crearOrden;
    private final DespacharOrdenUseCase despacharOrden;
    private final TransactionTemplate tx;

    public UUID crearVariante() {
        UUID categoriaId = UUID.randomUUID();
        jdbc.update("INSERT INTO categoria (id, nombre) VALUES (?, ?)", categoriaId, "Categoría " + categoriaId);
        UUID templateId = UUID.randomUUID();
        jdbc.update("INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)",
                templateId, categoriaId, "Template " + templateId);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)", id, templateId, "SKU-" + id);
        return id;
    }

    /** CD con {@code stock} unidades a la mano de la variante. */
    public UUID crearCd(UUID varianteId, int stock) {
        UUID cdId = UUID.randomUUID();
        jdbc.update("INSERT INTO cd (id, nombre, ubicacion) VALUES (?, ?, 'Bogotá')", cdId, "CD " + cdId);
        UUID nodo = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, cd_id) VALUES (?, 'CD', ?)", nodo, cdId);
        inventario.cargar(nodo, varianteId, stock, null);
        return cdId;
    }

    /** Tienda con Almacén y Bodega que se abastece del CD con prioridad 1. */
    public UUID crearTienda(UUID cdId) {
        UUID tiendaId = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')", tiendaId, "Tienda " + tiendaId);
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'ALMACEN', ?)", UUID.randomUUID(), tiendaId);
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'BODEGA_TIENDA', ?)", UUID.randomUUID(), tiendaId);
        jdbc.update("INSERT INTO tienda_cd_prioridad (tienda_id, cd_id, orden_prioridad) VALUES (?, ?, 1)", tiendaId, cdId);
        return tiendaId;
    }

    /** Orden APROBADA con reserva en el CD (HU-45). */
    public UUID crearOrden(UUID tiendaId, UUID varianteId, int cantidad) {
        return tx.execute(estado -> crearOrden.crear(null, tiendaId, varianteId, cantidad)).getId();
    }

    /** Despacha la orden con HU-46 y devuelve el id del ciclo. */
    public UUID despachar(UUID ordenId) {
        return despacharOrden.despachar(ordenId).getId();
    }

    /** Orden creada y despachada: el ciclo queda listo para un viaje. */
    public UUID cicloListo(UUID tiendaId, UUID varianteId, int cantidad) {
        return despachar(crearOrden(tiendaId, varianteId, cantidad));
    }

    public String estadoOrden(UUID ordenId) {
        return jdbc.queryForObject("SELECT estado FROM orden_reabastecimiento WHERE id = ?", String.class, ordenId);
    }

    public UUID ordenDelCiclo(UUID cicloId) {
        return jdbc.queryForObject("SELECT orden_id FROM ciclo_despacho WHERE id = ?", UUID.class, cicloId);
    }
}
