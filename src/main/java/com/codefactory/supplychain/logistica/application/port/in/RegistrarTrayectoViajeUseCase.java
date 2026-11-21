package com.codefactory.supplychain.logistica.application.port.in;

import com.codefactory.supplychain.logistica.domain.model.Viaje;

import java.time.Instant;
import java.util.UUID;

/**
 * Salida del CD y llegada a cada tienda, registradas a mano (HU-48). La hora puede venir del
 * cliente; si es nula se usa la del servidor (D-89).
 */
public interface RegistrarTrayectoViajeUseCase {

    Viaje registrarSalida(UUID viajeId, Instant fechaSalida);

    Viaje registrarLlegada(UUID viajeId, UUID paradaId, Instant fechaLlegada);
}
