package com.codefactory.supplychain.reposicion.domain.model;

import com.codefactory.supplychain.reposicion.domain.exception.OrdenInvalidaException;
import com.codefactory.supplychain.reposicion.domain.exception.TransicionOrdenInvalidaException;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento.Liberacion;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-53: cerrar con faltante y cancelar. */
class CierreOrdenTest {

    private static final Instant AHORA = Instant.parse("2026-11-21T15:00:00Z");
    private final UUID usuario = UUID.randomUUID();

    private static OrdenReabastecimiento orden(EstadoOrden estado, UUID cd, int s, int pendiente, int despachada) {
        return OrdenReabastecimiento.reconstruir(UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(),
                cd, null, s, pendiente, despachada, estado, AHORA, 0L, List.of(), null);
    }

    @Test
    void cerrarConFaltanteLiberaLaReservaVivaYElPendiente() {
        // S = 10, despachado 4, recibido 4: quedan 6 de reserva y 6 pendientes.
        OrdenReabastecimiento orden = orden(EstadoOrden.REABIERTA, UUID.randomUUID(), 10, 6, 4);

        Liberacion liberacion = orden.cerrarConFaltante(" Sin reposición del proveedor ", usuario, AHORA);

        assertThat(liberacion).isEqualTo(new Liberacion(6, 6));
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.CERRADA);
        assertThat(orden.getCantidadPendiente()).isEqualTo(6);
        assertThat(orden.getMotivoCierre()).isEqualTo("Sin reposición del proveedor");
        assertThat(orden.getCerradaPor()).isEqualTo(usuario);
        assertThat(orden.getFechaCierre()).isEqualTo(AHORA);
    }

    @Test
    void trasDespacharTodoSoloQuedaPorLiberarElEntrante() {
        // S = 10, despachado 10 (reserva consumida), recibido 7 por pérdida en tránsito.
        Liberacion liberacion = orden(EstadoOrden.REABIERTA, UUID.randomUUID(), 10, 3, 10)
                .cerrarConFaltante("Pérdida en tránsito asumida", usuario, AHORA);

        assertThat(liberacion).isEqualTo(new Liberacion(0, 3));
    }

    @Test
    void cancelarUnaGeneradaNoLiberaNada() {
        OrdenReabastecimiento orden = orden(EstadoOrden.GENERADA, null, 10, 10, 0);

        orden.cancelar("Ningún CD tiene la referencia", usuario, AHORA);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.CANCELADA);
        assertThat(orden.getCdAsignadoId()).isNull();
        assertThat(orden.getMotivoCierre()).isEqualTo("Ningún CD tiene la referencia");
    }

    @Test
    void soloDesdeSuEstadoYConMotivo() {
        UUID cd = UUID.randomUUID();
        assertThatThrownBy(() -> orden(EstadoOrden.APROBADA, cd, 10, 10, 0).cerrarConFaltante("m", usuario, AHORA))
                .isInstanceOf(TransicionOrdenInvalidaException.class);
        assertThatThrownBy(() -> orden(EstadoOrden.EN_TRANSITO, cd, 10, 10, 10).cerrarConFaltante("m", usuario, AHORA))
                .isInstanceOf(TransicionOrdenInvalidaException.class);
        assertThatThrownBy(() -> orden(EstadoOrden.APROBADA, cd, 10, 10, 0).cancelar("m", usuario, AHORA))
                .isInstanceOf(TransicionOrdenInvalidaException.class);
        assertThatThrownBy(() -> orden(EstadoOrden.GENERADA, null, 10, 10, 0).cancelar(" ", usuario, AHORA))
                .isInstanceOf(OrdenInvalidaException.class);
        assertThatThrownBy(() -> orden(EstadoOrden.REABIERTA, cd, 10, 6, 4)
                .cerrarConFaltante("x".repeat(501), usuario, AHORA))
                .isInstanceOf(OrdenInvalidaException.class);
    }
}
