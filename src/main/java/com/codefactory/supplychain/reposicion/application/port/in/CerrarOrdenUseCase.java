package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import com.codefactory.supplychain.shared.security.UsuarioActual;

import java.util.UUID;

/**
 * Terminar una orden antes de recibirla completa (HU-53). Solo sobre las tiendas del usuario
 * (fuera de alcance, 404).
 */
public interface CerrarOrdenUseCase {

    /** REABIERTA → CERRADA, liberando la reserva del CD y el entrante pendiente de la Bodega. */
    OrdenReabastecimiento cerrarConFaltante(UsuarioActual usuario, UUID ordenId, String motivo);

    /** GENERADA (sin CD) → CANCELADA. */
    OrdenReabastecimiento cancelar(UsuarioActual usuario, UUID ordenId, String motivo);
}
