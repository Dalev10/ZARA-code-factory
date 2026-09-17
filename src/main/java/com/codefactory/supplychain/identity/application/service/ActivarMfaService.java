package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.ActivarMfaComando;
import com.codefactory.supplychain.identity.application.port.in.ActivarMfaResultado;
import com.codefactory.supplychain.identity.application.port.in.ActivarMfaUseCase;
import com.codefactory.supplychain.identity.application.port.out.SecretEncryptorPort;
import com.codefactory.supplychain.identity.application.port.out.TotpPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.TokenInvalidoException;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Primer paso del alta de MFA: genera un secreto TOTP nuevo, lo cifra y lo guarda
 * como pendiente de confirmar (mfaHabilitado sigue en false hasta que el usuario
 * demuestre, con un código válido, que configuró su app correctamente).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Service
@RequiredArgsConstructor
public class ActivarMfaService implements ActivarMfaUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final TotpPort totpPort;
    private final SecretEncryptorPort secretEncryptorPort;

    @Override
    public ActivarMfaResultado activar(ActivarMfaComando comando) {
        Usuario usuario = usuarioRepositoryPort.buscarPorId(comando.usuarioId())
                .orElseThrow(TokenInvalidoException::new);

        String secretoPlano = totpPort.generarSecreto();
        String secretoCifrado = secretEncryptorPort.encriptar(secretoPlano);

        Usuario actualizado = usuario.iniciarActivacionMfa(secretoCifrado, Instant.now());
        usuarioRepositoryPort.guardar(actualizado);

        String qrDataUri = totpPort.generarQrDataUri(secretoPlano, usuario.getEmail().getValor());
        return new ActivarMfaResultado(secretoPlano, qrDataUri);
    }
}
