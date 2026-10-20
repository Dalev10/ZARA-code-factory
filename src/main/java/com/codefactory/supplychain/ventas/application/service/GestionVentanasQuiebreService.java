package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.ventas.application.port.in.GestionarVentanasQuiebreUseCase;
import com.codefactory.supplychain.ventas.application.port.out.VentanaQuiebreRepositoryPort;
import com.codefactory.supplychain.ventas.domain.model.MotivoCierreVentana;
import com.codefactory.supplychain.ventas.domain.model.VentanaQuiebre;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * HU-45. Reglas (D-46, D-47, D-48):
 * <ul>
 *   <li>el Almacén cruza de &gt; 0 a &lt;= 0: abre (una sola abierta por tienda y variante);</li>
 *   <li>a la mano &gt; 0 con una ventana abierta: cierra con REPOSICION;</li>
 *   <li>venta forzada: cierra con VENTA_FORZADA y nunca abre.</li>
 * </ul>
 * Una carga en 0 o un ajuste entre dos saldos &lt;= 0 no cruzan, así que no abren.
 */
@Service
@Transactional
public class GestionVentanasQuiebreService implements GestionarVentanasQuiebreUseCase {

    private final VentanaQuiebreRepositoryPort ventanas;

    public GestionVentanasQuiebreService(VentanaQuiebreRepositoryPort ventanas) {
        this.ventanas = ventanas;
    }

    @Override
    public void alCambiarExistencias(ExistenciasCambiadas cambio) {
        if (cambio.tiendaId() == null) {
            return; // CD o Bodega: no generan ventanas
        }
        Optional<VentanaQuiebre> abierta = ventanas.buscarAbierta(cambio.tiendaId(), cambio.varianteId());

        if (cambio.forzado()) {
            abierta.ifPresent(v -> cerrar(v, cambio.ocurridoEn(), MotivoCierreVentana.VENTA_FORZADA));
        } else if (cambio.aLaMano() > 0) {
            abierta.ifPresent(v -> cerrar(v, cambio.ocurridoEn(), MotivoCierreVentana.REPOSICION));
        } else if (cambio.aLaManoAnterior() > 0 && abierta.isEmpty()) {
            ventanas.abrirSiAusente(VentanaQuiebre.abrir(cambio.tiendaId(), cambio.varianteId(), cambio.ocurridoEn()));
        }
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
