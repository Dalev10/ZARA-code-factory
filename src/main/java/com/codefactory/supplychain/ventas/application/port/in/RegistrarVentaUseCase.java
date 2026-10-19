package com.codefactory.supplychain.ventas.application.port.in;

import com.codefactory.supplychain.ventas.domain.model.Venta;
import com.codefactory.supplychain.shared.security.UsuarioActual;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** HU-40: registrar una venta por lote (una transacción, varias líneas). */
public interface RegistrarVentaUseCase {

    ResultadoVenta registrar(Comando comando);

    /** El usuario viaja en el comando (D-08); el vendedor es siempre el del token (D-10). */
    record Comando(UsuarioActual actor, UUID transaccionId, UUID tiendaId, String canal,
                   boolean forzarSinStock, List<Item> items) {
    }

    record Item(String sku, int cantidad, BigDecimal precioUnitario) {
    }

    /** {@code creada = false} cuando el reintento devolvió la venta original (D-69). */
    record ResultadoVenta(boolean creada, List<Venta> lineas) {
    }
}
