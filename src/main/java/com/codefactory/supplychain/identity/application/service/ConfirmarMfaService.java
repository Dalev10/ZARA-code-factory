package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.ConfirmarMfaComando;
import com.codefactory.supplychain.identity.application.port.in.ConfirmarMfaResultado;
import com.codefactory.supplychain.identity.application.port.in.ConfirmarMfaUseCase;
import com.codefactory.supplychain.identity.application.port.out.CodigoRespaldoRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.SecretEncryptorPort;
import com.codefactory.supplychain.identity.application.port.out.TotpPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.CodigoMfaInvalidoException;
import com.codefactory.supplychain.identity.domain.exception.MfaNoConfiguradoException;
import com.codefactory.supplychain.identity.domain.exception.TokenInvalidoException;
import com.codefactory.supplychain.identity.domain.model.CodigoRespaldo;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Segundo paso del alta de MFA: valida el código TOTP contra el secreto pendiente,
 * habilita MFA, y genera los códigos de respaldo (se muestran una única vez).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Service
public class ConfirmarMfaService implements ConfirmarMfaUseCase {

    private static final int CANTIDAD_CODIGOS_RESPALDO = 8;
    private static final int LONGITUD_CODIGO_RESPALDO = 10;

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final TotpPort totpPort;
    private final SecretEncryptorPort secretEncryptorPort;
    private final CodigoRespaldoRepositoryPort codigoRespaldoRepositoryPort;

    public ConfirmarMfaService(UsuarioRepositoryPort usuarioRepositoryPort, TotpPort totpPort,
                                SecretEncryptorPort secretEncryptorPort,
                                CodigoRespaldoRepositoryPort codigoRespaldoRepositoryPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.totpPort = totpPort;
        this.secretEncryptorPort = secretEncryptorPort;
        this.codigoRespaldoRepositoryPort = codigoRespaldoRepositoryPort;
    }

    @Override
    public ConfirmarMfaResultado confirmar(ConfirmarMfaComando comando) {
        Instant ahora = Instant.now();
        Usuario usuario = usuarioRepositoryPort.buscarPorId(comando.usuarioId())
                .orElseThrow(TokenInvalidoException::new);

        // Chequeo explícito ANTES de desencriptar: sin esto, un usuario que nunca llamó
        // a /activar rompería acá con un null en vez del error de negocio claro que
        // Usuario.confirmarActivacionMfa lanzaría más abajo (esa validación queda como
        // red de seguridad adicional, no se duplica el mensaje).
        if (usuario.getMfaSecretEncrypted() == null) {
            throw new MfaNoConfiguradoException();
        }

        String secretoPlano = secretEncryptorPort.desencriptar(usuario.getMfaSecretEncrypted());
        if (!totpPort.verificarCodigo(secretoPlano, comando.codigo())) {
            throw new CodigoMfaInvalidoException();
        }

        Usuario confirmado = usuario.confirmarActivacionMfa(ahora);
        usuarioRepositoryPort.guardar(confirmado);

        List<String> codigosPlano = generarCodigosRespaldo();
        List<CodigoRespaldo> codigosParaGuardar = codigosPlano.stream()
                .map(codigo -> CodigoRespaldo.crear(usuario.getId(), HashingSupport.sha256Hex(codigo), ahora))
                .toList();
        codigoRespaldoRepositoryPort.guardarTodos(codigosParaGuardar);

        return new ConfirmarMfaResultado(codigosPlano);
    }

    private static List<String> generarCodigosRespaldo() {
        return java.util.stream.IntStream.range(0, CANTIDAD_CODIGOS_RESPALDO)
                .mapToObj(i -> HashingSupport.generarCodigoAlfanumerico(LONGITUD_CODIGO_RESPALDO))
                .toList();
    }
}
