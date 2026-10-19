package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.reposicion.domain.exception.UmbralYaExisteException;
import com.codefactory.supplychain.reposicion.application.port.in.UmbralReposicionUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.UmbralReposicionRepository;
import com.codefactory.supplychain.reposicion.application.port.out.VarianteConsultaPort;
import com.codefactory.supplychain.reposicion.domain.model.UmbralReposicion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Administración de umbrales de reposición interna (HU-47).
 * <p>
 * La variante se valida contra catalogo mediante {@link VarianteConsultaPort}. La
 * categoría no tiene puerto de consulta: si no existe, la FK de la base rechaza el
 * insert y {@code GlobalExceptionHandler} lo traduce a un 409 uniforme.
 */
@Service
@Transactional(readOnly = true)
public class UmbralReposicionService implements UmbralReposicionUseCase {

    private final UmbralReposicionRepository umbralRepository;
    private final VarianteConsultaPort varianteConsulta;

    public UmbralReposicionService(UmbralReposicionRepository umbralRepository,
                                   VarianteConsultaPort varianteConsulta) {
        this.umbralRepository = umbralRepository;
        this.varianteConsulta = varianteConsulta;
    }

    @Override
    @Transactional
    public UmbralReposicion crear(UUID varianteId, UUID categoriaId,
                                  int cantidadMinima, int cantidadObjetivo) {
        // Se construye primero: así el alcance y las cantidades se validan (422) antes
        // de consultar nada.
        UmbralReposicion umbral = UmbralReposicion.crear(
                varianteId, categoriaId, cantidadMinima, cantidadObjetivo);

        if (umbral.esPorVariante()) {
            varianteConsulta.buscar(varianteId)
                    .orElseThrow(() -> ReposicionRecursoNoEncontradoException.variante(varianteId));
            if (umbralRepository.existePorVariante(varianteId)) {
                throw UmbralYaExisteException.paraVariante(varianteId);
            }
        } else if (umbralRepository.existePorCategoria(categoriaId)) {
            throw UmbralYaExisteException.paraCategoria(categoriaId);
        }
        return umbralRepository.guardar(umbral);
    }

    @Override
    public UmbralReposicion obtenerPorId(UUID id) {
        return umbralRepository.buscarPorId(id)
                .orElseThrow(() -> ReposicionRecursoNoEncontradoException.umbral(id));
    }

    @Override
    public Page<UmbralReposicion> listar(Pageable pageable) {
        return umbralRepository.listar(pageable);
    }

    @Override
    @Transactional
    public UmbralReposicion modificar(UUID id, int cantidadMinima, int cantidadObjetivo) {
        UmbralReposicion umbral = obtenerPorId(id);
        return umbralRepository.guardar(umbral.cambiarCantidades(cantidadMinima, cantidadObjetivo));
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        if (!umbralRepository.existePorId(id)) {
            throw ReposicionRecursoNoEncontradoException.umbral(id);
        }
        umbralRepository.eliminarPorId(id);
    }
}
