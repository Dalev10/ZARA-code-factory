package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.domain.model.Scope;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper.RolMapper;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper.ScopeMapper;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RolMapper.class, RolRepositoryAdapter.class, ScopeMapper.class, ScopeRepositoryAdapter.class,
        RolScopeRepositoryAdapter.class})
class RolScopeRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RolRepositoryAdapter rolRepository;

    @Autowired
    private ScopeRepositoryAdapter scopeRepository;

    @Autowired
    private RolScopeRepositoryAdapter rolScopeRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void asignaYListaLosScopesDeUnRol() {
        Rol rol = rolRepository.guardar(Rol.crear("ROL_CON_SCOPES", null));
        Scope scopeA = scopeRepository.guardar(Scope.crear("scope:a", null, false));
        Scope scopeB = scopeRepository.guardar(Scope.crear("scope:b", null, false));
        entityManager.flush();

        rolScopeRepository.asignar(rol.getId(), scopeA.getId());
        rolScopeRepository.asignar(rol.getId(), scopeB.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(rolScopeRepository.listarScopesDeRol(rol.getId()))
                .extracting(Scope::getCodigo)
                .containsExactlyInAnyOrder("scope:a", "scope:b");
    }

    @Test
    void existeAsignacionDevuelveFalseSiNuncaSeAsigno() {
        Rol rol = rolRepository.guardar(Rol.crear("ROL_SIN_SCOPES", null));
        Scope scope = scopeRepository.guardar(Scope.crear("scope:suelto", null, false));
        entityManager.flush();

        assertThat(rolScopeRepository.existeAsignacion(rol.getId(), scope.getId())).isFalse();
    }

    @Test
    void quitarUnaAsignacionLaElimina() {
        Rol rol = rolRepository.guardar(Rol.crear("ROL_A_DESASIGNAR", null));
        Scope scope = scopeRepository.guardar(Scope.crear("scope:a-quitar", null, false));
        entityManager.flush();
        rolScopeRepository.asignar(rol.getId(), scope.getId());
        entityManager.flush();
        entityManager.clear();

        rolScopeRepository.quitar(rol.getId(), scope.getId());
        entityManager.flush();

        assertThat(rolScopeRepository.existeAsignacion(rol.getId(), scope.getId())).isFalse();
    }

    @Test
    void quitarUnaAsignacionInexistenteNoFalla() {
        Rol rol = rolRepository.guardar(Rol.crear("ROL_SIN_NADA", null));
        Scope scope = scopeRepository.guardar(Scope.crear("scope:nunca-asignado", null, false));
        entityManager.flush();

        rolScopeRepository.quitar(rol.getId(), scope.getId());
    }

    @Test
    void tieneRolesAsignadosDetectaSiElScopeEstaEnUso() {
        Rol rol = rolRepository.guardar(Rol.crear("ROL_QUE_USA_SCOPE", null));
        Scope scope = scopeRepository.guardar(Scope.crear("scope:en-uso", null, false));
        entityManager.flush();

        assertThat(rolScopeRepository.tieneRolesAsignados(scope.getId())).isFalse();

        rolScopeRepository.asignar(rol.getId(), scope.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(rolScopeRepository.tieneRolesAsignados(scope.getId())).isTrue();
    }

    @Test
    void seEliminanEnCascadaAlEliminarElRol() {
        Rol rol = rolRepository.guardar(Rol.crear("ROL_A_BORRAR", null));
        Scope scope = scopeRepository.guardar(Scope.crear("scope:cascada-rol", null, false));
        entityManager.flush();
        rolScopeRepository.asignar(rol.getId(), scope.getId());
        entityManager.flush();
        entityManager.clear();

        rolRepository.eliminar(rol.getId());
        entityManager.flush();

        assertThat(rolScopeRepository.tieneRolesAsignados(scope.getId())).isFalse();
    }
}
