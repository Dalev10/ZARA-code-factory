package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.RegistrarUsuarioComando;
import com.codefactory.supplychain.identity.application.port.in.RegistrarUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.out.PasswordComprometidaPort;
import com.codefactory.supplychain.identity.application.port.out.PasswordHasherPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.EmailYaRegistradoException;
import com.codefactory.supplychain.identity.domain.exception.PasswordComprometidaException;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.Password;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Service
@RequiredArgsConstructor
public class RegistrarUsuarioService implements RegistrarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final PasswordComprometidaPort passwordComprometidaPort;

    @Override
    public Usuario registrar(RegistrarUsuarioComando comando) {
        Email email = Email.de(comando.email());
        if (usuarioRepositoryPort.existePorEmail(email)) {
            throw new EmailYaRegistradoException("Ya existe un usuario registrado con este email");
        }

        Password password = Password.de(comando.password());
        if (passwordComprometidaPort.estaComprometida(password)) {
            throw new PasswordComprometidaException(
                    "La contraseña ingresada aparece en filtraciones conocidas; elegí otra");
        }

        PasswordHash passwordHash = passwordHasherPort.hashear(password);
        Usuario usuario = Usuario.crear(email, comando.nombreCompleto(), passwordHash);
        return usuarioRepositoryPort.guardar(usuario);
    }
}
