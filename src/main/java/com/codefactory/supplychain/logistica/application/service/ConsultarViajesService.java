package com.codefactory.supplychain.logistica.application.service;

import com.codefactory.supplychain.logistica.application.port.in.ConsultarViajesUseCase;
import com.codefactory.supplychain.logistica.application.port.out.ViajeRepository;
import com.codefactory.supplychain.logistica.domain.exception.ViajeNoEncontradoException;
import com.codefactory.supplychain.logistica.domain.model.EstadoViaje;
import com.codefactory.supplychain.logistica.domain.model.Viaje;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Consulta de viajes (HU-47). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultarViajesService implements ConsultarViajesUseCase {

    private final ViajeRepository viajes;

    @Override
    public Page<Viaje> listar(EstadoViaje estado, Pageable pageable) {
        return viajes.listar(estado, pageable);
    }

    @Override
    public Viaje obtener(UUID viajeId) {
        return viajes.buscarPorId(viajeId).orElseThrow(() -> new ViajeNoEncontradoException(viajeId));
    }
}
