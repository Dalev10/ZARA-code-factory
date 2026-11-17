package com.codefactory.supplychain.inventario.infrastructure.adapter.in.modulo;

import com.codefactory.supplychain.inventario.api.MovimientosAbastecimiento;
import com.codefactory.supplychain.inventario.api.StockInsuficienteException;
import com.codefactory.supplychain.inventario.application.port.in.RegistrarAbastecimientoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementa {@link MovimientosAbastecimiento} (API pública) sobre
 * {@link RegistrarAbastecimientoUseCase}. Solo traduce, incluida la excepción
 * de stock del dominio a la de la API.
 *
 * Módulo: inventario.
 */
@Component
@RequiredArgsConstructor
class MovimientosAbastecimientoAdapter implements MovimientosAbastecimiento {

    private final RegistrarAbastecimientoUseCase abastecimiento;

    @Override
    public void registrarDespacho(
            UUID cdId, UUID varianteId, int cantidadQueSale, int reservaALiberar, UUID referenciaId) {
        try {
            abastecimiento.registrarDespacho(cdId, varianteId, cantidadQueSale, reservaALiberar, referenciaId);
        } catch (com.codefactory.supplychain.inventario.domain.exception.StockInsuficienteException e) {
            throw new StockInsuficienteException();
        }
    }

    @Override
    public void registrarRecepcion(UUID tiendaId, UUID varianteId, int cantidad, UUID referenciaId) {
        abastecimiento.registrarRecepcion(tiendaId, varianteId, cantidad, referenciaId);
    }
}
