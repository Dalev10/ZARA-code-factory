package com.codefactory.supplychain.catalogo.application.service;

import java.util.UUID;
import com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException;
import com.codefactory.supplychain.catalogo.application.port.in.TemplateUseCase;
import com.codefactory.supplychain.catalogo.application.port.out.CategoriaRepository;
import com.codefactory.supplychain.catalogo.application.port.out.TemplateRepository;
import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.catalogo.domain.model.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Servicio de aplicación que implementa los casos de uso de Template
 * (FEAT-05 / HU-15 a HU-18), apoyándose en los puertos de salida
 * {@link TemplateRepository} y {@link CategoriaRepository} (este último
 * únicamente para resolver/comprobar la Categoria asociada). No depende
 * de JPA, de entidades de persistencia, de controllers ni de DTOs.
 */
@Service
@Transactional(readOnly = true)
public class TemplateService implements TemplateUseCase {

    private final TemplateRepository templateRepository;
    private final CategoriaRepository categoriaRepository;

    public TemplateService(TemplateRepository templateRepository,
                            CategoriaRepository categoriaRepository) {
        this.templateRepository = templateRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    @Transactional
    public Template crear(String nombre, String temporada, String proveedor,
                           BigDecimal precioBase, UUID categoriaId) {
        Categoria categoria = obtenerCategoriaExistente(categoriaId);
        Template template = Template.crear(nombre, temporada, proveedor, precioBase, categoria);
        return templateRepository.save(template);
    }

    @Override
    public Template obtenerPorId(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> CatalogoRecursoNoEncontradoException.template(id));
    }

    @Override
    public Page<Template> listar(Pageable pageable) {
        return templateRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Template modificar(UUID id, String nombre, String temporada,
                               String proveedor, BigDecimal precioBase) {
        Template template = obtenerPorId(id);
        return templateRepository.save(template.actualizarInformacion(nombre, temporada, proveedor, precioBase));
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        if (!templateRepository.existsById(id)) {
            throw CatalogoRecursoNoEncontradoException.template(id);
        }
        // Si el Template está referenciado por una o más Variante, el borrado
        // físico viola la FK definida en PostgreSQL (template_id en la tabla
        // variante, NOT NULL); no se implementa borrado lógico. La
        // DataIntegrityViolationException resultante la traduce
        // GlobalExceptionHandler a un 409 uniforme (HU-19).
        templateRepository.deleteById(id);
    }

    private Categoria obtenerCategoriaExistente(UUID categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> CatalogoRecursoNoEncontradoException.categoria(categoriaId));
    }
}
