package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper.UsuarioMapper;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica el adaptador de persistencia de Usuario contra un Postgres real (Testcontainers),
 * incluyendo que las migraciones de Flyway (V1-V3) y el mapeo JPA sean consistentes entre sí
 * (algo que un H2 embebido no garantizaría, dado el uso de gen_random_uuid(), CHECK constraints
 * y el trigger de actualizado_en).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UsuarioMapper.class, UsuarioRepositoryAdapter.class})
class UsuarioRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final PasswordHash HASH = PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv");

    @Autowired
    private UsuarioRepositoryAdapter usuarioRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void guardaYRecuperaUnUsuarioPorId() {
        Usuario usuario = Usuario.crear(Email.de("ana@ejemplo.com"), "Ana Pérez", HASH);

        Usuario guardado = usuarioRepository.guardar(usuario);
        entityManager.flush();
        entityManager.clear();

        Optional<Usuario> recuperado = usuarioRepository.buscarPorId(guardado.getId());

        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getEmail()).isEqualTo(usuario.getEmail());
        assertThat(recuperado.get().getNombreCompleto()).isEqualTo("Ana Pérez");
        assertThat(recuperado.get().getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(recuperado.get().getIntentosFallidos()).isZero();
        assertThat(recuperado.get().isMfaHabilitado()).isFalse();
    }

    @Test
    void buscaPorEmailNormalizadoAMinusculas() {
        usuarioRepository.guardar(Usuario.crear(Email.de("Carlos@Ejemplo.com"), "Carlos Ruiz", HASH));
        entityManager.flush();
        entityManager.clear();

        assertThat(usuarioRepository.buscarPorEmail(Email.de("carlos@ejemplo.com"))).isPresent();
    }

    @Test
    void buscarPorIdInexistenteDevuelveVacio() {
        assertThat(usuarioRepository.buscarPorId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void existePorEmailDevuelveFalseSiNoExiste() {
        assertThat(usuarioRepository.existePorEmail(Email.de("nadie@ejemplo.com"))).isFalse();
    }

    @Test
    void existePorEmailDevuelveTrueTrasGuardar() {
        Email email = Email.de("existente@ejemplo.com");
        usuarioRepository.guardar(Usuario.crear(email, "Persona Existente", HASH));
        entityManager.flush();

        assertThat(usuarioRepository.existePorEmail(email)).isTrue();
    }

    @Test
    void laBaseDeDatosRechazaEmailsDuplicados() {
        Email email = Email.de("duplicado@ejemplo.com");
        usuarioRepository.guardar(Usuario.crear(email, "Persona Uno", HASH));
        entityManager.flush();
        entityManager.clear();

        usuarioRepository.guardar(Usuario.crear(email, "Persona Dos", HASH));

        assertThatThrownBy(entityManager::flush).isInstanceOf(RuntimeException.class);
    }

    @Test
    void listarTodosIncluyeLosUsuariosGuardados() {
        usuarioRepository.guardar(Usuario.crear(Email.de("listado-a@ejemplo.com"), "Listado A", HASH));
        usuarioRepository.guardar(Usuario.crear(Email.de("listado-b@ejemplo.com"), "Listado B", HASH));
        entityManager.flush();

        assertThat(usuarioRepository.listarTodos())
                .extracting(u -> u.getEmail().getValor())
                .contains("listado-a@ejemplo.com", "listado-b@ejemplo.com");
    }
}
