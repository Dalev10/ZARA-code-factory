package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import com.codefactory.supplychain.identity.application.port.out.RolScopeRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRolRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.domain.model.Scope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScopesUsuarioAdapterTest {

    private final UsuarioRolRepositoryPort usuarioRolRepositoryPort = mock(UsuarioRolRepositoryPort.class);
    private final RolScopeRepositoryPort rolScopeRepositoryPort = mock(RolScopeRepositoryPort.class);
    private final ScopesUsuarioAdapter adapter =
            new ScopesUsuarioAdapter(usuarioRolRepositoryPort, rolScopeRepositoryPort);

    private static Rol rol(String nombre) {
        return Rol.reconstruir(UUID.randomUUID(), nombre, null, Instant.now());
    }

    private static Scope scope(String codigo) {
        return Scope.reconstruir(UUID.randomUUID(), codigo, null, false, Instant.now());
    }

    @Test
    void combinaLosScopesDeTodosLosRolesDelUsuarioSinDuplicados() {
        UUID usuarioId = UUID.randomUUID();
        Rol rolA = rol("ROL_A");
        Rol rolB = rol("ROL_B");
        when(usuarioRolRepositoryPort.listarRolesDeUsuario(usuarioId)).thenReturn(List.of(rolA, rolB));
        when(rolScopeRepositoryPort.listarScopesDeRol(rolA.getId()))
                .thenReturn(List.of(scope("usuarios:leer"), scope("roles:administrar")));
        when(rolScopeRepositoryPort.listarScopesDeRol(rolB.getId()))
                .thenReturn(List.of(scope("usuarios:leer"), scope("scopes:administrar")));

        List<String> scopes = adapter.obtenerScopes(usuarioId);

        assertThat(scopes).containsExactlyInAnyOrder("usuarios:leer", "roles:administrar", "scopes:administrar");
    }

    @Test
    void devuelveListaVaciaSiElUsuarioNoTieneRoles() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRolRepositoryPort.listarRolesDeUsuario(usuarioId)).thenReturn(List.of());

        assertThat(adapter.obtenerScopes(usuarioId)).isEmpty();
    }
}
