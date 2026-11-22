package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.in.RegistrarAbastecimientoUseCase;
import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.BodegaTiendaNoEncontradaException;
import com.codefactory.supplychain.inventario.domain.exception.CentroDistribucionNoEncontradoException;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Resuelve el nodo del CD o de la Bodega_Tienda y delega el movimiento en
 * {@link InventarioService}, que registra el kardex y publica
 * {@code ExistenciasActualizadas}.
 *
 * Sin {@code @Retryable}: entra directo al servicio transaccional, sin pasar
 * por {@code InventarioRetryableService}, para unirse a la transacción del
 * llamador (D-24).
 */
@Service
@RequiredArgsConstructor
public class AbastecimientoInventarioService implements RegistrarAbastecimientoUseCase {

    private final InventarioService inventarioService;
    private final NodoRepositoryPort nodoRepositoryPort;

    @Override
    @Transactional
    public void registrarDespacho(UUID cdId, UUID varianteId, int cantidadQueSale, int reservaALiberar,
                                  UUID referenciaId) {
        UUID nodoCd = nodoRepositoryPort.buscarPorCdId(cdId)
                .orElseThrow(() -> new CentroDistribucionNoEncontradoException("El CD " + cdId + " no existe"))
                .getId();
        inventarioService.despacharAbastecimiento(nodoCd, varianteId, cantidadQueSale, reservaALiberar, referenciaId);
    }

    @Override
    @Transactional
    public void registrarRecepcion(UUID tiendaId, UUID varianteId, int cantidad, UUID referenciaId) {
        UUID bodega = nodoRepositoryPort.buscarPorTiendaYTipo(tiendaId, TipoNodo.BODEGA_TIENDA)
                .orElseThrow(BodegaTiendaNoEncontradaException::new)
                .getId();
        inventarioService.recibirAbastecimiento(bodega, varianteId, cantidad, referenciaId);
    }
}
