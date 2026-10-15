package com.codefactory.supplychain.soporte;

import com.codefactory.supplychain.shared.security.ContextoAutorizacionPort;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import com.codefactory.supplychain.soporte.UsuariosDePrueba.UsuarioDePrueba;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-33: la utilidad entrega usuarios que el sistema reconoce de verdad, con los scopes de su
 * rol y sus tiendas, y un token válido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(UsuariosDePrueba.class)
class UsuariosDePruebaTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UsuariosDePrueba usuarios;

    @Autowired
    private ContextoAutorizacionPort contextoAutorizacionPort;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unVendedorSeResuelveConLosScopesDeSuRolYOperaSoloEnSusTiendas() {
        UUID suTienda = crearTienda();
        UsuarioDePrueba vendedor = usuarios.conRol("VENDEDOR", suTienda);

        UsuarioActual resuelto = contextoAutorizacionPort.resolver(vendedor.id()).orElseThrow();

        assertThat(resuelto.scopes()).containsExactlyInAnyOrder(
                "ventas:registrar", "ventas:consultar", "inventario:consultar", "ventas:forzar-sin-stock");
        assertThat(resuelto.puedeOperarEn(suTienda)).isTrue();
        assertThat(resuelto.puedeOperarEn(crearTienda())).isFalse();
    }

    @Test
    void conElScopeDeAccesoGlobalOperaEnCualquierTiendaSinAsignaciones() {
        UsuarioDePrueba global = usuarios.conScopes(Set.of(UsuarioActual.SCOPE_OPERAR_TODAS_LAS_TIENDAS));

        assertThat(contextoAutorizacionPort.resolver(global.id()).orElseThrow().puedeOperarEn(crearTienda()))
                .isTrue();
    }

    @Test
    void elTokenAutenticaDeVerdad() throws Exception {
        UsuarioDePrueba vendedor = usuarios.conRol("VENDEDOR");

        // Autenticado (no 401) pero sin el scope de administrar usuarios: 403.
        mockMvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, vendedor.bearer()))
                .andExpect(status().isForbidden());
    }

    private UUID crearTienda() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre) VALUES (?, ?)", id, "tienda-" + id);
        return id;
    }
}
