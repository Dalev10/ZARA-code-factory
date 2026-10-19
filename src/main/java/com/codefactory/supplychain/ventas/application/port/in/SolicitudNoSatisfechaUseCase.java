package com.codefactory.supplychain.ventas.application.port.in;

import com.codefactory.supplychain.ventas.domain.model.EstadoSolicitud;
import com.codefactory.supplychain.ventas.domain.model.SolicitudNoSatisfecha;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

/** Casos de uso HU-34 (registrar) y HU-35 (consultar y anular) de solicitudes no satisfechas. */
public interface SolicitudNoSatisfechaUseCase {

    /** {@code cantidad} nula equivale a 1. El usuario sale del token, nunca del cuerpo. */
    record RegistrarComando(UsuarioActual usuario, UUID tiendaId, UUID varianteId, Integer cantidad,
                            String claveIdempotencia) {
    }

    /** {@code creada = false} indica que fue una repetición idempotente de una solicitud previa. */
    record ResultadoRegistro(SolicitudNoSatisfecha solicitud, boolean creada) {
    }

    record ConsultaFiltros(UUID tiendaId, UUID varianteId, EstadoSolicitud estado, Instant desde, Instant hasta) {
    }

    /**
     * @throws com.codefactory.supplychain.ventas.domain.exception.SolicitudInvalidaException cantidad &lt;= 0 o clave inválida
     * @throws com.codefactory.supplychain.ventas.domain.exception.AccesoTiendaProhibidoException sin acceso a la tienda
     * @throws com.codefactory.supplychain.ventas.domain.exception.VarianteNoEncontradaException variante fuera de catálogo
     * @throws com.codefactory.supplychain.ventas.domain.exception.ClaveIdempotenciaReutilizadaException clave usada con otros datos
     */
    ResultadoRegistro registrar(RegistrarComando comando);

    /** Solo devuelve solicitudes de las tiendas del usuario; una tienda ajena produce una página vacía. */
    Page<SolicitudNoSatisfecha> consultar(UsuarioActual usuario, ConsultaFiltros filtros, Pageable pageable);

    /** @throws com.codefactory.supplychain.ventas.domain.exception.SolicitudNoEncontradaException si no existe o es de otra tienda */
    SolicitudNoSatisfecha obtener(UsuarioActual usuario, UUID id);

    /**
     * @throws com.codefactory.supplychain.ventas.domain.exception.SolicitudNoEncontradaException si no existe o es de otra tienda
     * @throws com.codefactory.supplychain.ventas.domain.exception.SolicitudYaAnuladaException si ya estaba anulada
     */
    SolicitudNoSatisfecha anular(UsuarioActual usuario, UUID id);
}
