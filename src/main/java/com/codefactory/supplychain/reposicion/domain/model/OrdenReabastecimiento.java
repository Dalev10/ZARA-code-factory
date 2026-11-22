package com.codefactory.supplychain.reposicion.domain.model;

import com.codefactory.supplychain.reposicion.domain.exception.OrdenInvalidaException;
import com.codefactory.supplychain.reposicion.domain.exception.TransicionOrdenInvalidaException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Orden de reabastecimiento CD → Bodega_Tienda.
 * <p>
 * Se crea con el fallback de CD: {@link #iniciar} abre la orden, cada CD evaluado se
 * registra con {@link #registrarSinStock} o {@link #asignar}, y si se agota la lista
 * se cierra con {@link #quedarSinCd}.
 * <ul>
 *   <li>Con CD: queda APROBADA con {@code cd_asignado_id} y el intento ASIGNADO.</li>
 *   <li>Sin CD: queda GENERADA con {@code cd_asignado_id} nulo y todos los intentos.</li>
 *   <li>Un CD sirve solo si cubre toda la cantidad; no se divide la orden.</li>
 * </ul>
 * Al crear: {@code cantidad_pendiente = S} y {@code cantidad_despachada_acumulada = 0}.
 * <p>
 * Transiciones del ciclo (sección 2.3, HD-02): APROBADA o REABIERTA → EN_DESPACHO →
 * DESPACHADA → EN_TRANSITO → CERRADA (pasando por RECIBIDA_TOTAL) o REABIERTA (pasando
 * por RECIBIDA_PARCIAL). No tocan el inventario: lo mueve logística.
 *
 * Módulo: reposicion.
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class OrdenReabastecimiento {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final UUID recomendacionId;
    private final UUID tiendaId;
    private final UUID varianteId;
    private UUID cdAsignadoId;
    private String motivoAsignacion;
    private final int cantidadTotalSolicitada;
    private int cantidadPendiente;
    private int cantidadDespachadaAcumulada;
    private EstadoOrden estado;
    private final Instant fechaCreacion;
    private final Long version;
    private final List<IntentoCd> intentos;
    private String motivoCierre;
    private UUID cerradaPor;
    private Instant fechaCierre;
    /** Solo mientras corre el fallback; una orden reconstruida está cerrada. */
    private transient boolean enFallback;

    private OrdenReabastecimiento(UUID id, UUID recomendacionId, UUID tiendaId, UUID varianteId,
                                  UUID cdAsignadoId, String motivoAsignacion, int cantidadTotalSolicitada,
                                  int cantidadPendiente, int cantidadDespachadaAcumulada, EstadoOrden estado,
                                  Instant fechaCreacion, Long version, List<IntentoCd> intentos,
                                  Cierre cierre) {
        this.id = Objects.requireNonNull(id, "id");
        this.recomendacionId = recomendacionId;
        this.tiendaId = Objects.requireNonNull(tiendaId, "tiendaId");
        this.varianteId = Objects.requireNonNull(varianteId, "varianteId");
        this.cdAsignadoId = cdAsignadoId;
        this.motivoAsignacion = motivoAsignacion;
        this.cantidadTotalSolicitada = cantidadTotalSolicitada;
        this.cantidadPendiente = cantidadPendiente;
        this.cantidadDespachadaAcumulada = cantidadDespachadaAcumulada;
        this.estado = Objects.requireNonNull(estado, "estado");
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion, "fechaCreacion");
        this.version = version;
        this.intentos = new ArrayList<>(intentos);
        if (cierre != null) {
            this.motivoCierre = cierre.motivo();
            this.cerradaPor = cierre.usuarioId();
            this.fechaCierre = cierre.fecha();
        }
        if (cantidadTotalSolicitada <= 0) {
            throw new OrdenInvalidaException("La cantidad solicitada debe ser mayor que cero");
        }
    }

    /** Abre una orden de S unidades para empezar el fallback. Aún sin CD. */
    public static OrdenReabastecimiento iniciar(UUID recomendacionId, UUID tiendaId, UUID varianteId,
                                               int cantidad, Instant ahora) {
        var orden = new OrdenReabastecimiento(UUID.randomUUID(), recomendacionId, tiendaId, varianteId,
                null, null, cantidad, cantidad, 0, EstadoOrden.GENERADA, ahora, null, List.of(), null);
        orden.enFallback = true;
        return orden;
    }

    public static OrdenReabastecimiento reconstruir(UUID id, UUID recomendacionId, UUID tiendaId, UUID varianteId,
                                                    UUID cdAsignadoId, String motivoAsignacion,
                                                    int cantidadTotalSolicitada, int cantidadPendiente,
                                                    int cantidadDespachadaAcumulada, EstadoOrden estado,
                                                    Instant fechaCreacion, Long version, List<IntentoCd> intentos,
                                                    Cierre cierre) {
        return new OrdenReabastecimiento(id, recomendacionId, tiendaId, varianteId, cdAsignadoId, motivoAsignacion,
                cantidadTotalSolicitada, cantidadPendiente, cantidadDespachadaAcumulada, estado, fechaCreacion,
                version, intentos, cierre);
    }

    /** Quién terminó la orden antes de recibirla completa, cuándo y por qué (HU-53). */
    public record Cierre(String motivo, UUID usuarioId, Instant fecha) {
    }

    /** Lo que hay que devolver al inventario al terminar la orden (HU-53). */
    public record Liberacion(int reservaCd, int entranteBodega) {
    }

    /**
     * REABIERTA → CERRADA aceptando el faltante. El pendiente se conserva como registro de lo que
     * no llegó; lo que se libera es la reserva viva del CD, {@code max(S − ΣD, 0)}, y el
     * {@code entrante} pendiente de la Bodega, P (sección 2.4).
     */
    public Liberacion cerrarConFaltante(String motivo, UUID usuarioId, Instant ahora) {
        exigirEstado("cerrar con faltante", EstadoOrden.REABIERTA);
        Liberacion liberacion = new Liberacion(reservaRestante(), cantidadPendiente);
        terminar(EstadoOrden.CERRADA, motivo, usuarioId, ahora);
        return liberacion;
    }

    /** GENERADA (sin CD) → CANCELADA. Sin CD nunca reservó ni sumó entrante: no hay nada que liberar. */
    public void cancelar(String motivo, UUID usuarioId, Instant ahora) {
        exigirEstado("cancelar", EstadoOrden.GENERADA);
        terminar(EstadoOrden.CANCELADA, motivo, usuarioId, ahora);
    }

    private void terminar(EstadoOrden estadoFinal, String motivo, UUID usuarioId, Instant ahora) {
        if (motivo == null || motivo.isBlank()) {
            throw new OrdenInvalidaException("El motivo es obligatorio");
        }
        if (motivo.strip().length() > 500) {
            throw new OrdenInvalidaException("El motivo no puede superar 500 caracteres");
        }
        this.estado = estadoFinal;
        this.motivoCierre = motivo.strip();
        this.cerradaPor = Objects.requireNonNull(usuarioId, "usuarioId");
        this.fechaCierre = ahora;
    }

    /** El CD en la posición {@code ordenEvaluado} no cubre toda la cantidad: se descarta y se sigue. */
    public void registrarSinStock(UUID cdId, int ordenEvaluado, int disponible, Instant ahora) {
        exigirFallbackAbierto();
        intentos.add(IntentoCd.sinStock(cdId, ordenEvaluado, disponible, cantidadTotalSolicitada, ahora));
    }

    /** El CD cubre toda la cantidad y ya se reservó: la orden queda APROBADA con ese CD. */
    public void asignar(UUID cdId, int ordenEvaluado, Instant ahora) {
        exigirFallbackAbierto();
        intentos.add(IntentoCd.asignado(cdId, ordenEvaluado, ahora));
        this.cdAsignadoId = cdId;
        this.motivoAsignacion = "Asignado al CD de prioridad " + ordenEvaluado;
        this.estado = EstadoOrden.APROBADA;
        this.enFallback = false;
    }

    /** Se agotó la lista de CDs: la orden queda GENERADA sin CD. */
    public void quedarSinCd() {
        exigirFallbackAbierto();
        this.motivoAsignacion = intentos.isEmpty()
                ? "La tienda no tiene CDs configurados"
                : "Ningún CD cubre la cantidad solicitada";
        this.enFallback = false;
    }

    public boolean tieneCd() {
        return cdAsignadoId != null;
    }

    /** D-92: {@code max(S − ΣD, 0)}. */
    public int reservaRestante() {
        return Math.max(cantidadTotalSolicitada - cantidadDespachadaAcumulada, 0);
    }

    // -------------------------------------------------------------------------
    // Transiciones del ciclo de abastecimiento (sección 2.3)
    // -------------------------------------------------------------------------

    /** APROBADA o REABIERTA → EN_DESPACHO. El ciclo solicita {@link #getCantidadPendiente()}. */
    public void iniciarDespacho() {
        exigirEstado("iniciar el despacho", EstadoOrden.APROBADA, EstadoOrden.REABIERTA);
        this.estado = EstadoOrden.EN_DESPACHO;
    }

    /**
     * EN_DESPACHO → DESPACHADA; acumula lo despachado (ΣD).
     *
     * @param cantidadDespachada mayor que 0 y no mayor que lo solicitado en el ciclo (el pendiente)
     */
    public void confirmarDespacho(int cantidadDespachada) {
        exigirEstado("confirmar el despacho", EstadoOrden.EN_DESPACHO);
        if (cantidadDespachada <= 0 || cantidadDespachada > cantidadPendiente) {
            throw new OrdenInvalidaException("La cantidad despachada debe estar entre 1 y " + cantidadPendiente
                    + " (lo solicitado en el ciclo)");
        }
        this.cantidadDespachadaAcumulada += cantidadDespachada;
        this.estado = EstadoOrden.DESPACHADA;
    }

    /** DESPACHADA → EN_TRANSITO (salida del viaje). */
    public void marcarEnTransito() {
        exigirEstado("marcar en tránsito", EstadoOrden.DESPACHADA);
        this.estado = EstadoOrden.EN_TRANSITO;
    }

    /**
     * EN_TRANSITO → RECIBIDA_TOTAL → CERRADA si el pendiente llega a 0, o
     * RECIBIDA_PARCIAL → REABIERTA si queda pendiente. Lo no recibido del
     * ciclo es pérdida en tránsito y lo registra logística.
     *
     * @param cantidadRecibida entre 0 y {@link #maximoRecibible()}
     * @return el estado final: CERRADA o REABIERTA
     */
    public EstadoOrden registrarRecepcion(int cantidadRecibida) {
        exigirEstado("registrar la recepción", EstadoOrden.EN_TRANSITO);
        int maximo = maximoRecibible();
        if (cantidadRecibida < 0 || cantidadRecibida > maximo) {
            throw new OrdenInvalidaException("La cantidad recibida debe estar entre 0 y " + maximo);
        }
        this.cantidadPendiente -= cantidadRecibida;
        // RECIBIDA_TOTAL y RECIBIDA_PARCIAL son pasos inmediatos, sin acción entre medio:
        // se persiste directamente el estado final.
        this.estado = cantidadPendiente == 0 ? EstadoOrden.CERRADA : EstadoOrden.REABIERTA;
        return estado;
    }

    /** ΣR = S − P */
    public int cantidadRecibidaAcumulada() {
        return cantidadTotalSolicitada - cantidadPendiente;
    }

    /**
     * Tope de lo que puede recibirse en el ciclo: {@code min(ΣD − ΣR, P)}. ΣD − ΣR incluye
     * la pérdida en tránsito de ciclos anteriores, y P la acota: nunca deja el pendiente
     * en negativo. La orden no guarda lo despachado de cada ciclo; el tope exacto
     * (recibido ≤ despachado del ciclo) lo valida logística con su Ciclo_Despacho.
     */
    public int maximoRecibible() {
        return Math.min(cantidadDespachadaAcumulada - cantidadRecibidaAcumulada(), cantidadPendiente);
    }

    private void exigirEstado(String operacion, EstadoOrden... permitidos) {
        for (EstadoOrden permitido : permitidos) {
            if (estado == permitido) {
                return;
            }
        }
        throw new TransicionOrdenInvalidaException(id, estado, operacion);
    }

    public List<IntentoCd> getIntentos() {
        return Collections.unmodifiableList(intentos);
    }

    private void exigirFallbackAbierto() {
        if (!enFallback) {
            throw new OrdenInvalidaException("El fallback de CD de la orden " + id + " ya terminó");
        }
    }
}
