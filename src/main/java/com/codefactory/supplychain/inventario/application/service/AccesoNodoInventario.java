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

    /**
     * Verifica que el nodo está en el alcance del usuario para consultarlo.
     * El scope {@code inventario:consultar} lo exige el controlador. Un CD no
     * pertenece a ninguna tienda: lo ve quien opera todas o quien administra
     * inventario de CD (D-31).
     */
    public Nodo exigirVisibilidad(UUID nodoId, UsuarioActual usuario) {
        Nodo nodo = nodoRepositoryPort.buscarPorId(nodoId)
                .orElseThrow(() -> new NodoNoEncontradoException(nodoId));
        boolean visible = nodo.getTipo() == TipoNodo.CD
                ? puedeVerCds(usuario)
                : usuario.puedeOperarEn(nodo.getTiendaId());
        if (!visible) {
            throw new NodoNoEncontradoException(nodoId);
        }
        return nodo;
    }

    static boolean puedeVerCds(UsuarioActual usuario) {
        return usuario.tieneScope(UsuarioActual.SCOPE_OPERAR_TODAS_LAS_TIENDAS)
                || usuario.tieneScope(SCOPE_INVENTARIO_CD);
    }

    private static void exigirScope(UsuarioActual usuario, String scope) {
        if (!usuario.tieneScope(scope)) {
            throw new AdministracionInventarioNoPermitidaException(scope);
        }
    }
}
