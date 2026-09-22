package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.identity.domain.model.CodigoRespaldo;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper.CodigoRespaldoMapper;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UsuarioMapper.class, UsuarioRepositoryAdapter.class, CodigoRespaldoMapper.class,
        CodigoRespaldoRepositoryAdapter.class})
class CodigoRespaldoRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UsuarioRepositoryAdapter usuarioRepository;

    @Autowired
    private CodigoRespaldoRepositoryAdapter codigoRespaldoRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void guardaTodosLosCodigosDeRespaldoDeUnUsuario() {
        Usuario usuario = usuarioRepository.guardar(Usuario.crear(Email.de("mfa@ejemplo.com"), "Usuario MFA",
                PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        entityManager.flush();

        Instant ahora = Instant.now();
        List<CodigoRespaldo> codigos = List.of(
                CodigoRespaldo.crear(usuario.getId(), "hash-1", ahora),
                CodigoRespaldo.crear(usuario.getId(), "hash-2", ahora),
                CodigoRespaldo.crear(usuario.getId(), "hash-3", ahora));

        List<CodigoRespaldo> guardados = codigoRespaldoRepository.guardarTodos(codigos);
        entityManager.flush();
        entityManager.clear();

        assertThat(guardados).hasSize(3);
        assertThat(guardados).allMatch(c -> c.getUsuarioId().equals(usuario.getId()));
        assertThat(guardados).allMatch(c -> !c.estaUsado());
    }

    @Test
    void seEliminanEnCascadaAlEliminarElUsuario() {
        Usuario usuario = usuarioRepository.guardar(Usuario.crear(Email.de("borrar-mfa@ejemplo.com"), "A Borrar",
                PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        entityManager.flush();

        codigoRespaldoRepository.guardarTodos(List.of(
                CodigoRespaldo.crear(usuario.getId(), "hash-a-borrar", Instant.now())));
        entityManager.flush();
        entityManager.clear();

        entityManager.getEntityManager()
                .createQuery("delete from UsuarioEntity u where u.id = :id")
                .setParameter("id", usuario.getId())
                .executeUpdate();
        entityManager.flush();

        Long restantes = entityManager.getEntityManager()
                .createQuery("select count(c) from CodigoRespaldoEntity c where c.usuarioId = :id", Long.class)
                .setParameter("id", usuario.getId())
                .getSingleResult();
        assertThat(restantes).isZero();
    }

    @Test
    void encuentraUnCodigoNoUsadoPorSuHash() {
        Usuario usuario = usuarioRepository.guardar(Usuario.crear(Email.de("mfa-buscar@ejemplo.com"), "Usuario MFA",
                PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        entityManager.flush();

        codigoRespaldoRepository.guardarTodos(List.of(
                CodigoRespaldo.crear(usuario.getId(), "hash-a-buscar", Instant.now())));
        entityManager.flush();
        entityManager.clear();

        Optional<CodigoRespaldo> encontrado = codigoRespaldoRepository.buscarNoUsadoPorHash(usuario.getId(),
                "hash-a-buscar");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().estaUsado()).isFalse();
    }

    @Test
    void noEncuentraUnCodigoYaUsadoNiUnoDeOtroUsuario() {
        Usuario usuario = usuarioRepository.guardar(Usuario.crear(Email.de("mfa-usado@ejemplo.com"), "Usuario MFA",
                PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        Usuario otroUsuario = usuarioRepository.guardar(Usuario.crear(Email.de("mfa-otro@ejemplo.com"), "Otro Usuario",
                PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        entityManager.flush();

        List<CodigoRespaldo> guardados = codigoRespaldoRepository.guardarTodos(List.of(
                CodigoRespaldo.crear(usuario.getId(), "hash-ya-usado", Instant.now()),
                CodigoRespaldo.crear(otroUsuario.getId(), "hash-de-otro", Instant.now())));
        entityManager.flush();
        entityManager.clear();

        CodigoRespaldo codigoUsado = guardados.stream()
                .filter(c -> c.getCodigoHash().equals("hash-ya-usado"))
                .findFirst().orElseThrow();
        codigoRespaldoRepository.guardar(codigoUsado.marcarUsado(Instant.now()));
        entityManager.flush();
        entityManager.clear();

        assertThat(codigoRespaldoRepository.buscarNoUsadoPorHash(usuario.getId(), "hash-ya-usado")).isEmpty();
        assertThat(codigoRespaldoRepository.buscarNoUsadoPorHash(usuario.getId(), "hash-de-otro")).isEmpty();
        assertThat(codigoRespaldoRepository.buscarNoUsadoPorHash(otroUsuario.getId(), "hash-de-otro")).isPresent();
    }
}
