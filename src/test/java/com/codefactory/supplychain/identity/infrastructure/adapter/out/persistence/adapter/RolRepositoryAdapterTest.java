package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper.RolMapper;
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
@Import({RolMapper.class, RolRepositoryAdapter.class})
class RolRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RolRepositoryAdapter rolRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void guardaYRecuperaUnRolPorId() {
        Rol guardado = rolRepository.guardar(Rol.crear("ADMIN", "Administrador del sistema"));
        entityManager.flush();
        entityManager.clear();

        assertThat(rolRepository.buscarPorId(guardado.getId())).isPresent();
    }

    @Test
    void buscaPorNombre() {
        rolRepository.guardar(Rol.crear("OPERADOR", "Operador de tienda"));
        entityManager.flush();
        entityManager.clear();

        assertThat(rolRepository.buscarPorNombre("OPERADOR")).isPresent();
    }

    @Test
    void listarTodosIncluyeLosRolesGuardados() {
        rolRepository.guardar(Rol.crear("ROL_A", null));
        rolRepository.guardar(Rol.crear("ROL_B", null));
        entityManager.flush();

        assertThat(rolRepository.listarTodos())
                .extracting(Rol::getNombre)
                .contains("ROL_A", "ROL_B");
    }

    @Test
    void existePorNombreDevuelveFalseSiNoExiste() {
        assertThat(rolRepository.existePorNombre("NO_EXISTE")).isFalse();
    }

    @Test
    void laBaseDeDatosRechazaNombresDuplicados() {
        rolRepository.guardar(Rol.crear("DUPLICADO", null));
        entityManager.flush();
        entityManager.clear();

        rolRepository.guardar(Rol.crear("DUPLICADO", null));

        assertThatThrownBy(entityManager::flush).isInstanceOf(RuntimeException.class);
    }
}
