package com.codefactory.supplychain.ventas.application.port.in;

import com.codefactory.supplychain.shared.security.UsuarioActual;
import com.codefactory.supplychain.ventas.domain.model.Venta;

import java.util.List;
import java.util.UUID;

/** HU-32: anulación de una línea o de todas las líneas de una transacción. */
public interface AnularVentaUseCase {

    Venta anular(UsuarioActual usuario, UUID ventaId, String motivo);

    List<Venta> anularTransaccion(UsuarioActual usuario, UUID transaccionId, String motivo);
}
