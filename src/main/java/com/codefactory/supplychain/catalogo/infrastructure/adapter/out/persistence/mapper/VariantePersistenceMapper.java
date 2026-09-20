package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.catalogo.domain.model.Template;
import com.codefactory.supplychain.catalogo.domain.model.Variante;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity.TemplateEntity;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity.VarianteEntity;
import org.springframework.stereotype.Component;

/**
 * Conversión pura entre {@link Variante} (dominio) y
 * {@link VarianteEntity} (persistencia). No contiene lógica de negocio.
 * <p>
 * {@code talla} y {@code color} forman parte del dominio {@link Variante}
 * y se traducen en ambos sentidos igual que el resto de los campos.
 */
@Component
public class VariantePersistenceMapper {

    private final TemplatePersistenceMapper templatePersistenceMapper;

    public VariantePersistenceMapper(TemplatePersistenceMapper templatePersistenceMapper) {
        this.templatePersistenceMapper = templatePersistenceMapper;
    }

    public VarianteEntity toEntity(Variante variante) {
        if (variante == null) {
            return null;
        }
        TemplateEntity templateEntity = templatePersistenceMapper.toEntity(variante.getTemplate());
        return new VarianteEntity(
                variante.getId(),
                templateEntity,
                variante.getTalla(),
                variante.getColor(),
                variante.getSku()
        );
    }

    public Variante toDomain(VarianteEntity entity) {
        if (entity == null) {
            return null;
        }
        Template template = templatePersistenceMapper.toDomain(entity.getTemplate());
        return new Variante(entity.getId(), entity.getSku(), template, entity.getTalla(), entity.getColor());
    }
}
