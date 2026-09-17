package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.CompletarLoginMfaComando;
import com.codefactory.supplychain.identity.application.port.in.CompletarLoginMfaUseCase;
import com.codefactory.supplychain.identity.application.port.in.LoginResultado;
import com.codefactory.supplychain.identity.application.port.out.CodigoRespaldoRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.MfaChallengeTokenPort;
import com.codefactory.supplychain.identity.application.port.out.SecretEncryptorPort;
import com.codefactory.supplychain.identity.application.port.out.TotpPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.CodigoMfaInvalidoException;
import com.codefactory.supplychain.identity.domain.exception.CredencialesInvalidasException;
import com.codefactory.supplychain.identity.domain.exception.TokenInvalidoException;
import com.codefactory.supplychain.identity.domain.model.CodigoRespaldo;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Segundo paso del login cuando la cuenta tiene MFA habilitado: intercambia el
 * token de desafío (emitido por LoginService tras validar la contraseña) más un
 * código — TOTP de 6 dígitos o un código de respaldo de un solo uso de HU-07 —
 * por los tokens reales de sesión.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Service
@RequiredArgsConstructor
public class CompletarLoginMfaService implements CompletarLoginMfaUseCase {

    private final MfaChallengeTokenPort mfaChallengeTokenPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final TotpPort totpPort;
    private final SecretEncryptorPort secretEncryptorPort;
    private final CodigoRespaldoRepositoryPort codigoRespaldoRepositoryPort;
    private final EmisionTokensService emisionTokensService;

    @Override
    public LoginResultado.Completado completar(CompletarLoginMfaComando comando) {
        Instant ahora = Instant.now();
        UUID usuarioId = mfaChallengeTokenPort.validar(comando.mfaChallengeToken());

        Usuario usuario = usuarioRepositoryPort.buscarPorId(usuarioId)
                .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO)
                .orElseThrow(TokenInvalidoException::new);

        if (usuario.estaBloqueadoTemporalmente(ahora)) {
            throw new CredencialesInvalidasException();
        }

        if (!verificarSegundoFactor(usuario, comando.codigo(), ahora)) {
            usuarioRepositoryPort.guardar(usuario.registrarIntentoFallido(ahora));
            throw new CodigoMfaInvalidoException();
        }

        Usuario actualizado = usuarioRepositoryPort.guardar(usuario.registrarLoginExitoso(ahora));
        return emisionTokensService.emitirParaUsuarioAutenticado(actualizado);
    }

    private boolean verificarSegundoFactor(Usuario usuario, String codigoIngresado, Instant ahora) {
        String secretoPlano = secretEncryptorPort.desencriptar(usuario.getMfaSecretEncrypted());
        if (totpPort.verificarCodigo(secretoPlano, codigoIngresado)) {
            return true;
        }
        return intentarConsumirCodigoDeRespaldo(usuario, codigoIngresado, ahora);
    }

    private boolean intentarConsumirCodigoDeRespaldo(Usuario usuario, String codigoIngresado, Instant ahora) {
        String hashCodigo = HashingSupport.sha256Hex(codigoIngresado.trim().toUpperCase());
        return codigoRespaldoRepositoryPort.buscarNoUsadoPorHash(usuario.getId(), hashCodigo)
                .map(codigo -> marcarComoUsado(codigo, ahora))
                .orElse(false);
    }

    private boolean marcarComoUsado(CodigoRespaldo codigo, Instant ahora) {
        codigoRespaldoRepositoryPort.guardar(codigo.marcarUsado(ahora));
        return true;
    }
}
