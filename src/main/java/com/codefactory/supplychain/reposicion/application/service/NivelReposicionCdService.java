package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.NivelReposicionCdUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.NivelReposicionCdRepository;
import com.codefactory.supplychain.reposicion.application.port.out.VarianteConsultaPort;
import com.codefactory.supplychain.reposicion.domain.exception.NivelReposicionYaExisteException;
import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.reposicion.domain.model.NivelReposicionCd;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Administración de los niveles de reposición al CD (HD-04). Mismas reglas que los umbrales:
 * la variante se valida contra catalogo; una categoría inexistente la rechaza la FK (409).
 */
@Service
@Transactional(readOnly = true)
public class NivelReposicionCdService implements NivelReposicionCdUseCase {

    private final NivelReposicionCdRepository nivelRepository;
    private final VarianteConsultaPort varianteConsulta;

    public NivelReposicionCdService(NivelReposicionCdRepository nivelRepository,
                                    VarianteConsultaPort varianteConsulta) {
        this.nivelRepository = nivelRepository;
        this.varianteConsulta = varianteConsulta;
    }

    @Override
    @Transactional
    public NivelReposicionCd crear(UUID varianteId, UUID categoriaId, int cantidadMinima, int cantidadObjetivo) {
        // Se construye primero para validar alcance y cantidades antes de consultar nada.
        NivelReposicionCd nivel = NivelReposicionCd.crear(varianteId, categoriaId, cantidadMinima, cantidadObjetivo);
        if (nivel.esPorVariante()) {
            varianteConsulta.buscar(varianteId)
                    .orElseThrow(() -> ReposicionRecursoNoEncontradoException.variante(varianteId));
            if (nivelRepository.existePorVariante(varianteId)) {
                throw NivelReposicionYaExisteException.paraVariante(varianteId);
            }
        } else if (nivelRepository.existePorCategoria(categoriaId)) {
            throw NivelReposicionYaExisteException.paraCategoria(categoriaId);
        }
        return nivelRepository.guardar(nivel);
    }

    @Override
    public NivelReposicionCd obtenerPorId(UUID id) {
        return nivelRepository.buscarPorId(id).orElseThrow(() -> ReposicionRecursoNoEncontradoException.nivel(id));
    }

    @Override
    public Page<NivelReposicionCd> listar(Pageable pageable) {
        return nivelRepository.listar(pageable);
    }

    @Override
    @Transactional
    public NivelReposicionCd modificar(UUID id, int cantidadMinima, int cantidadObjetivo) {
        return nivelRepository.guardar(obtenerPorId(id).cambiarCantidades(cantidadMinima, cantidadObjetivo));
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        if (!nivelRepository.existePorId(id)) {
            throw ReposicionRecursoNoEncontradoException.nivel(id);
        }
        nivelRepository.eliminarPorId(id);
    }
}
