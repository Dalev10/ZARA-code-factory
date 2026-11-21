package com.codefactory.supplychain.logistica.application.port.in;

import com.codefactory.supplychain.logistica.domain.model.CicloDespacho;

import java.util.UUID;

/** Despacha un ciclo de una orden APROBADA o REABIERTA con la disponibilidad real del CD (HU-46). */
public interface DespacharOrdenUseCase {

    CicloDespacho despachar(UUID ordenId);
}
