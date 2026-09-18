package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.LoginComando;
import com.codefactory.supplychain.identity.application.port.in.LoginResultado;
import com.codefactory.supplychain.identity.application.port.in.LoginUseCase;
import com.codefactory.supplychain.identity.application.port.out.MfaChallengeTokenPort;
import com.codefactory.supplychain.identity.application.port.out.PasswordHasherPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.CredencialesInvalidasException;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Service
@RequiredArgsConstructor
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
    private final MfaChallengeTokenPort mfaChallengeTokenPort;
    private final EmisionTokensService emisionTokensService;

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

        Usuario usuario = usuarioOpt.get();

        // MFA habilitado: no se emiten tokens todavía. El contador de intentos
        // fallidos tampoco se resetea acá — solo con un login TOTALMENTE completo
        // (ver CompletarLoginMfaService), para que un password correcto sin el
        // segundo factor no cuente como "éxito".
        if (usuario.isMfaHabilitado()) {
            String challengeToken = mfaChallengeTokenPort.generar(usuario.getId());
            return new LoginResultado.RequiereMfa(challengeToken);
        }

        Usuario actualizado = usuarioRepositoryPort.guardar(usuario.registrarLoginExitoso(ahora));
        return emisionTokensService.emitirParaUsuarioAutenticado(actualizado);
    }
}
