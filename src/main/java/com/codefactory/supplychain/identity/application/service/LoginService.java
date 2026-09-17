package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.LoginComando;
import com.codefactory.supplychain.identity.application.port.in.LoginResultado;
import com.codefactory.supplychain.identity.application.port.in.LoginUseCase;
import com.codefactory.supplychain.identity.application.port.out.AccessTokenGeneratorPort;
import com.codefactory.supplychain.identity.application.port.out.PasswordHasherPort;
import com.codefactory.supplychain.identity.application.port.out.RefreshTokenRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.CredencialesInvalidasException;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.RefreshToken;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Service
public class LoginService implements LoginUseCase {

    /**
     * Hash "señuelo" (de una contraseña aleatoria descartada, jamás usada por ningún
     * usuario real) que se compara igual quese usa cuando el email no existe, para que
     * el tiempo de respuesta no delate si una cuenta existe o no.
     */
    private static final String HASH_SEÑUELO =
            "$2a$10$CwTycUXWue0Thq9StjUM0uJ8G8OJ8bbNvJVv53bJ8y6bnJmZ9x7Nu";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final AccessTokenGeneratorPort accessTokenGeneratorPort;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final long refreshTokenTtlDias;

    public LoginService(UsuarioRepositoryPort usuarioRepositoryPort,
                         PasswordHasherPort passwordHasherPort,
                         AccessTokenGeneratorPort accessTokenGeneratorPort,
                         RefreshTokenRepositoryPort refreshTokenRepositoryPort,
                         @Value("${app.security.jwt.refresh-token-ttl-dias}") long refreshTokenTtlDias) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.accessTokenGeneratorPort = accessTokenGeneratorPort;
        this.refreshTokenRepositoryPort = refreshTokenRepositoryPort;
        this.refreshTokenTtlDias = refreshTokenTtlDias;
    }

    @Override
    public LoginResultado login(LoginComando comando) {
        Email email = Email.de(comando.email());
        Optional<Usuario> usuarioOpt = usuarioRepositoryPort.buscarPorEmail(email);

        String hashParaComparar = usuarioOpt
                .map(usuario -> usuario.getPasswordHash().getValor())
                .orElse(HASH_SEÑUELO);
        boolean passwordCoincide = passwordHasherPort.coincide(comando.password(), PasswordHash.de(hashParaComparar));

        boolean credencialesValidas = usuarioOpt.isPresent()
                && passwordCoincide
                && usuarioOpt.get().getEstado() == EstadoUsuario.ACTIVO;

        if (!credencialesValidas) {
            throw new CredencialesInvalidasException();
        }

        Usuario usuario = usuarioOpt.get();
        String accessToken = accessTokenGeneratorPort.generar(usuario);

        String refreshTokenValor = generarValorAleatorio();
        Instant ahora = Instant.now();
        Instant expiraEn = ahora.plus(Duration.ofDays(refreshTokenTtlDias));
        RefreshToken refreshToken = RefreshToken.crear(usuario.getId(), sha256Hex(refreshTokenValor), ahora, expiraEn);
        refreshTokenRepositoryPort.guardar(refreshToken);

        return new LoginResultado(accessToken, refreshTokenValor, expiraEn, usuario);
    }

    private static String generarValorAleatorio() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(valor.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en este JDK", e);
        }
    }
}
