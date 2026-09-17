package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioRolEntity;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioRolId;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper.RolMapper;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper.UsuarioMapper;
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

/**
 * La asignación real de roles a usuarios es HU-10 — acá se inserta la fila de
 * usuario_rol directamente vía el EntityManager para probar la lectura
 * (UsuarioRolRepositoryPort) que HU-09 sí necesita ya.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UsuarioMapper.class, UsuarioRepositoryAdapter.class, RolMapper.class, RolRepositoryAdapter.class,
        UsuarioRolRepositoryAdapter.class})
class UsuarioRolRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UsuarioRepositoryAdapter usuarioRepository;

    @Autowired
    private RolRepositoryAdapter rolRepository;

    @Autowired
    private UsuarioRolRepositoryAdapter usuarioRolRepository;

    @Autowired
    private TestEntityManager entityManager;

    private void asignarRol(UUID usuarioId, UUID rolId) {
        entityManager.persist(new UsuarioRolEntity(new UsuarioRolId(usuarioId, rolId)));
    }

    @Test
    void tieneUsuariosAsignadosEsFalseParaUnRolSinNadie() {
        Rol rol = rolRepository.guardar(Rol.crear("ROL_SIN_USUARIOS", null));
        entityManager.flush();

        assertThat(usuarioRolRepository.tieneUsuariosAsignados(rol.getId())).isFalse();
    }

    @Test
    void tieneUsuariosAsignadosEsTrueCuandoHayUnaAsignacion() {
        Usuario usuario = usuarioRepository.guardar(Usuario.crear(Email.de("con-rol@ejemplo.com"), "Con Rol",
                PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        Rol rol = rolRepository.guardar(Rol.crear("ROL_CON_USUARIO", null));
        entityManager.flush();

        asignarRol(usuario.getId(), rol.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(usuarioRolRepository.tieneUsuariosAsignados(rol.getId())).isTrue();
    }

    @Test
    void usuarioTieneRolNombradoEsFalseSiElRolNoExiste() {
        Usuario usuario = usuarioRepository.guardar(Usuario.crear(Email.de("sin-rol@ejemplo.com"), "Sin Rol",
                PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        entityManager.flush();

        assertThat(usuarioRolRepository.usuarioTieneRolNombrado(usuario.getId(), "NO_EXISTE")).isFalse();
    }

    @Test
    void usuarioTieneRolNombradoEsTrueSoloParaElUsuarioAsignado() {
        Usuario conRol = usuarioRepository.guardar(Usuario.crear(Email.de("admin-de-prueba@ejemplo.com"),
                "Admin de Prueba", PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        Usuario sinRol = usuarioRepository.guardar(Usuario.crear(Email.de("no-admin@ejemplo.com"), "No Admin",
                PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        Rol rolAdmin = rolRepository.guardar(Rol.crear("ADMIN_DE_PRUEBA", null));
        entityManager.flush();

        asignarRol(conRol.getId(), rolAdmin.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(usuarioRolRepository.usuarioTieneRolNombrado(conRol.getId(), "ADMIN_DE_PRUEBA")).isTrue();
        assertThat(usuarioRolRepository.usuarioTieneRolNombrado(sinRol.getId(), "ADMIN_DE_PRUEBA")).isFalse();
    }
}
