package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.UsuarioJpaRepository;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.UsuarioJpaRepository.FilaContextoAutorizacion;
import com.codefactory.supplychain.shared.security.ContextoAutorizacionPort;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resuelve en una sola consulta el estado, los scopes y las tiendas asignadas del usuario
 * de cada request (antes eran 2 + 2 consultas por rol).
 *
 * Solo opera un usuario ACTIVO: corta INACTIVO y BLOQUEADO (bloqueo administrativo).
 * El bloqueo temporal por intentos fallidos no corta la sesión, porque deja el estado
 * en ACTIVO y solo fija bloqueado_hasta: así un atacante que falla logins a propósito
 * no puede expulsar al usuario legítimo.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
@RequiredArgsConstructor
public class ContextoAutorizacionAdapter implements ContextoAutorizacionPort {

    private final UsuarioJpaRepository usuarioJpaRepository;

    @Override
    public Optional<UsuarioActual> resolver(UUID usuarioId) {
        List<FilaContextoAutorizacion> filas = usuarioJpaRepository.buscarContextoAutorizacion(usuarioId);
        if (filas.isEmpty() || !EstadoUsuario.ACTIVO.name().equals(filas.getFirst().getEstado())) {
            return Optional.empty();
        }
        return Optional.of(new UsuarioActual(usuarioId,
                columnaSinNulos(filas, FilaContextoAutorizacion::getCodigo),
                columnaSinNulos(filas, FilaContextoAutorizacion::getTienda)));
    }

    private static <T> Set<T> columnaSinNulos(List<FilaContextoAutorizacion> filas,
                                              Function<FilaContextoAutorizacion, T> columna) {
        return filas.stream().map(columna).filter(Objects::nonNull).collect(Collectors.toSet());
    }
}
