package com.codefactory.supplychain.logistica.application.service;

import com.codefactory.supplychain.logistica.application.port.in.ConsultarParadasPorRecibirUseCase;
import com.codefactory.supplychain.logistica.application.port.out.ParadasRecepcionPort;
import com.codefactory.supplychain.logistica.domain.model.ParadaRecepcion;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * HU-49: paradas por recibir. Con {@code tiendas:operar-todas} se listan las de todas las
 * tiendas; si no, solo las de las tiendas asignadas al usuario.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
@Service
@RequiredArgsConstructor
public class ConsultarParadasPorRecibirService implements ConsultarParadasPorRecibirUseCase {

    private final ParadasRecepcionPort paradas;

    @Override
    @Transactional(readOnly = true)
    public List<ParadaRecepcion> listar(UsuarioActual actor) {
        if (actor.tieneScope(UsuarioActual.SCOPE_OPERAR_TODAS_LAS_TIENDAS)) {
            return paradas.listarTodasPorRecibir();
        }
        return paradas.listarPorRecibir(actor.tiendasPermitidas());
    }
}
