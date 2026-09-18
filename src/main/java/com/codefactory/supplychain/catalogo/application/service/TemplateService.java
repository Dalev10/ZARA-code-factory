package com.codefactory.supplychain.catalogo.application.service;

import com.codefactory.supplychain.catalogo.application.exception.RecursoNoEncontradoException;
import com.codefactory.supplychain.catalogo.application.port.in.TemplateUseCase;
import com.codefactory.supplychain.catalogo.application.port.out.CategoriaRepository;
import com.codefactory.supplychain.catalogo.application.port.out.TemplateRepository;
import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.catalogo.domain.model.Template;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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
                           BigDecimal precioBase, Long categoriaId) {
        Categoria categoria = obtenerCategoriaExistente(categoriaId);
        Template template = new Template(nombre, temporada, proveedor, precioBase, categoria);
        return templateRepository.save(template);
    }

    @Override
    public Template obtenerPorId(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.template(id));
    }

    @Override
    public List<Template> listar() {
        return templateRepository.findAll();
    }

    @Override
    @Transactional
    public Template modificar(Long id, String nombre, String temporada,
                               String proveedor, BigDecimal precioBase) {
        Template template = obtenerPorId(id);
        template.actualizarInformacion(nombre, temporada, proveedor, precioBase);
        return templateRepository.save(template);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!templateRepository.existsById(id)) {
            throw RecursoNoEncontradoException.template(id);
        }
        // NOTA: si el Template está referenciado por una o más Variante,
        // el borrado físico puede violar la FK definida en PostgreSQL
        // (template_id en la tabla variante, NOT NULL). No se implementa
        // borrado lógico en esta etapa (ver PERSISTENCE_NOTES.md); si el
        // repositorio actual no permite resolver este caso, la excepción
        // de integridad referencial se propagará tal cual desde la capa
        // de persistencia. Documentado en lugar de resuelto silenciosamente.
        templateRepository.deleteById(id);
    }

    private Categoria obtenerCategoriaExistente(Long categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> RecursoNoEncontradoException.categoria(categoriaId));
    }
}
