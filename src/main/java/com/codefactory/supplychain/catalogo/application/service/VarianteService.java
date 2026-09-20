package com.codefactory.supplychain.catalogo.application.service;

import java.util.UUID;
import com.codefactory.supplychain.catalogo.application.exception.CatalogoRecursoNoEncontradoException;
import com.codefactory.supplychain.catalogo.application.exception.SkuDuplicadoException;
import com.codefactory.supplychain.catalogo.application.port.in.VarianteUseCase;
import com.codefactory.supplychain.catalogo.application.port.out.TemplateRepository;
import com.codefactory.supplychain.catalogo.application.port.out.VarianteRepository;
import com.codefactory.supplychain.catalogo.domain.model.Template;
import com.codefactory.supplychain.catalogo.domain.model.Variante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación que implementa los casos de uso de Variante/SKU
 * (FEAT-05 / HU-15 a HU-18), apoyándose en los puertos de salida
 * {@link VarianteRepository} y {@link TemplateRepository} (este último
 * únicamente para resolver/comprobar el Template asociado). No depende de
 * JPA, de entidades de persistencia, de controllers ni de DTOs.
 * <p>
 * NOTA sobre {@link #modificar(UUID, String, UUID, String, String)}: el modelo de
 * dominio {@link Variante} es inmutable (no expone setters ni métodos de
 * modificación). Para no rediseñar el dominio ya aprobado, la modificación
 * se implementa reconstituyendo una nueva
 * instancia de {@code Variante} con el mismo id (usando el constructor de
 * reconstitución que el propio dominio ya expone para este fin) y
 * guardándola a través del puerto de salida.
 */
@Service
@Transactional(readOnly = true)
public class VarianteService implements VarianteUseCase {

    private final VarianteRepository varianteRepository;
    private final TemplateRepository templateRepository;

    public VarianteService(VarianteRepository varianteRepository,
                            TemplateRepository templateRepository) {
        this.varianteRepository = varianteRepository;
        this.templateRepository = templateRepository;
    }

    @Override
    @Transactional
    public Variante crear(String sku, UUID templateId, String talla, String color) {
        Template template = obtenerTemplateExistente(templateId);
        rechazarSiSkuYaExiste(sku);
        Variante variante = new Variante(sku, template, talla, color);
        return varianteRepository.save(variante);
    }

    @Override
    public Variante obtenerPorId(UUID id) {
        return varianteRepository.findById(id)
                .orElseThrow(() -> CatalogoRecursoNoEncontradoException.variante(id));
    }

    @Override
    public Variante obtenerPorSku(String sku) {
        return varianteRepository.findBySku(sku)
                .orElseThrow(() -> CatalogoRecursoNoEncontradoException.varianteConSku(sku));
    }

    @Override
    public Page<Variante> listar(Pageable pageable) {
        return varianteRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Variante modificar(UUID id, String nuevoSku, UUID nuevoTemplateId, String nuevaTalla, String nuevoColor) {
        Variante existente = obtenerPorId(id);
        Template template = obtenerTemplateExistente(nuevoTemplateId);
        rechazarSiSkuPerteneceAOtraVariante(existente, nuevoSku);

        Variante actualizada = new Variante(existente.getId(), nuevoSku, template, nuevaTalla, nuevoColor);
        return varianteRepository.save(actualizada);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        if (!varianteRepository.existsById(id)) {
            throw CatalogoRecursoNoEncontradoException.variante(id);
        }
        // La Variante es la unidad operativa (hoja de la jerarquía
        // Categoria -> Template -> Variante), por lo que su borrado
        // físico no debería violar ninguna FK entrante de otra entidad
        // del módulo catalogo en este MVP.
        varianteRepository.deleteById(id);
    }

    private Template obtenerTemplateExistente(UUID templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> CatalogoRecursoNoEncontradoException.template(templateId));
    }

    private void rechazarSiSkuYaExiste(String sku) {
        if (varianteRepository.existsBySku(sku)) {
            throw new SkuDuplicadoException(sku);
        }
    }

    private void rechazarSiSkuPerteneceAOtraVariante(Variante existente, String nuevoSku) {
        if (nuevoSku.equals(existente.getSku())) {
            // El propio sku de la Variante no se considera duplicado de sí misma.
            return;
        }
        if (varianteRepository.existsBySku(nuevoSku)) {
            throw new SkuDuplicadoException(nuevoSku);
        }
    }
}
