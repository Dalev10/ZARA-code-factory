package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.RefreshToken;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper.RefreshTokenMapper;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UsuarioMapper.class, UsuarioRepositoryAdapter.class, RefreshTokenMapper.class, RefreshTokenRepositoryAdapter.class})
class RefreshTokenRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UsuarioRepositoryAdapter usuarioRepository;

    @Autowired
    private RefreshTokenRepositoryAdapter refreshTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void guardaUnRefreshTokenAsociadoAUnUsuario() {
        Usuario usuario = usuarioRepository.guardar(Usuario.crear(Email.de("ana@ejemplo.com"), "Ana Pérez",
                PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        entityManager.flush();

        Instant ahora = Instant.now();
        RefreshToken token = RefreshToken.crear(usuario.getId(), "hash-de-prueba", ahora, ahora.plusSeconds(3600));

        RefreshToken guardado = refreshTokenRepository.guardar(token);
        entityManager.flush();
        entityManager.clear();

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getUsuarioId()).isEqualTo(usuario.getId());
        assertThat(guardado.getTokenHash()).isEqualTo("hash-de-prueba");
    }

    @Test
    void laBaseDeDatosRechazaTokenHashDuplicado() {
        Usuario usuario = usuarioRepository.guardar(Usuario.crear(Email.de("carlos@ejemplo.com"), "Carlos Ruiz",
                PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        entityManager.flush();

        Instant ahora = Instant.now();
        refreshTokenRepository.guardar(
                RefreshToken.crear(usuario.getId(), "hash-repetido", ahora, ahora.plusSeconds(3600)));
        entityManager.flush();
        entityManager.clear();

        refreshTokenRepository.guardar(
                RefreshToken.crear(usuario.getId(), "hash-repetido", ahora, ahora.plusSeconds(3600)));

        org.assertj.core.api.Assertions.assertThatThrownBy(entityManager::flush)
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void seEliminaEnCascadaAlEliminarElUsuario() {
        Usuario usuario = usuarioRepository.guardar(Usuario.crear(Email.de("borrar@ejemplo.com"), "A Borrar",
                PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv")));
        entityManager.flush();

        Instant ahora = Instant.now();
        refreshTokenRepository.guardar(
                RefreshToken.crear(usuario.getId(), "hash-a-borrar", ahora, ahora.plusSeconds(3600)));
        entityManager.flush();
        entityManager.clear();

        entityManager.getEntityManager()
                .createQuery("delete from UsuarioEntity u where u.id = :id")
                .setParameter("id", usuario.getId())
                .executeUpdate();
        entityManager.flush();

        Long restantes = entityManager.getEntityManager()
                .createQuery("select count(r) from RefreshTokenEntity r where r.usuarioId = :id", Long.class)
                .setParameter("id", usuario.getId())
                .getSingleResult();
        assertThat(restantes).isZero();
    }
}
