package com.codefactory.supplychain.inventario.domain.model;

import com.codefactory.supplychain.inventario.domain.exception.AjusteDejaStockNegativoException;
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

        MovimientoInventario movimiento = inventario.cargar(10, null);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.CARGA_INICIAL);
        assertThat(movimiento.cantidad()).isEqualTo(10);
        assertThat(inventario.getALaMano()).isEqualTo(10);
        assertThat(inventario.getDisponible()).isEqualTo(10);
        assertThat(inventario.getPronosticado()).isEqualTo(10);
    }

    @Test
    void cargarRegistraAlAutorEnElMovimiento() {
        UUID usuarioId = UUID.randomUUID();

        MovimientoInventario movimiento = Inventario.crear(nodoId, varianteId).cargar(5, usuarioId);

        assertThat(movimiento.usuarioId()).isEqualTo(usuarioId);
    }

    @Test
    void cargarEnCeroEsValidaYDejaLaVarianteCargada() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);

        MovimientoInventario movimiento = inventario.cargar(0, null);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.CARGA_INICIAL);
        assertThat(movimiento.cantidad()).isZero();
        verificarInvariantes(inventario, movimiento);
    }

    @Test
    void ventaNormalDescuentaStock() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(10, null);

        MovimientoInventario movimiento = inventario.aplicarVenta(4, false, UUID.randomUUID());

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
        inventario.cargar(3, null);

        assertThatThrownBy(() -> inventario.aplicarVenta(4, false, UUID.randomUUID()))
                .isInstanceOf(StockInsuficienteException.class);
        assertThat(inventario.getALaMano()).isEqualTo(3);
    }

    @Test
    void ventaForzadaPermiteStockNegativo() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);

        MovimientoInventario movimiento = inventario.aplicarVenta(4, true, UUID.randomUUID());

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
        inventario.cargar(10, null);

        inventario.aplicarAjuste(-3, MotivoAjuste.CONTEO_FISICO, null, null);

        assertThat(inventario.getALaMano()).isEqualTo(7);
        assertThat(inventario.getDisponible()).isEqualTo(7);
        assertThat(inventario.getPronosticado()).isEqualTo(7);
    }

    @Test
    void ajusteQueDejaStockNegativoEsRechazado() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(1, null);

        assertThatThrownBy(() -> inventario.aplicarAjuste(-2, MotivoAjuste.MERMA, null, null))
                .isInstanceOf(AjusteDejaStockNegativoException.class);
        assertThat(inventario.getALaMano()).isEqualTo(1);
    }

    @Test
    void ajustePuedeDejarElStockExactamenteEnCero() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(2, null);

        MovimientoInventario movimiento = inventario.aplicarAjuste(-2, MotivoAjuste.MERMA, null, null);

        verificarInvariantes(inventario, movimiento);
        assertThat(inventario.getALaMano()).isZero();
    }

    @Test
    void ajustePorCorreccionExigeComentario() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(2, null);

        assertThatThrownBy(() -> inventario.aplicarAjuste(1, MotivoAjuste.CORRECCION, null, null))
                .isInstanceOf(InventarioInvalidoException.class);
        assertThatThrownBy(() -> inventario.aplicarAjuste(1, MotivoAjuste.CORRECCION, "  ", null))
                .isInstanceOf(InventarioInvalidoException.class);
        assertThat(inventario.getALaMano()).isEqualTo(2);
    }

    @Test
    void ajusteSinMotivoEsRechazado() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);

        assertThatThrownBy(() -> inventario.aplicarAjuste(1, null, null, null))
                .isInstanceOf(InventarioInvalidoException.class);
    }

    @Test
    void ajusteRegistraMotivoComentarioYAutor() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(2, null);
        UUID usuarioId = UUID.randomUUID();

        MovimientoInventario movimiento = inventario.aplicarAjuste(
                -1, MotivoAjuste.CORRECCION, "Error de digitación", usuarioId);

        assertThat(movimiento.motivo()).isEqualTo("CORRECCION");
        assertThat(movimiento.comentario()).isEqualTo("Error de digitación");
        assertThat(movimiento.usuarioId()).isEqualTo(usuarioId);
        verificarInvariantes(inventario, movimiento);
    }

    @Test
    void revertirRegistraAnulacionYReponeStock() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(2, null);
        inventario.aplicarVenta(2, false, UUID.randomUUID());

        MovimientoInventario movimiento = inventario.revertir(2, UUID.randomUUID());

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.ANULACION_VENTA);
        assertThat(inventario.getALaMano()).isEqualTo(2);
    }

    @Test
    void recibirTraspasoRegistraEntradaConSuReferencia() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.aplicarVenta(2, true, UUID.randomUUID());
        UUID traspasoId = UUID.randomUUID();

        MovimientoInventario movimiento = inventario.recibirTraspaso(5, traspasoId);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.TRASPASO_ENTRADA);
        assertThat(movimiento.referenciaId()).isEqualTo(traspasoId);
        assertThat(inventario.getALaMano()).isEqualTo(3);
    }

    @Test
    void despacharTraspasoNuncaDejaElOrigenEnNegativo() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(2, null);

        assertThatThrownBy(() -> inventario.despacharTraspaso(3, UUID.randomUUID()))
                .isInstanceOf(StockInsuficienteException.class);
        MovimientoInventario movimiento = inventario.despacharTraspaso(2, UUID.randomUUID());

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.TRASPASO_SALIDA);
        assertThat(movimiento.cantidad()).isEqualTo(-2);
        assertThat(inventario.getALaMano()).isZero();
    }

    @Test
    void ajustePositivoAumentaStockYRegistraMotivo() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(2, null);

        MovimientoInventario movimiento = inventario.aplicarAjuste(3, MotivoAjuste.INGRESO_MANUAL, null, null);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.AJUSTE);
        assertThat(movimiento.cantidad()).isEqualTo(3);
        assertThat(movimiento.motivo()).isEqualTo("INGRESO_MANUAL");
        assertThat(movimiento.forzado()).isFalse();
        assertThat(inventario.getALaMano()).isEqualTo(5);
    }

    @Test
    void ajusteEnCeroEsRechazado() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(2, null);

        assertThatThrownBy(() -> inventario.aplicarAjuste(0, MotivoAjuste.CONTEO_FISICO, null, null))
                .isInstanceOf(InventarioInvalidoException.class);
        assertThat(inventario.getALaMano()).isEqualTo(2);
    }

    @Test
    void ventaNormalPuedeDejarElStockExactamenteEnCero() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(3, null);

        MovimientoInventario movimiento = inventario.aplicarVenta(3, false, UUID.randomUUID());

        assertThat(movimiento.saldoResultante()).isZero();
        assertThat(inventario.getALaMano()).isZero();
    }

    @Test
    void cadaTransicionMantieneLasInvariantesYElSaldoDelMovimiento() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);

        verificarInvariantes(inventario, inventario.cargar(10, null));
        verificarInvariantes(inventario, inventario.recibirTraspaso(2, UUID.randomUUID()));
        verificarInvariantes(inventario, inventario.aplicarVenta(4, false, UUID.randomUUID()));
        verificarInvariantes(inventario, inventario.aplicarAjuste(3, MotivoAjuste.INGRESO_MANUAL, null, null));
        verificarInvariantes(inventario, inventario.aplicarAjuste(-1, MotivoAjuste.MERMA, null, null));
        verificarInvariantes(inventario, inventario.revertir(4, UUID.randomUUID()));
        verificarInvariantes(inventario, inventario.aplicarVenta(20, true, UUID.randomUUID()));

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
                null,
                true,
                null,
                null,
                1,
                java.time.Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaCantidadesNoPositivas() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);

        assertThatThrownBy(() -> inventario.cargar(-1, null))
                .isInstanceOf(InventarioInvalidoException.class);
        assertThatThrownBy(() -> inventario.aplicarVenta(-1, false, UUID.randomUUID()))
                .isInstanceOf(InventarioInvalidoException.class);
        assertThatThrownBy(() -> inventario.revertir(0, UUID.randomUUID()))
                .isInstanceOf(InventarioInvalidoException.class);
        assertThatThrownBy(() -> inventario.recibirTraspaso(0, UUID.randomUUID()))
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
