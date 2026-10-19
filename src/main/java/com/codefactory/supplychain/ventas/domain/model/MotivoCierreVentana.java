package com.codefactory.supplychain.ventas.domain.model;

/** Motivos de cierre de una ventana de quiebre; el motivo histórico puede ser indeterminado. */
public enum MotivoCierreVentana {
    REPOSICION,
    VENTA_FORZADA,
    TIENDA_DESACTIVADA,
    HISTORICO_NO_CLASIFICADO
}
