package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.out.OrdenReabastecimientoRepository;
import com.codefactory.supplychain.reposicion.application.port.out.ReservarStockCdPort;
import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento.Liberacion;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Lógica transaccional de HU-53. Al cerrar con faltante, la orden y la liberación en el
 * inventario van en la misma transacción; el reintento vive en
 * {@link CerrarOrdenRetryableService}.
 *
 * Módulo: reposicion.
 */
@Service
public class CerrarOrdenService {

    private final OrdenReabastecimientoRepository ordenes;
    private final ReservarStockCdPort reservas;
    private final Clock reloj;

    public CerrarOrdenService(OrdenReabastecimientoRepository ordenes, ReservarStockCdPort reservas, Clock reloj) {
        this.ordenes = ordenes;
        this.reservas = reservas;
        this.reloj = reloj;
    }

    @Transactional
    public OrdenReabastecimiento cerrarConFaltante(UsuarioActual usuario, UUID ordenId, String motivo) {
        OrdenReabastecimiento orden = cargar(usuario, ordenId);
        Liberacion liberacion = orden.cerrarConFaltante(motivo, usuario.id(), Instant.now(reloj));
        reservas.liberar(orden.getCdAsignadoId(), orden.getTiendaId(), orden.getVarianteId(),
                liberacion.reservaCd(), liberacion.entranteBodega(), orden.getId());
        return ordenes.guardar(orden);
    }

    @Transactional
    public OrdenReabastecimiento cancelar(UsuarioActual usuario, UUID ordenId, String motivo) {
        OrdenReabastecimiento orden = cargar(usuario, ordenId);
        orden.cancelar(motivo, usuario.id(), Instant.now(reloj));
        return ordenes.guardar(orden);
    }

    private OrdenReabastecimiento cargar(UsuarioActual usuario, UUID ordenId) {
        return ordenes.buscarPorId(ordenId)
                .filter(orden -> usuario.puedeOperarEn(orden.getTiendaId()))
                .orElseThrow(() -> ReposicionRecursoNoEncontradoException.orden(ordenId));
    }
}
