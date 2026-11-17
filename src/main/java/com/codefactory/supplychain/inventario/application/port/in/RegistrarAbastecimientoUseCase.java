package com.codefactory.supplychain.inventario.application.port.in;

import java.util.UUID;

/**
 * Movimientos físicos del abastecimiento CD → Bodega_Tienda (HD-01): el
 * despacho descuenta del CD y la recepción acredita en la Bodega_Tienda.
 *
 * Se une a la transacción del llamador y no reintenta: el reintento ante un
 * conflicto de versión va en el punto de entrada del llamador (D-24).
 */
public interface RegistrarAbastecimientoUseCase {

    void registrarDespacho(UUID cdId, UUID varianteId, int cantidadQueSale, int reservaALiberar, UUID referenciaId);

    void registrarRecepcion(UUID tiendaId, UUID varianteId, int cantidad, UUID referenciaId);
}
