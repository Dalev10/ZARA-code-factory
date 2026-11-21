package com.codefactory.supplychain.logistica.application.service;

import com.codefactory.supplychain.logistica.application.port.in.ConsultarCiclosDespachoUseCase;
import com.codefactory.supplychain.logistica.application.port.out.CicloDespachoRepository;
import com.codefactory.supplychain.logistica.domain.exception.CicloDespachoNoEncontradoException;
import com.codefactory.supplychain.logistica.domain.model.CicloDespacho;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Consulta de ciclos de despacho (HU-46). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultarCiclosDespachoService implements ConsultarCiclosDespachoUseCase {

    private final CicloDespachoRepository ciclos;

    @Override
    public Page<CicloDespacho> listar(UUID ordenId, boolean sinViaje, Pageable pageable) {
        return ciclos.listar(ordenId, sinViaje, pageable);
    }

    @Override
    public CicloDespacho obtener(UUID cicloId) {
        return ciclos.buscarPorId(cicloId).orElseThrow(() -> new CicloDespachoNoEncontradoException(cicloId));
    }
}
