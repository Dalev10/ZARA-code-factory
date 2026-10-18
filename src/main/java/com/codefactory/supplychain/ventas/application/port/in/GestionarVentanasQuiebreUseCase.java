package com.codefactory.supplychain.ventas.application.port.in;

import java.time.Instant;
import java.util.UUID;

/** HU-36: apertura y cierre automáticos de ventanas de quiebre. Lo invocan los listeners de eventos. */
public interface GestionarVentanasQuiebreUseCase {

    /**
     * Hecho de Inventario traducido al lenguaje de ventas. {@code tipoMovimiento} es el nombre del tipo
     * de movimiento de kardex (CARGA_INICIAL, VENTA, AJUSTE, ...).
     */
    record ExistenciasCambiadas(UUID nodoId, UUID varianteId, String tipoMovimiento, boolean forzado,
                                int aLaMano, Instant ocurridoEn) {
    }

    /** Idempotente: un evento repetido no abre ni cierra dos veces. Los eventos de nodos que no son Almacén se ignoran. */
    void alCambiarExistencias(ExistenciasCambiadas cambio);

    /** Cierra con TIENDA_DESACTIVADA todas las ventanas abiertas de la tienda. Devuelve cuántas cerró. */
    int alDesactivarTienda(UUID tiendaId, Instant fecha);
}
