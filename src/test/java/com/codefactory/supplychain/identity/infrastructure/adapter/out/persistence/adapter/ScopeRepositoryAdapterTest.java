package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.identity.domain.model.Scope;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper.ScopeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ScopeMapper.class, ScopeRepositoryAdapter.class})
class ScopeRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ScopeRepositoryAdapter scopeRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void guardaYRecuperaUnScopePorId() {
        Scope guardado = scopeRepository.guardar(Scope.crear("usuarios:leer", "Lectura de usuarios", false));
        entityManager.flush();
        entityManager.clear();

        assertThat(scopeRepository.buscarPorId(guardado.getId())).isPresent();
    }

    @Test
    void buscaPorCodigo() {
        scopeRepository.guardar(Scope.crear("usuarios:escribir", "Escritura de usuarios", true));
        entityManager.flush();
        entityManager.clear();

        assertThat(scopeRepository.buscarPorCodigo("usuarios:escribir"))
                .isPresent()
                .get()
                .satisfies(scope -> assertThat(scope.isSensible()).isTrue());
    }

    @Test
    void listarTodosIncluyeLosScopesGuardados() {
        scopeRepository.guardar(Scope.crear("scope:a", null, false));
        scopeRepository.guardar(Scope.crear("scope:b", null, false));
        entityManager.flush();

        assertThat(scopeRepository.listarTodos())
                .extracting(Scope::getCodigo)
                .contains("scope:a", "scope:b");
    }

    @Test
    void existePorCodigoDevuelveFalseSiNoExiste() {
        assertThat(scopeRepository.existePorCodigo("no:existe")).isFalse();
    }

    @Test
    void laBaseDeDatosRechazaCodigosDuplicados() {
        scopeRepository.guardar(Scope.crear("duplicado:codigo", null, false));
        entityManager.flush();
        entityManager.clear();

        scopeRepository.guardar(Scope.crear("duplicado:codigo", null, false));

        assertThatThrownBy(entityManager::flush).isInstanceOf(RuntimeException.class);
    }

    @Test
    void guardarUnScopeYaExistenteLoActualizaEnVezDeDuplicarlo() {
        Scope creado = scopeRepository.guardar(Scope.crear("scope:actualizable", "desc vieja", false));
        entityManager.flush();
        entityManager.clear();

        scopeRepository.guardar(creado.actualizar("scope:actualizado", "desc nueva", true));
        entityManager.flush();
        entityManager.clear();

        assertThat(scopeRepository.buscarPorId(creado.getId())).get()
                .satisfies(scope -> {
                    assertThat(scope.getCodigo()).isEqualTo("scope:actualizado");
                    assertThat(scope.isSensible()).isTrue();
                });
    }

    @Test
    void eliminaUnScopePorId() {
        Scope creado = scopeRepository.guardar(Scope.crear("scope:a-eliminar", null, false));
        entityManager.flush();
        entityManager.clear();

        scopeRepository.eliminar(creado.getId());
        entityManager.flush();

        assertThat(scopeRepository.buscarPorId(creado.getId())).isEmpty();
    }
}
