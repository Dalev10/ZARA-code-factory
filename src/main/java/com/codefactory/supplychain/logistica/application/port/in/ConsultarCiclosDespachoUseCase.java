package com.codefactory.supplychain.logistica.application.port.in;

import com.codefactory.supplychain.logistica.domain.model.CicloDespacho;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Consulta de los ciclos de despacho (HU-46). */
public interface ConsultarCiclosDespachoUseCase {

    /**
     * @param ordenId  solo los de esa orden; nulo para todas
     * @param sinViaje solo los que aún no están en un viaje (los que HU-47 puede cargar)
     */
    Page<CicloDespacho> listar(UUID ordenId, boolean sinViaje, Pageable pageable);

    CicloDespacho obtener(UUID cicloId);
}
