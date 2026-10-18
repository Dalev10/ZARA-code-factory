package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.in.AjustarInventarioUseCase;
import com.codefactory.supplychain.inventario.application.port.in.AjusteInventarioComando;
import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.inventario.application.port.out.VariantesCatalogoPort;
import com.codefactory.supplychain.inventario.domain.exception.SkuInexistenteException;
import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * Ajuste de inventario (HU-29, D-29, D-31, D-32).
 *
 * No es transaccional a propósito: delega en {@link InventarioUseCase}, cuyo
 * reintento optimista abre una transacción por intento (D-24).
 * La compuerta de carga, el kardex y el evento viven en
 * {@link InventarioService}.
 */
@Service
@RequiredArgsConstructor
public class AjusteInventarioService implements AjustarInventarioUseCase {

    private final AccesoNodoInventario accesoNodoInventario;
    private final VariantesCatalogoPort variantesCatalogoPort;
    private final InventarioUseCase inventarioUseCase;

    @Override
    public MovimientoInventario ajustar(AjusteInventarioComando comando) {
        accesoNodoInventario.exigirAdministracion(comando.nodoId(), comando.usuario());
        UUID varianteId = variantesCatalogoPort.resolverIdsPorSku(Set.of(comando.sku())).get(comando.sku());
        if (varianteId == null) {
            throw new SkuInexistenteException(comando.sku());
        }
        return inventarioUseCase.aplicarAjuste(
                comando.nodoId(),
                varianteId,
                comando.delta(),
                comando.motivo(),
                comando.comentario(),
                comando.usuario().id());
    }
}
