package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.ConsultarRecomendacionesUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.RecomendacionRepository;
import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.reposicion.domain.model.EstadoRecomendacion;
import com.codefactory.supplychain.reposicion.domain.model.Recomendacion;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ConsultarRecomendacionesService implements ConsultarRecomendacionesUseCase {

    private final RecomendacionRepository repository;

    public ConsultarRecomendacionesService(RecomendacionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<Recomendacion> listar(UsuarioActual usuario, EstadoRecomendacion estado, UUID tiendaId,
                                      UUID varianteId, Pageable pageable) {
        if (tiendaId != null && !usuario.puedeOperarEn(tiendaId)) {
            throw ReposicionRecursoNoEncontradoException.tienda(tiendaId);
        }
        boolean todas = usuario.tieneScope(UsuarioActual.SCOPE_OPERAR_TODAS_LAS_TIENDAS);
        if (!todas && tiendaId == null && usuario.tiendasPermitidas().isEmpty()) {
            return Page.empty(pageable);
        }
        return repository.listar(estado, tiendaId, varianteId,
                todas ? null : usuario.tiendasPermitidas(), pageable);
    }

    @Override
    public Recomendacion obtener(UsuarioActual usuario, UUID id) {
        return repository.buscarPorId(id)
                .filter(recomendacion -> usuario.puedeOperarEn(recomendacion.getTiendaId()))
                .orElseThrow(() -> ReposicionRecursoNoEncontradoException.recomendacion(id));
    }
}
