package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.ventas.application.port.in.GestionarVentanasQuiebreUseCase;
import com.codefactory.supplychain.ventas.application.port.out.AlmacenTiendaPort;
import com.codefactory.supplychain.ventas.application.port.out.VentanaQuiebreRepositoryPort;
import com.codefactory.supplychain.ventas.domain.model.MotivoCierreVentana;
import com.codefactory.supplychain.ventas.domain.model.VentanaQuiebre;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * HU-36. Reglas:
 * <ul>
 *   <li>a la mano &lt;= 0 en un movimiento que pudo bajar el saldo: abre (una sola abierta por tienda y variante);</li>
 *   <li>a la mano &gt; 0: cierra con REPOSICION;</li>
 *   <li>venta forzada: cierra con VENTA_FORZADA y nunca abre;</li>
 *   <li>CARGA_INICIAL en cero no abre; los movimientos que solo suman no abren.</li>
 * </ul>
 * LIMITACIÓN: ExistenciasActualizadas no trae el saldo anterior, así que el cruce &gt;0 -&gt; &lt;=0 se
 * infiere por estado (no hay ventana abierta). Un AJUSTE que mueva el saldo entre dos valores &lt;= 0 podría
 * abrir una ventana de más; la reconciliación (HU-37) la deja reportada. Ver CONTRATOS_PENDIENTES.md.
 */
@Service
@Transactional
public class GestionVentanasQuiebreService implements GestionarVentanasQuiebreUseCase {

    private static final String CARGA_INICIAL = "CARGA_INICIAL";
    private static final Set<String> MOVIMIENTOS_QUE_SOLO_SUMAN =
            Set.of("ANULACION_VENTA", "TRASPASO_ENTRADA");

    private final VentanaQuiebreRepositoryPort ventanas;
    private final AlmacenTiendaPort almacenes;

    public GestionVentanasQuiebreService(VentanaQuiebreRepositoryPort ventanas, AlmacenTiendaPort almacenes) {
        this.ventanas = ventanas;
        this.almacenes = almacenes;
    }

    @Override
    public void alCambiarExistencias(ExistenciasCambiadas cambio) {
        Optional<UUID> tienda = almacenes.tiendaDeAlmacen(cambio.nodoId());
        if (tienda.isEmpty()) {
            return; // CD o Bodega: no generan ventanas
        }
        UUID tiendaId = tienda.get();
        Optional<VentanaQuiebre> abierta = ventanas.buscarAbierta(tiendaId, cambio.varianteId());

        if (cambio.forzado()) {
            abierta.ifPresent(v -> cerrar(v, cambio.ocurridoEn(), MotivoCierreVentana.VENTA_FORZADA));
        } else if (cambio.aLaMano() > 0) {
            abierta.ifPresent(v -> cerrar(v, cambio.ocurridoEn(), MotivoCierreVentana.REPOSICION));
        } else if (abierta.isEmpty() && puedeAbrir(cambio.tipoMovimiento())) {
            ventanas.abrirSiAusente(VentanaQuiebre.abrir(tiendaId, cambio.varianteId(), cambio.ocurridoEn()));
        }
    }

    private static boolean puedeAbrir(String tipoMovimiento) {
        return !CARGA_INICIAL.equals(tipoMovimiento) && !MOVIMIENTOS_QUE_SOLO_SUMAN.contains(tipoMovimiento);
    }

    private void cerrar(VentanaQuiebre ventana, Instant fecha, MotivoCierreVentana motivo) {
        ventana.cerrar(fecha, motivo);
        ventanas.guardar(ventana);
    }

    @Override
    public int alDesactivarTienda(UUID tiendaId, Instant fecha) {
        var abiertas = ventanas.listarAbiertasPorTienda(tiendaId);
        abiertas.forEach(v -> cerrar(v, fecha, MotivoCierreVentana.TIENDA_DESACTIVADA));
        return abiertas.size();
    }
}
