package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.mapper.CategoriaPersistenceMapper;
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
@Import({CategoriaPersistenceMapper.class, CategoriaPersistenceAdapter.class})
class CategoriaPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CategoriaPersistenceAdapter categoriaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void guardaYRecuperaUnaCategoriaPorId() {
        Categoria guardada = categoriaRepository.save(new Categoria("Calzado"));
        entityManager.flush();
        entityManager.clear();

        assertThat(categoriaRepository.findById(guardada.getId()))
                .isPresent().get().extracting(Categoria::getNombre).isEqualTo("Calzado");
    }

    @Test
    void listarTodasIncluyeLasCategoriasGuardadas() {
        categoriaRepository.save(new Categoria("Calzado"));
        categoriaRepository.save(new Categoria("Ropa"));
        entityManager.flush();

        assertThat(categoriaRepository.findAll()).extracting(Categoria::getNombre).contains("Calzado", "Ropa");
    }

    @Test
    void existsByIdDevuelveFalseSiNoExiste() {
        assertThat(categoriaRepository.existsById(UUID.randomUUID())).isFalse();
    }

    @Test
    void guardarUnaCategoriaYaExistenteLaActualiza() {
        Categoria creada = categoriaRepository.save(new Categoria("Calzado"));
        entityManager.flush();
        entityManager.clear();

        creada.cambiarNombre("Calzado Deportivo");
        categoriaRepository.save(creada);
        entityManager.flush();
        entityManager.clear();

        assertThat(categoriaRepository.findById(creada.getId()))
                .isPresent().get().extracting(Categoria::getNombre).isEqualTo("Calzado Deportivo");
    }

    @Test
    void laBaseDeDatosRechazaNombresDuplicados() {
        categoriaRepository.save(new Categoria("Categoria Duplicada"));
        entityManager.flush();
        entityManager.clear();

        categoriaRepository.save(new Categoria("Categoria Duplicada"));

        assertThatThrownBy(entityManager::flush).isInstanceOf(RuntimeException.class);
    }

    @Test
    void deleteByIdEliminaLaCategoria() {
        Categoria creada = categoriaRepository.save(new Categoria("Descartable"));
        entityManager.flush();
        entityManager.clear();

        categoriaRepository.deleteById(creada.getId());
        entityManager.flush();

        assertThat(categoriaRepository.findById(creada.getId())).isEmpty();
    }
}
