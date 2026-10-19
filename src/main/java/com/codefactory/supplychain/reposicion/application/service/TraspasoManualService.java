package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.TraspasoManualUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.EjecutarTraspasoPort;
import com.codefactory.supplychain.reposicion.application.port.out.TraspasoInternoRepository;
import com.codefactory.supplychain.reposicion.application.port.out.VarianteConsultaPort;
import com.codefactory.supplychain.reposicion.domain.exception.BodegaInsuficienteException;
import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.reposicion.domain.exception.TiendaNoPermitidaException;
import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Registra un traspaso manual. Inventario valida el stock de la Bodega en el mismo
 * movimiento, así que no hay una lectura previa que pueda quedar desactualizada.
 *
 * Módulo: reposicion — Reposición interna.
 */
@Service
public class TraspasoManualService implements TraspasoManualUseCase {

    private final VarianteConsultaPort variantes;
    private final EjecutarTraspasoPort ejecutarTraspaso;
    private final TraspasoInternoRepository traspasoRepository;

    public TraspasoManualService(VarianteConsultaPort variantes,
                                 EjecutarTraspasoPort ejecutarTraspaso,
                                 TraspasoInternoRepository traspasoRepository) {
        this.variantes = variantes;
        this.ejecutarTraspaso = ejecutarTraspaso;
        this.traspasoRepository = traspasoRepository;
    }

    @Override
    @Transactional
    public TraspasoInterno registrar(UsuarioActual usuario, UUID tiendaId, UUID varianteId, int cantidad) {
        if (!usuario.puedeOperarEn(tiendaId)) {
            throw new TiendaNoPermitidaException(tiendaId);
        }
        if (variantes.buscar(varianteId).isEmpty()) {
            throw ReposicionRecursoNoEncontradoException.variante(varianteId);
        }
        TraspasoInterno traspaso = TraspasoInterno.registrarManual(varianteId, tiendaId, cantidad, usuario.id());
        if (!ejecutarTraspaso.traspasar(tiendaId, varianteId, cantidad, traspaso.getId())) {
            throw new BodegaInsuficienteException(varianteId, cantidad);
        }
        return traspasoRepository.guardar(traspaso);
    }
}
