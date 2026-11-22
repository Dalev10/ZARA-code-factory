package com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto;

import java.time.Instant;

/** Cuerpo opcional de la llegada a una parada (HU-48): sin fecha, la del servidor (D-89). */
public record RegistrarLlegadaRequest(Instant fechaLlegada) {
}
