package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.TraspasoAutomaticoUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarExistenciasPort;
import com.codefactory.supplychain.reposicion.application.port.out.EjecutarTraspasoPort;
import com.codefactory.supplychain.reposicion.application.port.out.ResolverUmbralPort;
import com.codefactory.supplychain.reposicion.application.port.out.ResolverUmbralPort.UmbralAplicable;
import com.codefactory.supplychain.reposicion.application.port.out.TraspasoInternoRepository;
import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementa el traspaso automático:
 * <ol>
 *   <li>Resuelve el umbral vigente para la variante en la tienda.</li>
 *   <li>Comprueba si el Almacén está por debajo del mínimo.</li>
 *   <li>Calcula la cantidad: {@code MIN(objetivo − MAX(almacén, 0), bodega)}.</li>
 *   <li>Si cantidad > 0, persiste el traspaso y mueve el stock.</li>
 * </ol>
 *
 * El método corre en su propia transacción (REQUIRES_NEW) porque lo invoca un
 * listener AFTER_COMMIT: la transacción original ya cerró y no podemos unirla.
 *
 * Módulo: reposicion — Reposición interna.
 */
@Service
public class TraspasoAutomaticoService implements TraspasoAutomaticoUseCase {

    private final ResolverUmbralPort resolverUmbral;
    private final ConsultarExistenciasPort consultarExistencias;
    private final EjecutarTraspasoPort ejecutarTraspaso;
    private final TraspasoInternoRepository traspasoRepository;

    public TraspasoAutomaticoService(ResolverUmbralPort resolverUmbral,
                                     ConsultarExistenciasPort consultarExistencias,
                                     EjecutarTraspasoPort ejecutarTraspaso,
                                     TraspasoInternoRepository traspasoRepository) {
        this.resolverUmbral = resolverUmbral;
        this.consultarExistencias = consultarExistencias;
        this.ejecutarTraspaso = ejecutarTraspaso;
        this.traspasoRepository = traspasoRepository;
    }

    @Override
    @Transactional
    public Optional<TraspasoInterno> evaluar(UUID tiendaId, UUID varianteId) {
        // 1. ¿Hay umbral configurado para esta variante en esta tienda?
        Optional<UmbralAplicable> umbralOpt = resolverUmbral.resolver(tiendaId, varianteId);
        if (umbralOpt.isEmpty()) {
            return Optional.empty();
        }
        UmbralAplicable umbral = umbralOpt.get();

        // 2. ¿Dispara? (D-53): almacén actual < cantidad_minima
        int almacen = consultarExistencias.consultarStockAlmacen(tiendaId, varianteId).orElse(0);
        if (almacen >= umbral.cantidadMinima()) {
            return Optional.empty();
        }

        int bodega = consultarExistencias.consultarStockBodega(tiendaId, varianteId).orElse(0);
        TraspasoInterno traspaso = TraspasoInterno.calcularAutomatico(
                varianteId, tiendaId, almacen, bodega, umbral.cantidadObjetivo());

        if (traspaso == null) {
            // Bodega vacía o almacén ya en objetivo (puede ocurrir si otro hilo actuó antes)
            return Optional.empty();
        }

        // 4. Aplicar movimiento contable en inventario y persistir el traspaso
        ejecutarTraspaso.traspasar(tiendaId, varianteId, traspaso.getCantidad(), traspaso.getId());
        TraspasoInterno guardado = traspasoRepository.guardar(traspaso);

        return Optional.of(guardado);
    }
}
