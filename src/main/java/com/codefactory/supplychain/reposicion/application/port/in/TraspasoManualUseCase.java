package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import com.codefactory.supplychain.shared.security.UsuarioActual;

import java.util.UUID;

/**
 * Registra un traspaso manual Bodega_Tienda → Almacén ejecutado físicamente por un
 * operario (HU-49, D-56).
 * <p>
 * Diferencias respecto al automático:
 * <ul>
 *   <li>El origen siempre es MANUAL y {@code registradoPor} es el usuario del token.</li>
 *   <li>La cantidad la decide el operario, no el algoritmo; nunca deja la Bodega en negativo.</li>
 * </ul>
 *
 * Módulo: reposicion — Reposición interna.
 */
public interface TraspasoManualUseCase {

    /**
     * @throws com.codefactory.supplychain.reposicion.domain.exception.TiendaNoPermitidaException
     *         si el usuario no tiene asignada la tienda
     * @throws com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException
     *         si la tienda o la variante no existen
     * @throws com.codefactory.supplychain.reposicion.domain.exception.BodegaInsuficienteException
     *         si la Bodega no tiene las unidades solicitadas
     */
    TraspasoInterno registrar(UsuarioActual usuario, UUID tiendaId, UUID varianteId, int cantidad);
}
