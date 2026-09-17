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

import java.time.Duration;
import java.time.Instant;
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
        Instant ahora = Instant.now();
        Email email = Email.de(comando.email());
        Optional<Usuario> usuarioOpt = usuarioRepositoryPort.buscarPorEmail(email);

        // Una cuenta bloqueada temporalmente rechaza de una, sin sumar más intentos ni
        // extender el bloqueo — la duración quedó fijada cuando se activó el bloqueo.
        if (usuarioOpt.isPresent() && usuarioOpt.get().estaBloqueadoTemporalmente(ahora)) {
            throw new CredencialesInvalidasException();
        }

        String hashParaComparar = usuarioOpt
                .map(usuario -> usuario.getPasswordHash().getValor())
                .orElse(HASH_SEÑUELO);
        boolean passwordCoincide = passwordHasherPort.coincide(comando.password(), PasswordHash.de(hashParaComparar));

        boolean credencialesValidas = usuarioOpt.isPresent()
                && passwordCoincide
                && usuarioOpt.get().getEstado() == EstadoUsuario.ACTIVO;

        if (!credencialesValidas) {
            usuarioOpt.ifPresent(usuario -> usuarioRepositoryPort.guardar(usuario.registrarIntentoFallido(ahora)));
            throw new CredencialesInvalidasException();
        }

        Usuario usuario = usuarioRepositoryPort.guardar(usuarioOpt.get().registrarLoginExitoso(ahora));
        String accessToken = accessTokenGeneratorPort.generar(usuario);

        String refreshTokenValor = RefreshTokenSupport.generarValorAleatorio();
        Instant expiraEn = ahora.plus(Duration.ofDays(refreshTokenTtlDias));
        RefreshToken refreshToken = RefreshToken.crearNuevaFamilia(usuario.getId(),
                RefreshTokenSupport.sha256Hex(refreshTokenValor), ahora, expiraEn);
        refreshTokenRepositoryPort.guardar(refreshToken);

        return new LoginResultado(accessToken, refreshTokenValor, expiraEn, usuario);
    }
}
