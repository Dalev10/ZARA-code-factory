package com.codefactory.supplychain.catalogo.application.service;

import java.util.UUID;
import com.codefactory.supplychain.catalogo.application.exception.DatosInvalidosException;
import com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException;
import com.codefactory.supplychain.catalogo.application.port.in.CategoriaUseCase;
import com.codefactory.supplychain.catalogo.application.port.out.CategoriaRepository;
import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicación que implementa los casos de uso de Categoria
 * (FEAT-05 / HU-15 a HU-18), apoyándose exclusivamente en el puerto de
 * salida {@link CategoriaRepository}. No depende de JPA, de entidades de
 * persistencia, de controllers ni de DTOs.
 */
@Service
@Transactional(readOnly = true)
public class CategoriaService implements CategoriaUseCase {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    @Transactional
    public Categoria crear(String nombre) {
        validarNombre(nombre);
        return categoriaRepository.save(new Categoria(nombre));
    }

    @Override
    public Categoria obtenerPorId(UUID id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> CatalogoRecursoNoEncontradoException.categoria(id));
    }

    @Override
    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    @Override
    @Transactional
    public Categoria modificar(UUID id, String nuevoNombre) {
        validarNombre(nuevoNombre);
        Categoria categoria = obtenerPorId(id);
        categoria.cambiarNombre(nuevoNombre);
        return categoriaRepository.save(categoria);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        if (!categoriaRepository.existsById(id)) {
            throw CatalogoRecursoNoEncontradoException.categoria(id);
        }
        // Si la Categoria está referenciada por uno o más Template, el borrado
        // físico viola la FK definida en PostgreSQL (categoria_id en la tabla
        // template); no se implementa borrado lógico. La DataIntegrityViolationException
        // resultante la traduce GlobalExceptionHandler a un 409 uniforme (HU-19).
        categoriaRepository.deleteById(id);
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El nombre de la Categoria no puede ser vacío o null");
        }
    }
}
