package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.ventas.application.port.in.SolicitudNoSatisfechaUseCase;
import com.codefactory.supplychain.ventas.application.port.out.AlmacenTiendaPort;
import com.codefactory.supplychain.ventas.application.port.out.SolicitudNoSatisfechaRepositoryPort;
import com.codefactory.supplychain.ventas.application.port.out.StockAlmacenPort;
import com.codefactory.supplychain.ventas.application.port.out.VarianteCatalogoPort;
import com.codefactory.supplychain.ventas.domain.exception.AccesoTiendaProhibidoException;
import com.codefactory.supplychain.ventas.domain.exception.ClaveIdempotenciaReutilizadaException;
import com.codefactory.supplychain.ventas.domain.exception.SolicitudInvalidaException;
import com.codefactory.supplychain.ventas.domain.exception.SolicitudNoEncontradaException;
import com.codefactory.supplychain.ventas.domain.exception.TiendaSinAlmacenException;
import com.codefactory.supplychain.ventas.domain.exception.VarianteNoEncontradaException;
import com.codefactory.supplychain.ventas.domain.model.EstadoSolicitud;
import com.codefactory.supplychain.ventas.domain.model.SolicitudNoSatisfecha;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** HU-43 y HU-44. Opera solo sobre la persistencia de ventas; únicamente el registro consulta Inventario. */
@Service
@Transactional(readOnly = true)
public class SolicitudNoSatisfechaService implements SolicitudNoSatisfechaUseCase {

    private final SolicitudNoSatisfechaRepositoryPort repositorio;
    private final VarianteCatalogoPort catalogo;
    private final AlmacenTiendaPort almacenes;
    private final StockAlmacenPort stock;
    private final Clock reloj;

    public SolicitudNoSatisfechaService(SolicitudNoSatisfechaRepositoryPort repositorio, VarianteCatalogoPort catalogo,
                                        AlmacenTiendaPort almacenes, StockAlmacenPort stock, Clock reloj) {
        this.repositorio = repositorio;
        this.catalogo = catalogo;
        this.almacenes = almacenes;
        this.stock = stock;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public ResultadoRegistro registrar(RegistrarComando comando) {
        UsuarioActual usuario = comando.usuario();
        int cantidad = comando.cantidad() == null ? 1 : comando.cantidad();
        if (cantidad <= 0) {
            throw new SolicitudInvalidaException("La cantidad solicitada debe ser mayor que cero");
        }
        if (comando.claveIdempotencia() == null || comando.claveIdempotencia().isBlank()) {
            throw new SolicitudInvalidaException("La clave de idempotencia es obligatoria");
        }
        if (comando.tiendaId() == null || !usuario.puedeOperarEn(comando.tiendaId())) {
            throw new AccesoTiendaProhibidoException(comando.tiendaId());
        }

        var previa = repositorio.buscarPorClave(comando.claveIdempotencia());
        if (previa.isPresent()) {
            return repeticion(previa.get(), comando, cantidad);
        }

        if (comando.varianteId() == null || !catalogo.existe(comando.varianteId())) {
            throw new VarianteNoEncontradaException(comando.varianteId());
        }
        UUID almacenId = almacenes.almacenDeTienda(comando.tiendaId())
                .orElseThrow(() -> new TiendaSinAlmacenException(comando.tiendaId()));
        // El stock observado se guarda tal cual: nunca se rechaza por tener stock > 0 (inventario fantasma).
        Integer stockSistema = stock.aLaMano(almacenId, comando.varianteId()).orElse(null);

        SolicitudNoSatisfecha nueva = SolicitudNoSatisfecha.registrar(comando.tiendaId(), comando.varianteId(),
                cantidad, stockSistema, Instant.now(reloj), usuario.id(), comando.claveIdempotencia());
        if (repositorio.insertarSiAusente(nueva)) {
            return new ResultadoRegistro(nueva, true);
        }
        // Otra petición con la misma clave ganó la carrera: se trata como repetición.
        SolicitudNoSatisfecha ganadora = repositorio.buscarPorClave(comando.claveIdempotencia()).orElseThrow();
        return repeticion(ganadora, comando, cantidad);
    }

    private ResultadoRegistro repeticion(SolicitudNoSatisfecha previa, RegistrarComando comando, int cantidad) {
        if (previa.esRepeticionDe(comando.usuario().id(), comando.tiendaId(), comando.varianteId(), cantidad)) {
            return new ResultadoRegistro(previa, false);
        }
        throw new ClaveIdempotenciaReutilizadaException();
    }

    @Override
    public Page<SolicitudNoSatisfecha> consultar(UsuarioActual usuario, ConsultaFiltros filtros, Pageable pageable) {
        if (AccesoTiendas.filtroFueraDeAlcance(usuario, filtros.tiendaId())) {
            return Page.empty(pageable);
        }
        return repositorio.buscar(new SolicitudNoSatisfechaRepositoryPort.Filtro(
                AccesoTiendas.tiendasPermitidas(usuario), filtros.tiendaId(), filtros.varianteId(),
                filtros.estado(), filtros.desde(), filtros.hasta()), pageable);
    }

    @Override
    public SolicitudNoSatisfecha obtener(UsuarioActual usuario, UUID id) {
        return repositorio.buscarPorId(id)
                .filter(s -> usuario.puedeOperarEn(s.getTiendaId()))
                .orElseThrow(() -> new SolicitudNoEncontradaException(id));
    }

    @Override
    @Transactional
    /** Igual que la venta (D-44): una segunda anulación devuelve la solicitud ya anulada, sin cambios. */
    public SolicitudNoSatisfecha anular(UsuarioActual usuario, UUID id, String motivo) {
        SolicitudNoSatisfecha solicitud = obtener(usuario, id);
        SolicitudNoSatisfecha.validarMotivo(motivo);
        if (solicitud.getEstado() == EstadoSolicitud.ANULADA) {
            return solicitud;
        }
        solicitud.anular(usuario.id(), Instant.now(reloj), motivo);
        return repositorio.guardar(solicitud);
    }
}
