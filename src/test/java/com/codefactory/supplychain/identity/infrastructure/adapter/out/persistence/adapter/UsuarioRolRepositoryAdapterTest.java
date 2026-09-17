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
    void asignaYListaLosRolesDeUnUsuario() {
        Usuario usuario = usuarioRepository.guardar(Usuario.crear(Email.de("con-roles@ejemplo.com"), "Con Roles",
                PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        Rol rolA = rolRepository.guardar(Rol.crear("ROL_ASIGNADO_A", null));
        Rol rolB = rolRepository.guardar(Rol.crear("ROL_ASIGNADO_B", null));
        entityManager.flush();

        usuarioRolRepository.asignar(usuario.getId(), rolA.getId());
        usuarioRolRepository.asignar(usuario.getId(), rolB.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(usuarioRolRepository.listarRolesDeUsuario(usuario.getId()))
                .extracting(Rol::getNombre)
                .containsExactlyInAnyOrder("ROL_ASIGNADO_A", "ROL_ASIGNADO_B");
    }

    @Test
    void existeAsignacionDevuelveFalseSiNuncaSeAsigno() {
        Usuario usuario = usuarioRepository.guardar(Usuario.crear(Email.de("sin-asignar@ejemplo.com"),
                "Sin Asignar", PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        Rol rol = rolRepository.guardar(Rol.crear("ROL_NUNCA_ASIGNADO", null));
        entityManager.flush();

        assertThat(usuarioRolRepository.existeAsignacion(usuario.getId(), rol.getId())).isFalse();
    }

    @Test
    void quitarUnaAsignacionLaElimina() {
        Usuario usuario = usuarioRepository.guardar(Usuario.crear(Email.de("a-desasignar@ejemplo.com"),
                "A Desasignar", PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        Rol rol = rolRepository.guardar(Rol.crear("ROL_A_DESASIGNAR", null));
        entityManager.flush();
        usuarioRolRepository.asignar(usuario.getId(), rol.getId());
        entityManager.flush();
        entityManager.clear();

        usuarioRolRepository.quitar(usuario.getId(), rol.getId());
        entityManager.flush();

        assertThat(usuarioRolRepository.existeAsignacion(usuario.getId(), rol.getId())).isFalse();
    }

    @Test
    void contarUsuariosConRolCuentaSoloLasAsignacionesDeEseRol() {
        Usuario usuarioUno = usuarioRepository.guardar(Usuario.crear(Email.de("contar-uno@ejemplo.com"),
                "Contar Uno", PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        Usuario usuarioDos = usuarioRepository.guardar(Usuario.crear(Email.de("contar-dos@ejemplo.com"),
                "Contar Dos", PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        Rol rol = rolRepository.guardar(Rol.crear("ROL_PARA_CONTAR", null));
        entityManager.flush();

        assertThat(usuarioRolRepository.contarUsuariosConRol(rol.getId())).isZero();

        usuarioRolRepository.asignar(usuarioUno.getId(), rol.getId());
        entityManager.flush();
        entityManager.clear();
        assertThat(usuarioRolRepository.contarUsuariosConRol(rol.getId())).isEqualTo(1);

        usuarioRolRepository.asignar(usuarioDos.getId(), rol.getId());
        entityManager.flush();
        entityManager.clear();
        assertThat(usuarioRolRepository.contarUsuariosConRol(rol.getId())).isEqualTo(2);
    }
}
