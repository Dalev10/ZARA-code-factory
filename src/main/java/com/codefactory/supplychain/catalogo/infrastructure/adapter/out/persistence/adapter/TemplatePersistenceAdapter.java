package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.adapter;

import java.util.UUID;
import com.codefactory.supplychain.catalogo.application.port.out.TemplateRepository;
import com.codefactory.supplychain.catalogo.domain.model.Template;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity.TemplateEntity;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.mapper.TemplatePersistenceMapper;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.repository.TemplateJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Adaptador de salida que implementa {@link TemplateRepository}
 * apoyándose en Spring Data JPA.
 */
@Repository
public class TemplatePersistenceAdapter implements TemplateRepository {

    private final TemplateJpaRepository templateJpaRepository;
    private final TemplatePersistenceMapper templatePersistenceMapper;

    public TemplatePersistenceAdapter(TemplateJpaRepository templateJpaRepository,
                                       TemplatePersistenceMapper templatePersistenceMapper) {
        this.templateJpaRepository = templateJpaRepository;
        this.templatePersistenceMapper = templatePersistenceMapper;
    }

    @Override
    public Template save(Template template) {
        TemplateEntity entity = templatePersistenceMapper.toEntity(template);
        TemplateEntity saved = templateJpaRepository.save(entity);
        return templatePersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Template> findById(UUID id) {
        return templateJpaRepository.findById(id)
                .map(templatePersistenceMapper::toDomain);
    }

    @Override
    public Page<Template> findAll(Pageable pageable) {
        return templateJpaRepository.findAll(pageable).map(templatePersistenceMapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return templateJpaRepository.existsById(id);
    }

    @Override
    public void deleteById(UUID id) {
        templateJpaRepository.deleteById(id);
    }
}
