package com.codefactory.supplychain.ventas.application.port.in;

import com.codefactory.supplychain.shared.security.UsuarioActual;
import com.codefactory.supplychain.ventas.domain.model.EstadoVenta;
import com.codefactory.supplychain.ventas.domain.model.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

/** HU-42: historial de ventas limitado al alcance de tiendas del usuario. */
public interface ConsultarVentasUseCase {

    record Filtros(UUID tiendaId, UUID varianteId, UUID transaccionId, EstadoVenta estado,
                   Instant desde, Instant hasta) {
    }

    Page<Venta> consultar(UsuarioActual usuario, Filtros filtros, Pageable pageable);

    Venta obtener(UsuarioActual usuario, UUID ventaId);
}
