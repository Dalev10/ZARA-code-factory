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
    void ventaNormalDescuentaStock() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(10);

        MovimientoInventario movimiento = inventario.aplicarVenta(4, false);

        assertThat(movimiento.tipo()).isEqualTo(TipoMovimientoInventario.VENTA);
        assertThat(movimiento.cantidad()).isEqualTo(-4);
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
    void rechazaCantidadesNoPositivas() {
        Inventario inventario = Inventario.crear(nodoId, varianteId);

        assertThatThrownBy(() -> inventario.cargar(0))
                .isInstanceOf(InventarioInvalidoException.class);
        assertThatThrownBy(() -> inventario.aplicarVenta(-1, false))
                .isInstanceOf(InventarioInvalidoException.class);
        assertThatThrownBy(() -> inventario.revertir(0, "Anulación"))
                .isInstanceOf(InventarioInvalidoException.class);
    }
}
