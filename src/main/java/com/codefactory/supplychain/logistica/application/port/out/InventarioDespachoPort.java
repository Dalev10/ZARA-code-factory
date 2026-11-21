package com.codefactory.supplychain.logistica.application.port.out;

import java.util.UUID;

/** Lectura y salida de stock del CD para el despacho. Respaldado por {@code inventario.api}. */
public interface InventarioDespachoPort {

    /** {@code a_la_mano} de la variante en el CD; 0 si no tiene fila. */
    int aLaManoEnCd(UUID cdId, UUID varianteId);

    /** Descuenta lo que sale y libera hasta {@code reservaALiberar} de la reserva, con kardex DESPACHO. */
    void registrarDespacho(UUID cdId, UUID varianteId, int cantidadQueSale, int reservaALiberar, UUID cicloId);
}
