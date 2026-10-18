package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.ventas.application.port.in.RegistrarVentaUseCase.Comando;
import com.codefactory.supplychain.ventas.application.port.in.RegistrarVentaUseCase.Item;
import com.codefactory.supplychain.ventas.application.port.in.RegistrarVentaUseCase.ResultadoVenta;
import com.codefactory.supplychain.ventas.application.port.out.DescontarStockPort;
import com.codefactory.supplychain.ventas.application.port.out.TiendaVentaPort;
import com.codefactory.supplychain.ventas.application.port.out.TiendaVentaPort.TiendaVenta;
import com.codefactory.supplychain.ventas.application.port.out.VarianteVentaPort;
import com.codefactory.supplychain.ventas.application.port.out.VentaRepositoryPort;
import com.codefactory.supplychain.ventas.domain.exception.OperacionVentaProhibidaException;
import com.codefactory.supplychain.ventas.domain.exception.SinStockException;
import com.codefactory.supplychain.ventas.domain.exception.TiendaVentaNoEncontradaException;
import com.codefactory.supplychain.ventas.domain.exception.TransaccionVentaReutilizadaException;
import com.codefactory.supplychain.ventas.domain.exception.VarianteVentaNoEncontradaException;
import com.codefactory.supplychain.ventas.domain.exception.VentaInvalidaException;
import com.codefactory.supplychain.ventas.domain.model.Venta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Lógica transaccional de HU-31. Todo el lote ocurre en una transacción (D-36): si una línea
 * falla, no queda ninguna venta ni descuento. El reintento optimista vive en
 * {@link VentaRetryableService}, fuera de esta transacción.
 */
@Service
@RequiredArgsConstructor
public class VentaService {

    public static final String SCOPE_FORZAR_SIN_STOCK = "ventas:forzar-sin-stock";

    private final VentaRepositoryPort ventaRepository;
    private final TiendaVentaPort tiendaPort;
    private final VarianteVentaPort variantePort;
    private final DescontarStockPort descontarStockPort;
    private final Clock reloj;

    @Transactional
    public ResultadoVenta registrar(Comando comando) {
        validarPermisos(comando);
        validarComando(comando);

        ventaRepository.bloquearTransaccion(comando.transaccionId());
        Map<UUID, String> skuPorVariante = new HashMap<>();
        List<Venta> ventas = construirLineas(comando, skuPorVariante, Instant.now(reloj));
        List<Venta> existentes = ventaRepository.buscarPorTransaccion(comando.transaccionId());
        if (!existentes.isEmpty()) {
            return repetirSiCoincide(comando, ventas, existentes);
        }

        TiendaVenta tienda = tiendaPort.buscar(comando.tiendaId())
                .orElseThrow(TiendaVentaNoEncontradaException::new);
        if (!tienda.activa()) {
            throw new VentaInvalidaException("La tienda está inactiva y no puede registrar ventas");
        }

        for (Venta venta : ventas) {
            if (!descontarStockPort.descontar(tienda.almacenId(), venta.varianteId(), venta.cantidad(),
                    venta.id(), comando.forzarSinStock())) {
                throw new SinStockException(skuPorVariante.get(venta.varianteId()));
            }
        }
        return new ResultadoVenta(true, ventaRepository.guardarTodas(ventas));
    }

    private void validarPermisos(Comando c) {
        if (c == null || c.actor() == null || c.transaccionId() == null || c.tiendaId() == null) {
            throw new VentaInvalidaException("Usuario, transacción y tienda son obligatorios");
        }
        if (c.forzarSinStock() && !c.actor().tieneScope(SCOPE_FORZAR_SIN_STOCK)) {
            throw new OperacionVentaProhibidaException("No tiene permiso para forzar ventas sin stock");
        }
        if (!c.actor().puedeOperarEn(c.tiendaId())) {
            throw new OperacionVentaProhibidaException("No tiene acceso a esta tienda");
        }
    }

    private void validarComando(Comando c) {
        if (c.items() == null || c.items().isEmpty()) {
            throw new VentaInvalidaException("La venta debe tener al menos un ítem");
        }
        if (c.canal() != null && (c.canal().isBlank() || c.canal().trim().length() > 30)) {
            throw new VentaInvalidaException("El canal debe contener entre 1 y 30 caracteres");
        }
        for (Item item : c.items()) {
            if (item == null || item.sku() == null || item.sku().isBlank()
                    || item.cantidad() <= 0 || item.precioUnitario() == null
                    || item.precioUnitario().signum() < 0 || item.precioUnitario().scale() > 2) {
                throw new VentaInvalidaException("Cada ítem requiere SKU, cantidad positiva y precio válido");
            }
        }
    }

    private ResultadoVenta repetirSiCoincide(Comando comando, List<Venta> solicitadas, List<Venta> existentes) {
        Map<UUID, Venta> porVariante = new HashMap<>();
        existentes.forEach(v -> porVariante.put(v.varianteId(), v));
        boolean coincide = solicitadas.size() == existentes.size()
                && solicitadas.stream().allMatch(solicitada -> {
                    Venta existente = porVariante.get(solicitada.varianteId());
                    return existente != null && existente.coincideCon(comando.transaccionId(), comando.tiendaId(),
                            solicitada.varianteId(), solicitada.cantidad(), solicitada.precio(),
                            solicitada.canal(), comando.actor().id(), comando.forzarSinStock());
                });
        if (!coincide) {
            throw new TransaccionVentaReutilizadaException(comando.transaccionId());
        }
        return new ResultadoVenta(false, existentes);
    }

    /** Resuelve SKUs y ordena por variante: orden de bloqueo estable entre ventas concurrentes. */
    private List<Venta> construirLineas(Comando c, Map<UUID, String> skuPorVariante, Instant timestamp) {
        Set<String> vistos = new HashSet<>();
        List<Venta> ventas = new ArrayList<>();
        for (Item item : c.items()) {
            if (!vistos.add(item.sku())) {
                throw new VentaInvalidaException("El SKU " + item.sku() + " está repetido en la transacción");
            }
            UUID varianteId = variantePort.buscarIdPorSku(item.sku())
                    .orElseThrow(() -> new VarianteVentaNoEncontradaException(item.sku()));
            skuPorVariante.put(varianteId, item.sku());
            ventas.add(Venta.registrarPorApi(c.transaccionId(), varianteId, c.tiendaId(), item.cantidad(),
                    item.precioUnitario(), c.canal(), c.actor().id(), c.forzarSinStock(), timestamp));
        }
        ventas.sort(Comparator.comparing(Venta::varianteId));
        return ventas;
    }
}
