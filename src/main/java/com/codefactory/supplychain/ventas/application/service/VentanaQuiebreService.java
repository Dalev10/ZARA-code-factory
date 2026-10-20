package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.ventas.application.port.in.VentanaQuiebreUseCase;
import com.codefactory.supplychain.ventas.application.port.out.AlmacenTiendaPort;
import com.codefactory.supplychain.ventas.application.port.out.HistorialStockPort;
import com.codefactory.supplychain.ventas.application.port.out.HistorialStockPort.CambioStock;
import com.codefactory.supplychain.ventas.application.port.out.StockAlmacenPort;
import com.codefactory.supplychain.ventas.application.port.out.VentanaQuiebreRepositoryPort;
import com.codefactory.supplychain.ventas.domain.exception.AccesoTiendaProhibidoException;
import com.codefactory.supplychain.ventas.domain.exception.TiendaSinAlmacenException;
import com.codefactory.supplychain.ventas.domain.exception.VentanaInvalidaException;
import com.codefactory.supplychain.ventas.domain.exception.VentanaNoEncontradaException;
import com.codefactory.supplychain.ventas.domain.model.MotivoCierreVentana;
import com.codefactory.supplychain.ventas.domain.model.VentanaQuiebre;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * HU-46. La reconciliación recorre el kardex real: un periodo empieza cuando el saldo pasa de &gt; 0 a &lt;= 0
 * y termina cuando vuelve a &gt; 0. No inventa fechas: si el historial no permite conocer el saldo previo,
 * no se reconstruye nada para ese par. Es no destructiva: crea, cierra o corrige; lo que no tiene
 * respaldo en el kardex solo se reporta.
 */
@Service
@Transactional(readOnly = true)
public class VentanaQuiebreService implements VentanaQuiebreUseCase {

    /** Las fechas del evento y del kardex vienen del mismo instante pero pueden diferir por redondeo. */
    static final Duration TOLERANCIA = Duration.ofSeconds(1);

    private final VentanaQuiebreRepositoryPort ventanas;
    private final AlmacenTiendaPort almacenes;
    private final StockAlmacenPort stock;
    private final HistorialStockPort historial;
    private final Clock reloj;

    public VentanaQuiebreService(VentanaQuiebreRepositoryPort ventanas, AlmacenTiendaPort almacenes,
                                 StockAlmacenPort stock, HistorialStockPort historial, Clock reloj) {
        this.ventanas = ventanas;
        this.almacenes = almacenes;
        this.stock = stock;
        this.historial = historial;
        this.reloj = reloj;
    }

    @Override
    public Page<VentanaQuiebre> consultar(UsuarioActual usuario, ConsultaFiltros filtros, Pageable pageable) {
        if (AccesoTiendas.filtroFueraDeAlcance(usuario, filtros.tiendaId())) {
            return Page.empty(pageable);
        }
        return ventanas.buscar(new VentanaQuiebreRepositoryPort.Filtro(AccesoTiendas.tiendasPermitidas(usuario),
                filtros.tiendaId(), filtros.varianteId(), filtros.abierta(), filtros.desde(), filtros.hasta()), pageable);
    }

    @Override
    public VentanaQuiebre obtener(UsuarioActual usuario, UUID id) {
        return ventanas.buscarPorId(id)
                .filter(v -> usuario.puedeOperarEn(v.getTiendaId()))
                .orElseThrow(() -> new VentanaNoEncontradaException(id));
    }

    @Override
    @Transactional
    public ResultadoReconciliacion reconciliar(ReconciliarComando comando) {
        if (comando.tiendaId() == null) {
            throw new VentanaInvalidaException("La tienda es obligatoria para reconciliar");
        }
        if (!comando.usuario().puedeOperarEn(comando.tiendaId())) {
            throw new AccesoTiendaProhibidoException(comando.tiendaId());
        }
        Instant desde = comando.desde() == null ? Instant.EPOCH : comando.desde();
        Instant hasta = comando.hasta() == null ? Instant.now(reloj) : comando.hasta();
        if (hasta.isBefore(desde)) {
            throw new VentanaInvalidaException("El rango de fechas es inválido");
        }
        UUID almacenId = almacenes.almacenDeTienda(comando.tiendaId())
                .orElseThrow(() -> new TiendaSinAlmacenException(comando.tiendaId()));

        Set<UUID> variantes = new LinkedHashSet<>();
        if (comando.varianteId() != null) {
            variantes.add(comando.varianteId());
        } else {
            variantes.addAll(stock.variantesConExistencias(almacenId));
            variantes.addAll(ventanas.variantesConVentanas(comando.tiendaId()));
        }

        Acumulador total = new Acumulador();
        for (UUID varianteId : variantes) {
            reconciliarPar(comando.tiendaId(), almacenId, varianteId, desde, hasta, total);
        }
        return new ResultadoReconciliacion(variantes.size(), total.creadas, total.cerradas, total.corregidas,
                total.sinCambios, total.sinRespaldo);
    }

    private void reconciliarPar(UUID tiendaId, UUID almacenId, UUID varianteId, Instant desde, Instant hasta,
                                Acumulador total) {
        List<CambioStock> cambios = new ArrayList<>(historial.cambios(almacenId, varianteId, desde, hasta));
        cambios.sort(Comparator.comparing(CambioStock::fecha));
        List<Periodo> periodos = reconstruir(cambios);

        List<VentanaQuiebre> existentes = new ArrayList<>(ventanas.listarPorTiendaYVariante(tiendaId, varianteId));
        Set<UUID> respaldadas = new HashSet<>();

        for (Periodo periodo : periodos) {
            VentanaQuiebre existente = existentes.stream()
                    .filter(v -> cercanas(v.getFechaInicio(), periodo.inicio()))
                    .findFirst().orElse(null);
            if (existente == null) {
                total.creadas += crear(tiendaId, varianteId, periodo, existentes);
                continue;
            }
            respaldadas.add(existente.getId());
            conciliar(existente, periodo, total);
        }
        existentes.stream()
                .filter(v -> !respaldadas.contains(v.getId()))
                .filter(v -> !v.getFechaInicio().isBefore(desde) && !v.getFechaInicio().isAfter(hasta))
                .forEach(v -> total.sinRespaldo++);
    }

    /** Devuelve 1 si creó la ventana, 0 si no pudo (ya hay otra abierta distinta). */
    private int crear(UUID tiendaId, UUID varianteId, Periodo periodo, List<VentanaQuiebre> existentes) {
        VentanaQuiebre nueva = VentanaQuiebre.abrir(tiendaId, varianteId, periodo.inicio());
        if (periodo.fin() == null) {
            return ventanas.abrirSiAusente(nueva) ? 1 : 0;
        }
        nueva.cerrar(periodo.fin(), MotivoCierreVentana.REPOSICION);
        ventanas.guardar(nueva);
        existentes.add(nueva);
        return 1;
    }

    private void conciliar(VentanaQuiebre existente, Periodo periodo, Acumulador total) {
        if (existente.estaAbierta() && periodo.fin() != null) {
            existente.cerrar(periodo.fin(), MotivoCierreVentana.REPOSICION);
            ventanas.guardar(existente);
            total.cerradas++;
        } else if (!existente.estaAbierta() && periodo.fin() != null
                && existente.getMotivoCierre() == MotivoCierreVentana.REPOSICION
                && !cercanas(existente.getFechaFin(), periodo.fin())) {
            existente.corregirCierre(periodo.fin(), MotivoCierreVentana.REPOSICION);
            ventanas.guardar(existente);
            total.corregidas++;
        } else {
            // Incluye cierres por VENTA_FORZADA / TIENDA_DESACTIVADA: el kardex no distingue ese motivo y no se pisan.
            total.sinCambios++;
        }
    }

    /**
     * Recorre los movimientos en orden. El saldo previo al primero se deduce de él mismo
     * (saldoResultante - cantidad).
     */
    static List<Periodo> reconstruir(List<CambioStock> cambios) {
        List<Periodo> periodos = new ArrayList<>();
        if (cambios.isEmpty()) {
            return periodos;
        }
        CambioStock primero = cambios.get(0);
        int saldoPrevio = primero.saldoResultante() - primero.cantidad();
        Instant inicioAbierto = null;
        for (CambioStock cambio : cambios) {
            if (inicioAbierto == null && saldoPrevio > 0 && cambio.saldoResultante() <= 0) {
                inicioAbierto = cambio.fecha();
            } else if (inicioAbierto != null && cambio.saldoResultante() > 0) {
                periodos.add(new Periodo(inicioAbierto, cambio.fecha()));
                inicioAbierto = null;
            }
            saldoPrevio = cambio.saldoResultante();
        }
        if (inicioAbierto != null) {
            periodos.add(new Periodo(inicioAbierto, null));
        }
        return periodos;
    }

    private static boolean cercanas(Instant a, Instant b) {
        return Duration.between(a, b).abs().compareTo(TOLERANCIA) <= 0;
    }

    record Periodo(Instant inicio, Instant fin) {
    }

    private static final class Acumulador {
        int creadas;
        int cerradas;
        int corregidas;
        int sinCambios;
        int sinRespaldo;
    }
}
