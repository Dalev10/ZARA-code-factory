package com.codefactory.supplychain.logistica.application.port.in;

import com.codefactory.supplychain.logistica.domain.model.ParadaRecepcion;
import com.codefactory.supplychain.shared.security.UsuarioActual;

import java.util.List;

/**
 * HU-49: paradas ENTREGADO que todavía no tienen recepción, de las tiendas del usuario.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
public interface ConsultarParadasPorRecibirUseCase {

    List<ParadaRecepcion> listar(UsuarioActual actor);
}
