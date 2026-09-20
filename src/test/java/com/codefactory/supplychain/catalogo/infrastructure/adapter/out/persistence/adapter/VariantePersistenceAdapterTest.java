package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.catalogo.domain.model.Template;
import com.codefactory.supplychain.catalogo.domain.model.Variante;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.mapper.CategoriaPersistenceMapper;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.mapper.TemplatePersistenceMapper;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.mapper.VariantePersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Módulo: catalogo — Categoria/Template/Variante (FEAT-05).
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CategoriaPersistenceMapper.class, CategoriaPersistenceAdapter.class,
        TemplatePersistenceMapper.class, TemplatePersistenceAdapter.class,
        VariantePersistenceMapper.class, VariantePersistenceAdapter.class})
class VariantePersistenceAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CategoriaPersistenceAdapter categoriaRepository;

    @Autowired
    private TemplatePersistenceAdapter templateRepository;

    @Autowired
    private VariantePersistenceAdapter varianteRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Template templatePersistido() {
        Categoria categoria = categoriaRepository.save(new Categoria("Calzado"));
        Template template = templateRepository.save(new Template("Zapatilla X", null, null, null, categoria));
        entityManager.flush();
        return template;
    }

    @Test
    void guardaYRecuperaUnaVarianteConSuTemplate() {
        Template template = templatePersistido();

        Variante guardada = varianteRepository.save(new Variante("SKU-001", template, "M", "Rojo"));
        entityManager.flush();
        entityManager.clear();

        Variante recuperada = varianteRepository.findById(guardada.getId()).orElseThrow();
        assertThat(recuperada.getSku()).isEqualTo("SKU-001");
        assertThat(recuperada.getTemplate().getId()).isEqualTo(template.getId());
    }

    @Test
    void buscaPorSku() {
        Template template = templatePersistido();
        varianteRepository.save(new Variante("SKU-UNICO", template, null, null));
        entityManager.flush();
        entityManager.clear();

        assertThat(varianteRepository.findBySku("SKU-UNICO")).isPresent();
    }

    @Test
    void existsBySkuDevuelveFalseSiNoExiste() {
        assertThat(varianteRepository.existsBySku("NO-EXISTE")).isFalse();
    }

    @Test
    void laBaseDeDatosRechazaSkusDuplicados() {
        Template template = templatePersistido();
        varianteRepository.save(new Variante("SKU-DUP", template, null, null));
        entityManager.flush();
        entityManager.clear();

        varianteRepository.save(new Variante("SKU-DUP", template, null, null));

        assertThatThrownBy(entityManager::flush).isInstanceOf(RuntimeException.class);
    }

    @Test
    void existsByIdDevuelveFalseSiNoExiste() {
        assertThat(varianteRepository.existsById(UUID.randomUUID())).isFalse();
    }

    @Test
    void deleteByIdEliminaLaVariante() {
        Template template = templatePersistido();
        Variante creada = varianteRepository.save(new Variante("SKU-BORRAR", template, null, null));
        entityManager.flush();
        entityManager.clear();

        varianteRepository.deleteById(creada.getId());
        entityManager.flush();

        assertThat(varianteRepository.findById(creada.getId())).isEmpty();
    }
}
