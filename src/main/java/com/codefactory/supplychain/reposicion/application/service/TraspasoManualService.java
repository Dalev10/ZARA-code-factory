package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.domain.exception.BodegaInsuficienteException;
import com.codefactory.supplychain.reposicion.application.port.in.TraspasoManualUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarExistenciasPort;
import com.codefactory.supplychain.reposicion.application.port.out.EjecutarTraspasoPort;
import com.codefactory.supplychain.reposicion.application.port.out.TraspasoInternoRepository;
import com.codefactory.supplychain.reposicion.domain.model.OrigenTraspaso;
import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Registra un traspaso manual con validación de stock.
 * <p>
 * Flujo:
 * <ol>
 *   <li>Comprueba que la Bodega tiene suficiente stock (nunca deja negativo).</li>
 *   <li>Crea el objeto de dominio con origen MANUAL y el registrador del token.</li>
 *   <li>Aplica el movimiento contable en inventario.</li>
 *   <li>Persiste el traspaso.</li>
 * </ol>
 *
 * Módulo: reposicion — Reposición interna.
 */
@Service
@Transactional
public class TraspasoManualService implements TraspasoManualUseCase {

    private final ConsultarExistenciasPort consultarExistencias;
    private final EjecutarTraspasoPort ejecutarTraspaso;
    private final TraspasoInternoRepository traspasoRepository;

    public TraspasoManualService(ConsultarExistenciasPort consultarExistencias,
                                  EjecutarTraspasoPort ejecutarTraspaso,
                                  TraspasoInternoRepository traspasoRepository) {
        this.consultarExistencias = consultarExistencias;
        this.ejecutarTraspaso = ejecutarTraspaso;
        this.traspasoRepository = traspasoRepository;
    }

    @Override
    public TraspasoInterno registrar(UUID tiendaId, UUID varianteId,
                                      int cantidad, UUID registradoPor) {
        int bodega = consultarExistencias.consultarStockBodega(tiendaId, varianteId).orElse(0);
        if (bodega < cantidad) {
            throw new BodegaInsuficienteException(tiendaId, varianteId, bodega, cantidad);
        }

        TraspasoInterno traspaso = TraspasoInterno.reconstruir(
                UUID.randomUUID(), varianteId, tiendaId, cantidad,
                OrigenTraspaso.MANUAL, registradoPor, Instant.now());

        // Aplicar movimiento en inventario y persistir
        ejecutarTraspaso.traspasar(tiendaId, varianteId, cantidad, traspaso.getId());
        return traspasoRepository.guardar(traspaso);
    }
}
