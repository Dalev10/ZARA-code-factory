package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.AdministracionInventarioNoPermitidaException;
import com.codefactory.supplychain.inventario.domain.exception.NodoNoEncontradoException;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Autoriza la administración del inventario de un nodo (carga inicial y
 * ajustes). El scope depende del tipo de nodo (D-31), por eso no alcanza un
 * {@code @PreAuthorize} fijo.
 *
 * Un nodo de una tienda fuera del alcance del usuario responde como
 * inexistente (404), para no revelar que existe.
 */
@Component
@RequiredArgsConstructor
public class AccesoNodoInventario {

    static final String SCOPE_INVENTARIO_TIENDAS = "inventario:administrar";
    static final String SCOPE_INVENTARIO_CD = "inventario-cd:administrar";

    private final NodoRepositoryPort nodoRepositoryPort;

    public Nodo exigirAdministracion(UUID nodoId, UsuarioActual usuario) {
        Nodo nodo = nodoRepositoryPort.buscarPorId(nodoId)
                .orElseThrow(() -> new NodoNoEncontradoException(nodoId));
        if (nodo.getTipo() == TipoNodo.CD) {
            exigirScope(usuario, SCOPE_INVENTARIO_CD);
            return nodo;
        }
        if (!usuario.puedeOperarEn(nodo.getTiendaId())) {
            throw new NodoNoEncontradoException(nodoId);
        }
        exigirScope(usuario, SCOPE_INVENTARIO_TIENDAS);
        return nodo;
    }

    private static void exigirScope(UsuarioActual usuario, String scope) {
        if (!usuario.tieneScope(scope)) {
            throw new AdministracionInventarioNoPermitidaException(scope);
        }
    }
}
