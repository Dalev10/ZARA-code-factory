package com.codefactory.supplychain.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutorizacionAdminTest {

    private final VerificadorRolAdminPort verificadorRolAdminPort = mock(VerificadorRolAdminPort.class);
    private final AutorizacionAdmin autorizacionAdmin = new AutorizacionAdmin(verificadorRolAdminPort);

    @Test
    void esAdminDelegaAlPuertoConElUsuarioIdDelPrincipal() {
        UUID usuarioId = UUID.randomUUID();
        Authentication authentication = new UsernamePasswordAuthenticationToken(usuarioId, null, List.of());
        when(verificadorRolAdminPort.esAdmin(usuarioId)).thenReturn(true);

        assertThat(autorizacionAdmin.esAdmin(authentication)).isTrue();
    }

    @Test
    void esAdminEsFalseSiElPuertoDiceQueNoLoEs() {
        UUID usuarioId = UUID.randomUUID();
        Authentication authentication = new UsernamePasswordAuthenticationToken(usuarioId, null, List.of());
        when(verificadorRolAdminPort.esAdmin(usuarioId)).thenReturn(false);

        assertThat(autorizacionAdmin.esAdmin(authentication)).isFalse();
    }

    @Test
    void esAdminEsFalseSiNoHayAutenticacion() {
        assertThat(autorizacionAdmin.esAdmin(null)).isFalse();
    }

    @Test
    void esAdminEsFalseSiElPrincipalNoEsUnUuid() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("no-es-un-uuid", null, List.of());

        assertThat(autorizacionAdmin.esAdmin(authentication)).isFalse();
    }
}
