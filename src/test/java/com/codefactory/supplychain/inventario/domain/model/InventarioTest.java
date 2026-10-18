package com.codefactory.supplychain.inventario.domain.model;

import com.codefactory.supplychain.inventario.domain.exception.InventarioInvalidoException;
import com.codefactory.supplychain.inventario.domain.exception.StockInsuficienteException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventarioTest {

    private final UUID nodoId = UUID.randomUUID();
    private final UUID varianteId = UUID.randomUUID();

    @Test
    void crearInicializaLasCincoMagnitudesEnCero() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);

        assertThat(inventario.getALaMano()).isZero();
        assertThat(inventario.getEntrante()).isZero();
        assertThat(inventario.getSaliente()).isZero();
        assertThat(inventario.getDisponible()).isZero();
        assertThat(inventario.getPronosticado()).isZero();
    }

    @Test
    void cargarAumentaALaManoYRecalculaDerivadas() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);

        MovimientoInventario movimiento = inventario.cargar(10);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.CARGA_INICIAL);
        assertThat(movimiento.cantidad()).isEqualTo(10);
        assertThat(inventario.getALaMano()).isEqualTo(10);
        assertThat(inventario.getDisponible()).isEqualTo(10);
        assertThat(inventario.getPronosticado()).isEqualTo(10);
    }

    @Test
    void cargarEnCeroEsValidaYDejaLaVarianteCargada() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);

        MovimientoInventario movimiento = inventario.cargar(0);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.CARGA_INICIAL);
        assertThat(movimiento.cantidad()).isZero();
        verificarInvariantes(inventario, movimiento);
    }

    @Test
    void ventaNormalDescuentaStock() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(10);

        MovimientoInventario movimiento = inventario.aplicarVenta(4, false);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.VENTA);
        assertThat(movimiento.cantidad()).isEqualTo(-4);
        assertThat(movimiento.forzado()).isFalse();
        assertThat(inventario.getALaMano()).isEqualTo(6);
        assertThat(inventario.getDisponible()).isEqualTo(6);
        assertThat(inventario.getPronosticado()).isEqualTo(6);
    }

    @Test
    void ventaNormalConStockInsuficienteEsRechazadaSinMutarEstado() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(3);

        assertThatThrownBy(() -> inventario.aplicarVenta(4, false))
                .isInstanceOf(StockInsuficienteException.class);
        assertThat(inventario.getALaMano()).isEqualTo(3);
    }

    @Test
    void ventaForzadaPermiteStockNegativo() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);

        MovimientoInventario movimiento = inventario.aplicarVenta(4, true);

        assertThat(movimiento.cantidad()).isEqualTo(-4);
        assertThat(movimiento.forzado()).isTrue();
        assertThat(movimiento.saldoResultante()).isEqualTo(-4);
        assertThat(inventario.getALaMano()).isEqualTo(-4);
        assertThat(inventario.getDisponible()).isEqualTo(-4);
        assertThat(inventario.getPronosticado()).isEqualTo(-4);
    }

    @Test
    void ajustePositivoYNegativoMantieneDerivadasCoherentes() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(10);

        inventario.aplicarAjuste(-3, "Conteo físico");

        assertThat(inventario.getALaMano()).isEqualTo(7);
        assertThat(inventario.getDisponible()).isEqualTo(7);
        assertThat(inventario.getPronosticado()).isEqualTo(7);
    }

    @Test
    void ajusteQueDejaStockNegativoEsRechazado() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);

        assertThatThrownBy(() -> inventario.aplicarAjuste(-1, "Conteo físico"))
                .isInstanceOf(InventarioInvalidoException.class);
    }

    @Test
    void revertirRegistraAnulacionYReponeStock() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(2);
        inventario.aplicarVenta(2, false);

        MovimientoInventario movimiento = inventario.revertir(2, "Venta anulada");

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.ANULACION_VENTA);
        assertThat(inventario.getALaMano()).isEqualTo(2);
    }

    @Test
    void reponerRegistraReposicion() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.aplicarVenta(2, true);

        MovimientoInventario movimiento = inventario.reponer(5);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.REPOSICION);
        assertThat(inventario.getALaMano()).isEqualTo(3);
    }

    @Test
    void ajustePositivoAumentaStockYRegistraMotivo() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(2);

        MovimientoInventario movimiento = inventario.aplicarAjuste(3, "INGRESO_MANUAL");

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.AJUSTE);
        assertThat(movimiento.cantidad()).isEqualTo(3);
        assertThat(movimiento.motivo()).isEqualTo("INGRESO_MANUAL");
        assertThat(movimiento.forzado()).isFalse();
        assertThat(inventario.getALaMano()).isEqualTo(5);
    }

    @Test
    void ajusteEnCeroEsRechazado() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(2);

        assertThatThrownBy(() -> inventario.aplicarAjuste(0, "CORRECCION"))
                .isInstanceOf(InventarioInvalidoException.class);
        assertThat(inventario.getALaMano()).isEqualTo(2);
    }

    @Test
    void ventaNormalPuedeDejarElStockExactamenteEnCero() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(3);

        MovimientoInventario movimiento = inventario.aplicarVenta(3, false);

        assertThat(movimiento.saldoResultante()).isZero();
        assertThat(inventario.getALaMano()).isZero();
    }

    @Test
    void cadaTransicionMantieneLasInvariantesYElSaldoDelMovimiento() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);

        verificarInvariantes(inventario, inventario.cargar(10));
        verificarInvariantes(inventario, inventario.reponer(2));
        verificarInvariantes(inventario, inventario.aplicarVenta(4, false));
        verificarInvariantes(inventario, inventario.aplicarAjuste(3, "INGRESO_MANUAL"));
        verificarInvariantes(inventario, inventario.aplicarAjuste(-1, "MERMA"));
        verificarInvariantes(inventario, inventario.revertir(4, "Venta anulada"));
        verificarInvariantes(inventario, inventario.aplicarVenta(20, true));

        assertThat(inventario.getALaMano()).isEqualTo(-6);
    }

    @Test
    void reconstruirRecalculaDerivadasYConservaLaVersion() {
        UUID id = UUID.randomUUID();

        Inventario inventario = Inventario.reconstruir(id, nodoId, varianteId, 7, 0, 0, 3);

        assertThat(inventario.getId()).isEqualTo(id);
        assertThat(inventario.getVersion()).isEqualTo(3);
        assertThat(inventario.getDisponible()).isEqualTo(7);
        assertThat(inventario.getPronosticado()).isEqualTo(7);
    }

    @Test
    void reconstruirRechazaDatosIncompletosOMagnitudesNegativas() {
        assertThatThrownBy(() -> Inventario.reconstruir(null, nodoId, varianteId, 0, 0, 0))
                .isInstanceOf(InventarioInvalidoException.class);
        assertThatThrownBy(() -> Inventario.reconstruir(UUID.randomUUID(), nodoId, varianteId, 0, -1, 0))
                .isInstanceOf(InventarioInvalidoException.class);
        assertThatThrownBy(() -> Inventario.reconstruir(UUID.randomUUID(), nodoId, varianteId, 0, 0, -1))
                .isInstanceOf(InventarioInvalidoException.class);
    }

    @Test
    void soloUnaVentaPuedeRegistrarseComoForzada() {
        assertThatThrownBy(() -> new MovimientoInventario(
                UUID.randomUUID(),
                TipoMovimientoInventario.AJUSTE,
                1,
                "Conteo físico",
                true,
                1,
                java.time.Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaCantidadesNoPositivas() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);

        assertThatThrownBy(() -> inventario.cargar(-1))
                .isInstanceOf(InventarioInvalidoException.class);
        assertThatThrownBy(() -> inventario.aplicarVenta(-1, false))
                .isInstanceOf(InventarioInvalidoException.class);
        assertThatThrownBy(() -> inventario.revertir(0, "Anulación"))
                .isInstanceOf(InventarioInvalidoException.class);
        assertThatThrownBy(() -> inventario.reponer(0))
                .isInstanceOf(InventarioInvalidoException.class);
    }

    private static void verificarInvariantes(Inventario inventario, MovimientoInventario movimiento) {
        assertThat(inventario.getDisponible()).isEqualTo(inventario.getALaMano());
        assertThat(inventario.getPronosticado()).isEqualTo(
                inventario.getALaMano() + inventario.getEntrante() - inventario.getSaliente());
        assertThat(inventario.getEntrante()).isZero();
        assertThat(inventario.getSaliente()).isZero();
        assertThat(movimiento.saldoResultante()).isEqualTo(inventario.getALaMano());
    }
}
