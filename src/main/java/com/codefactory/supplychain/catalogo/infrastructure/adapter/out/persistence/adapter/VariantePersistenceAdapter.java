package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.adapter;

import java.util.UUID;
import com.codefactory.supplychain.catalogo.application.port.out.VarianteRepository;
import com.codefactory.supplychain.catalogo.domain.model.Variante;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity.VarianteEntity;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.mapper.VariantePersistenceMapper;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.repository.VarianteJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida que implementa {@link VarianteRepository}
 * apoyándose en Spring Data JPA.
 * <p>
 * {@code talla} y {@code color} forman parte del dominio {@link Variante},
 * por lo que {@link #save(Variante)} delega directamente en el mapper
 * (igual que {@code TemplatePersistenceAdapter}): ya no es necesario
 * recuperar primero la fila existente para preservar esos campos, porque
 * el propio dominio los trae con el valor correcto en cada guardado
 * (creación o actualización).
 */
@Repository
public class VariantePersistenceAdapter implements VarianteRepository {

    private final VarianteJpaRepository varianteJpaRepository;
    private final VariantePersistenceMapper variantePersistenceMapper;

    public VariantePersistenceAdapter(VarianteJpaRepository varianteJpaRepository,
                                       VariantePersistenceMapper variantePersistenceMapper) {
        this.varianteJpaRepository = varianteJpaRepository;
        this.variantePersistenceMapper = variantePersistenceMapper;
    }

    @Override
    public Variante save(Variante variante) {
        VarianteEntity entity = variantePersistenceMapper.toEntity(variante);
        VarianteEntity saved = varianteJpaRepository.save(entity);
        return variantePersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Variante> findById(UUID id) {
        return varianteJpaRepository.findById(id)
                .map(variantePersistenceMapper::toDomain);
    }

    @Override
    public Optional<Variante> findBySku(String sku) {
        return varianteJpaRepository.findBySku(sku)
                .map(variantePersistenceMapper::toDomain);
    }

    @Override
    public List<Variante> findAll() {
        return varianteJpaRepository.findAll().stream()
                .map(variantePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return varianteJpaRepository.existsById(id);
    }

    @Override
    public boolean existsBySku(String sku) {
        return varianteJpaRepository.existsBySku(sku);
    }

    @Override
    public void deleteById(UUID id) {
        varianteJpaRepository.deleteById(id);
    }
}
