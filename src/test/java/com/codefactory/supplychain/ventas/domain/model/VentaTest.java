package com.codefactory.supplychain.ventas.domain.model;

import com.codefactory.supplychain.ventas.domain.exception.VentaInvalidaException;
import com.codefactory.supplychain.ventas.domain.exception.VentaYaAnuladaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VentaTest {

    private final UUID transaccionId = UUID.randomUUID();
    private final UUID varianteId = UUID.randomUUID();
    private final UUID tiendaId = UUID.randomUUID();
    private final UUID vendedorId = UUID.randomUUID();
    private final Instant ahora = Instant.parse("2026-10-17T12:00:00Z");

    @Test
    void registrarPorApiAsignaOrigenVendedorYCanalPorDefecto() {
        Venta venta = Venta.registrarPorApi(transaccionId, varianteId, tiendaId, 2,
                new BigDecimal("12.50"), null, vendedorId, false, ahora);

        assertThat(venta.origen()).isEqualTo(OrigenVenta.API);
        assertThat(venta.vendedorId()).isEqualTo(vendedorId);
        assertThat(venta.canal()).isEqualTo("FISICO");
        assertThat(venta.timestampVenta()).isEqualTo(ahora);
        assertThat(venta.estado()).isEqualTo(EstadoVenta.REGISTRADA);
    }

    @Test
    void anularConservaVendedorYRegistraUsuarioYMotivo() {
        Venta venta = venta();
        UUID anuladorId = UUID.randomUUID();

        Venta anulada = venta.anular(anuladorId, ahora, "  Error de captura  ");

        assertThat(anulada.estado()).isEqualTo(EstadoVenta.ANULADA);
        assertThat(anulada.vendedorId()).isEqualTo(vendedorId);
        assertThat(anulada.anuladaPor()).isEqualTo(anuladorId);
        assertThat(anulada.anuladaEn()).isEqualTo(ahora);
        assertThat(anulada.motivoAnulacion()).isEqualTo("Error de captura");
    }

    @Test
    void anularExigeMotivoYNoPermiteAnularDosVeces() {
        Venta venta = venta();
        Venta anulada = venta.anular(UUID.randomUUID(), ahora, "Motivo");

        assertThatThrownBy(() -> venta.anular(UUID.randomUUID(), ahora, " "))
                .isInstanceOf(VentaInvalidaException.class);
        assertThatThrownBy(() -> anulada.anular(UUID.randomUUID(), ahora, "Motivo"))
                .isInstanceOf(VentaYaAnuladaException.class);
    }

    private Venta venta() {
        return Venta.registrarPorApi(transaccionId, varianteId, tiendaId, 2, new BigDecimal("12.50"),
                "FISICO", vendedorId, false, ahora);
    }
}
