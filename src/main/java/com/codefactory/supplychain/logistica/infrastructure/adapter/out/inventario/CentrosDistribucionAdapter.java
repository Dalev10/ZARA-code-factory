package com.codefactory.supplychain.logistica.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.inventario.api.ConsultaNodos;
import com.codefactory.supplychain.logistica.application.port.out.CentrosDistribucionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ACL hacia {@code inventario.api}: un CD existe si tiene su nodo (HU-47).
 *
 * Módulo: logistica.
 */
@Component
@RequiredArgsConstructor
class CentrosDistribucionAdapter implements CentrosDistribucionPort {

    private final ConsultaNodos consultaNodos;

    @Override
    public boolean existe(UUID cdId) {
        return consultaNodos.buscarNodoCd(cdId).isPresent();
    }
}
