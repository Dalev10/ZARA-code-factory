package com.codefactory.supplychain.ventas.application.port.in;

import java.time.Instant;
import java.util.UUID;

/** HU-45: apertura y cierre automáticos de ventanas de quiebre. Lo invocan los listeners de eventos. */
public interface GestionarVentanasQuiebreUseCase {

    /**
     * Hecho de Inventario traducido al lenguaje de ventas. {@code tiendaId} es la tienda dueña del
     * Almacén; {@code null} si el movimiento no ocurrió en un Almacén (CD o Bodega).
     */
    record ExistenciasCambiadas(UUID tiendaId, UUID varianteId, boolean forzado, int aLaManoAnterior,
                                int aLaMano, Instant ocurridoEn) {
    }

    /** Idempotente: un evento repetido no abre ni cierra dos veces. Los eventos de nodos que no son Almacén se ignoran. */
    void alCambiarExistencias(ExistenciasCambiadas cambio);

    /** Cierra con TIENDA_DESACTIVADA todas las ventanas abiertas de la tienda. Devuelve cuántas cerró. */
    int alDesactivarTienda(UUID tiendaId, Instant fecha);
}
