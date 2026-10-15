package com.codefactory.supplychain.soporte;

import com.codefactory.supplychain.identity.application.port.out.AccessTokenGeneratorPort;
import com.codefactory.supplychain.identity.application.port.out.RolRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.RolScopeRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.ScopeRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRolRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioTiendaRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;

import java.util.Set;
import java.util.UUID;

/**
 * HU-33: crea usuarios listos para probar autorización, con sus roles, scopes y tiendas,
 * y su access token. Evita copiar en cada test el armado de usuario + rol + login.
 *
 * El token se genera directo (sin login HTTP), así que sirve en cualquier
 * {@code @SpringBootTest}, con o sin MockMvc. Uso:
 * <pre>
 * &#64;Import(UsuariosDePrueba.class)
 * ...
 * UsuarioDePrueba vendedor = usuarios.conRol("VENDEDOR", tiendaId);
 * mockMvc.perform(post(...).header(HttpHeaders.AUTHORIZATION, vendedor.bearer()));
 * </pre>
 * Las tiendas que se pasan deben existir (usuario_tienda tiene FK a tienda).
 */
@TestComponent
@RequiredArgsConstructor
public class UsuariosDePrueba {

    // Nunca se usa: el token se genera sin login, así que la contraseña no importa.
    private static final PasswordHash HASH_SIN_USO = PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv");

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final RolRepositoryPort rolRepositoryPort;
    private final ScopeRepositoryPort scopeRepositoryPort;
    private final RolScopeRepositoryPort rolScopeRepositoryPort;
    private final UsuarioRolRepositoryPort usuarioRolRepositoryPort;
    private final UsuarioTiendaRepositoryPort usuarioTiendaRepositoryPort;
    private final AccessTokenGeneratorPort accessTokenGeneratorPort;

    /**
     * Usuario con uno de los roles sembrados (VENDEDOR, SUPERVISOR_TIENDA, OPERADOR_CD, ADMIN).
     */
    public UsuarioDePrueba conRol(String nombreRol, UUID... tiendas) {
        Rol rol = rolRepositoryPort.buscarPorNombre(nombreRol)
                .orElseThrow(() -> new IllegalArgumentException("No existe el rol " + nombreRol));
        return crear(rol, tiendas);
    }

    /**
     * Usuario con exactamente estos scopes (deben existir), mediante un rol creado solo para él.
     */
    public UsuarioDePrueba conScopes(Set<String> scopes, UUID... tiendas) {
        Rol rol = rolRepositoryPort.guardar(Rol.crear("ROL_PRUEBA_" + UUID.randomUUID(), null));
        for (String codigo : scopes) {
            UUID scopeId = scopeRepositoryPort.buscarPorCodigo(codigo)
                    .orElseThrow(() -> new IllegalArgumentException("No existe el scope " + codigo))
                    .getId();
            rolScopeRepositoryPort.asignar(rol.getId(), scopeId);
        }
        return crear(rol, tiendas);
    }

    private UsuarioDePrueba crear(Rol rol, UUID... tiendas) {
        Usuario usuario = usuarioRepositoryPort.guardar(Usuario.crear(
                Email.de("prueba-" + UUID.randomUUID() + "@ejemplo.com"), "Usuario de Prueba", HASH_SIN_USO));
        usuarioRolRepositoryPort.asignar(usuario.getId(), rol.getId());
        for (UUID tiendaId : tiendas) {
            usuarioTiendaRepositoryPort.asignar(usuario.getId(), tiendaId);
        }
        return new UsuarioDePrueba(usuario.getId(), accessTokenGeneratorPort.generar(usuario));
    }

    public record UsuarioDePrueba(UUID id, String token) {

        public String bearer() {
            return "Bearer " + token;
        }
    }
}
