package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.ConsultarOrdenesUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.OrdenReabastecimientoRepository;
import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.reposicion.domain.model.EstadoOrden;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Consulta de órdenes. Fuera del alcance del usuario, 404.
 */
@Service
@Transactional(readOnly = true)
public class ConsultarOrdenesService implements ConsultarOrdenesUseCase {

    private final OrdenReabastecimientoRepository repository;

    public ConsultarOrdenesService(OrdenReabastecimientoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<OrdenReabastecimiento> listar(UsuarioActual usuario, EstadoOrden estado, UUID tiendaId,
                                              Pageable pageable) {
        if (tiendaId != null && !usuario.puedeOperarEn(tiendaId)) {
            throw ReposicionRecursoNoEncontradoException.tienda(tiendaId);
        }
        boolean todas = usuario.tieneScope(UsuarioActual.SCOPE_OPERAR_TODAS_LAS_TIENDAS);
        if (!todas && tiendaId == null && usuario.tiendasPermitidas().isEmpty()) {
            return Page.empty(pageable);
        }
        return repository.listar(estado, tiendaId, todas ? null : usuario.tiendasPermitidas(), pageable);
    }

    @Override
    public OrdenReabastecimiento obtener(UsuarioActual usuario, UUID id) {
        return repository.buscarPorId(id)
                .filter(orden -> usuario.puedeOperarEn(orden.getTiendaId()))
                .orElseThrow(() -> ReposicionRecursoNoEncontradoException.orden(id));
    }

    @Override
    public java.util.Optional<OrdenReabastecimiento> buscar(UUID id) {
        return repository.buscarPorId(id);
    }
}
