package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity.CategoriaEntity;
import org.springframework.stereotype.Component;

/**
 * Conversión pura entre {@link Categoria} (dominio) y
 * {@link CategoriaEntity} (persistencia). No contiene lógica de negocio.
 */
@Component
public class CategoriaPersistenceMapper {

    public CategoriaEntity toEntity(Categoria categoria) {
        if (categoria == null) {
            return null;
        }
        return new CategoriaEntity(categoria.getId(), categoria.getNombre());
    }

    public Categoria toDomain(CategoriaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Categoria.reconstruir(entity.getId(), entity.getNombre());
    }
}
