package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.catalogo.domain.model.Template;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity.CategoriaEntity;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity.TemplateEntity;
import org.springframework.stereotype.Component;

/**
 * Conversión pura entre {@link Template} (dominio) y
 * {@link TemplateEntity} (persistencia). No contiene lógica de negocio.
 * <p>
 * La Categoria asociada se delega en {@link CategoriaPersistenceMapper}.
 * No se aplica cascada al persistir esta referencia: Hibernate solo
 * utiliza el id de la Categoria para resolver la clave foránea
 * {@code categoria_id}, sin insertar/actualizar la Categoria en sí.
 */
@Component
public class TemplatePersistenceMapper {

    private final CategoriaPersistenceMapper categoriaPersistenceMapper;

    public TemplatePersistenceMapper(CategoriaPersistenceMapper categoriaPersistenceMapper) {
        this.categoriaPersistenceMapper = categoriaPersistenceMapper;
    }

    public TemplateEntity toEntity(Template template) {
        if (template == null) {
            return null;
        }
        CategoriaEntity categoriaEntity = categoriaPersistenceMapper.toEntity(template.getCategoria());
        return new TemplateEntity(
                template.getId(),
                categoriaEntity,
                template.getNombre(),
                template.getTemporada(),
                template.getProveedor(),
                template.getPrecioBase()
        );
    }

    public Template toDomain(TemplateEntity entity) {
        if (entity == null) {
            return null;
        }
        Categoria categoria = categoriaPersistenceMapper.toDomain(entity.getCategoria());
        return new Template(
                entity.getId(),
                entity.getNombre(),
                entity.getTemporada(),
                entity.getProveedor(),
                entity.getPrecioBase(),
                categoria
        );
    }
}
