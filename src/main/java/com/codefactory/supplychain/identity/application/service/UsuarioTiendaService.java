package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.AsignarTiendaAUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.AsignarTiendaComando;
import com.codefactory.supplychain.identity.application.port.in.ListarTiendasDeUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.QuitarTiendaDeUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.out.TiendaAsignable;
import com.codefactory.supplychain.identity.application.port.out.TiendaConsultaPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioTiendaRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.TiendaNoEncontradaException;
import com.codefactory.supplychain.identity.domain.exception.UsuarioNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * HU-32: en qué tiendas puede operar cada usuario (D-09). Mismo comportamiento que la
 * asignación de roles: asignar y quitar no fallan si ya estaba hecho.
 *
 * Una tienda inactiva sí se puede asignar (D-70): la asignación no habilita a operar,
 * porque cada operación valida que la tienda esté ACTIVA; así se puede preparar al
 * personal antes de reactivarla.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Service
@RequiredArgsConstructor
public class UsuarioTiendaService implements AsignarTiendaAUsuarioUseCase, QuitarTiendaDeUsuarioUseCase,
        ListarTiendasDeUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final UsuarioTiendaRepositoryPort usuarioTiendaRepositoryPort;
    private final TiendaConsultaPort tiendaConsultaPort;

    @Override
    public void asignar(AsignarTiendaComando comando) {
        validarQueExisteElUsuario(comando.usuarioId());
        tiendaConsultaPort.buscar(comando.tiendaId()).orElseThrow(TiendaNoEncontradaException::new);
        if (!usuarioTiendaRepositoryPort.existeAsignacion(comando.usuarioId(), comando.tiendaId())) {
            usuarioTiendaRepositoryPort.asignar(comando.usuarioId(), comando.tiendaId());
        }
    }

    @Override
    public void quitar(AsignarTiendaComando comando) {
        usuarioTiendaRepositoryPort.quitar(comando.usuarioId(), comando.tiendaId());
    }

    @Override
    public List<TiendaAsignable> listarTiendas(UUID usuarioId) {
        validarQueExisteElUsuario(usuarioId);
        return tiendaConsultaPort.buscarTodas(usuarioTiendaRepositoryPort.listarTiendasDeUsuario(usuarioId));
    }

    private void validarQueExisteElUsuario(UUID usuarioId) {
        usuarioRepositoryPort.buscarPorId(usuarioId).orElseThrow(UsuarioNoEncontradoException::new);
    }
}
