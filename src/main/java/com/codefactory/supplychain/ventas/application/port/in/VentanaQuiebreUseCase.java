package com.codefactory.supplychain.ventas.application.port.in;

import com.codefactory.supplychain.ventas.domain.model.VentanaQuiebre;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

/** HU-37: consulta y reconciliación de ventanas de quiebre (scope inventario:administrar). */
public interface VentanaQuiebreUseCase {

    record ConsultaFiltros(UUID tiendaId, UUID varianteId, Boolean abierta, Instant desde, Instant hasta) {
    }

    /** {@code varianteId} nulo reconcilia todas las variantes conocidas de la tienda. */
    record ReconciliarComando(UsuarioActual usuario, UUID tiendaId, UUID varianteId, Instant desde, Instant hasta) {
    }

    record ResultadoReconciliacion(int paresProcesados, int creadas, int cerradas, int corregidas,
                                   int sinCambios, int sinRespaldoEnKardex) {
    }

    Page<VentanaQuiebre> consultar(UsuarioActual usuario, ConsultaFiltros filtros, Pageable pageable);

    /** @throws com.codefactory.supplychain.ventas.domain.exception.VentanaNoEncontradaException si no existe o es de otra tienda */
    VentanaQuiebre obtener(UsuarioActual usuario, UUID id);

    /**
     * Reconstruye los periodos de quiebre desde el kardex real. Idempotente y no destructiva.
     *
     * @throws com.codefactory.supplychain.ventas.domain.exception.IntegracionInventarioPendienteException contrato de Inventario sin implementar
     */
    ResultadoReconciliacion reconciliar(ReconciliarComando comando);
}
