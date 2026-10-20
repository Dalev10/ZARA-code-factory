package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.ConsultarTraspasosUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.TraspasoInternoRepository;
import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.reposicion.domain.exception.TiendaNoPermitidaException;
import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: reposicion — Reposición interna.
 */
@Service
@Transactional(readOnly = true)
public class ConsultarTraspasosService implements ConsultarTraspasosUseCase {

    private final TraspasoInternoRepository repository;

    public ConsultarTraspasosService(TraspasoInternoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TraspasoInterno> listarPorTienda(UsuarioActual usuario, UUID tiendaId) {
        if (!usuario.puedeOperarEn(tiendaId)) {
            throw new TiendaNoPermitidaException(tiendaId);
        }
        return repository.listarPorTienda(tiendaId);
    }

    @Override
    public TraspasoInterno obtener(UsuarioActual usuario, UUID id) {
        return repository.buscarPorId(id)
                .filter(traspaso -> usuario.puedeOperarEn(traspaso.getTiendaId()))
                .orElseThrow(() -> ReposicionRecursoNoEncontradoException.traspaso(id));
    }
}
