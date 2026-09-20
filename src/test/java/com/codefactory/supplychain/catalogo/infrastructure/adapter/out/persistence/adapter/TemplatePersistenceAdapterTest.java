package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.catalogo.domain.model.Template;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.mapper.CategoriaPersistenceMapper;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.mapper.TemplatePersistenceMapper;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Módulo: catalogo — Categoria/Template/Variante (FEAT-05).
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CategoriaPersistenceMapper.class, CategoriaPersistenceAdapter.class,
        TemplatePersistenceMapper.class, TemplatePersistenceAdapter.class})
class TemplatePersistenceAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CategoriaPersistenceAdapter categoriaRepository;

    @Autowired
    private TemplatePersistenceAdapter templateRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Categoria categoriaPersistida() {
        Categoria categoria = categoriaRepository.save(new Categoria("Calzado"));
        entityManager.flush();
        return categoria;
    }

    @Test
    void guardaYRecuperaUnTemplateConSuCategoria() {
        Categoria categoria = categoriaPersistida();

        Template guardado = templateRepository.save(
                new Template("Zapatilla X", "Verano", "ProveedorX", BigDecimal.TEN, categoria));
        entityManager.flush();
        entityManager.clear();

        Template recuperado = templateRepository.findById(guardado.getId()).orElseThrow();
        assertThat(recuperado.getNombre()).isEqualTo("Zapatilla X");
        assertThat(recuperado.getCategoria().getId()).isEqualTo(categoria.getId());
    }

    @Test
    void listarTodosIncluyeLosTemplatesGuardados() {
        Categoria categoria = categoriaPersistida();
        templateRepository.save(new Template("Template A", null, null, null, categoria));
        templateRepository.save(new Template("Template B", null, null, null, categoria));
        entityManager.flush();

        assertThat(templateRepository.findAll()).extracting(Template::getNombre)
                .contains("Template A", "Template B");
    }

    @Test
    void existsByIdDevuelveFalseSiNoExiste() {
        assertThat(templateRepository.existsById(UUID.randomUUID())).isFalse();
    }

    @Test
    void guardarUnTemplateYaExistenteLoActualiza() {
        Categoria categoria = categoriaPersistida();
        Template creado = templateRepository.save(
                new Template("Zapatilla X", "Verano", "ProveedorX", BigDecimal.TEN, categoria));
        entityManager.flush();
        entityManager.clear();

        creado.actualizarInformacion("Zapatilla Y", "Invierno", "ProveedorY", BigDecimal.ONE);
        templateRepository.save(creado);
        entityManager.flush();
        entityManager.clear();

        Template actualizado = templateRepository.findById(creado.getId()).orElseThrow();
        assertThat(actualizado.getNombre()).isEqualTo("Zapatilla Y");
        assertThat(actualizado.getPrecioBase()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void deleteByIdEliminaElTemplate() {
        Categoria categoria = categoriaPersistida();
        Template creado = templateRepository.save(new Template("Descartable", null, null, null, categoria));
        entityManager.flush();
        entityManager.clear();

        templateRepository.deleteById(creado.getId());
        entityManager.flush();

        assertThat(templateRepository.findById(creado.getId())).isEmpty();
    }
}
