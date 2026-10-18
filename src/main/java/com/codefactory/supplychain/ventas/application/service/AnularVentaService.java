package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.shared.security.UsuarioActual;
import com.codefactory.supplychain.ventas.application.port.out.ReintegrarStockPort;
import com.codefactory.supplychain.ventas.application.port.out.TiendaVentaPort;
import com.codefactory.supplychain.ventas.application.port.out.TiendaVentaPort.TiendaVenta;
import com.codefactory.supplychain.ventas.application.port.out.VentaRepositoryPort;
import com.codefactory.supplychain.ventas.domain.exception.TiendaSinAlmacenException;
import com.codefactory.supplychain.ventas.domain.exception.VentaInvalidaException;
import com.codefactory.supplychain.ventas.domain.exception.VentaNoEncontradaException;
import com.codefactory.supplychain.ventas.domain.model.EstadoVenta;
import com.codefactory.supplychain.ventas.domain.model.Venta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** HU-32: anulación transaccional, con restitución al Almacén y auditoría del anulador. */
@Service
@RequiredArgsConstructor
public class AnularVentaService {

    private final VentaRepositoryPort ventas;
    private final TiendaVentaPort tiendas;
    private final ReintegrarStockPort reintegrarStock;
    private final Clock reloj;

    @Transactional
    public Venta anular(UsuarioActual usuario, UUID ventaId, String motivo) {
        Venta original = ventas.buscarPorIdParaActualizar(ventaId)
                .orElseThrow(() -> new VentaNoEncontradaException(ventaId));
        validarAcceso(usuario, original);
        validarMotivo(motivo);
        if (original.estado() == EstadoVenta.ANULADA) {
            return original;
        }
        Venta anulada = original.anular(usuario.id(), Instant.now(reloj), motivo);
        UUID almacenId = almacenDeTienda(original.tiendaId());
        reintegrarStock.reintegrar(almacenId, original.varianteId(), original.cantidad(), original.id());
        return ventas.guardar(anulada);
    }

    @Transactional
    public List<Venta> anularTransaccion(UsuarioActual usuario, UUID transaccionId, String motivo) {
        validarMotivo(motivo);
        List<Venta> originales = ventas.buscarPorTransaccionParaActualizar(transaccionId);
        if (originales.isEmpty()) {
            throw new VentaNoEncontradaException(transaccionId);
        }
        UUID tiendaId = originales.get(0).tiendaId();
        Instant fechaAnulacion = Instant.now(reloj);
        List<Venta> porAnular = new ArrayList<>();
        for (Venta venta : originales) {
            validarAcceso(usuario, venta);
            if (!tiendaId.equals(venta.tiendaId())) {
                throw new VentaNoEncontradaException(transaccionId);
            }
            if (venta.estado() != EstadoVenta.ANULADA) {
                porAnular.add(venta.anular(usuario.id(), fechaAnulacion, motivo));
            }
        }
        if (porAnular.isEmpty()) {
            return originales;
        }
        UUID almacenId = almacenDeTienda(tiendaId);
        for (Venta venta : porAnular) {
            reintegrarStock.reintegrar(almacenId, venta.varianteId(), venta.cantidad(), venta.id());
        }
        List<Venta> guardadas = ventas.guardarTodas(porAnular);
        Map<UUID, Venta> porId = new HashMap<>();
        guardadas.forEach(venta -> porId.put(venta.id(), venta));
        return originales.stream().map(venta -> porId.getOrDefault(venta.id(), venta)).toList();
    }

    private static void validarAcceso(UsuarioActual usuario, Venta venta) {
        if (usuario == null || !usuario.puedeOperarEn(venta.tiendaId())) {
            throw new VentaNoEncontradaException(venta.id());
        }
    }

    private static void validarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank() || motivo.trim().length() > 500) {
            throw new VentaInvalidaException("El motivo de anulación es obligatorio y admite hasta 500 caracteres");
        }
    }

    private UUID almacenDeTienda(UUID tiendaId) {
        return tiendas.buscar(tiendaId).map(TiendaVenta::almacenId)
                .orElseThrow(() -> new TiendaSinAlmacenException(tiendaId));
    }
}
