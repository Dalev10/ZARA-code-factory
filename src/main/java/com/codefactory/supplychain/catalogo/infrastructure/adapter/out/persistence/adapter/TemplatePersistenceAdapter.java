package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.catalogo.application.port.out.TemplateRepository;
import com.codefactory.supplychain.catalogo.domain.model.Template;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity.TemplateEntity;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.mapper.TemplatePersistenceMapper;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.repository.TemplateJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    public Optional<Template> findById(Long id) {
        return templateJpaRepository.findById(id)
                .map(templatePersistenceMapper::toDomain);
    }

    @Override
    public List<Template> findAll() {
        return templateJpaRepository.findAll().stream()
                .map(templatePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(Long id) {
        return templateJpaRepository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        templateJpaRepository.deleteById(id);
    }
}
