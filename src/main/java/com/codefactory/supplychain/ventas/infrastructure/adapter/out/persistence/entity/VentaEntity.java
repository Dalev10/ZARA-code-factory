package com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.entity;

import com.codefactory.supplychain.ventas.domain.model.EstadoVenta;
import com.codefactory.supplychain.ventas.domain.model.OrigenVenta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "venta")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaEntity {

    @Id
    private UUID id;

    @Column(name = "transaccion_id")
    private UUID transaccionId;

    @Column(name = "variante_id", nullable = false)
    private UUID varianteId;

    @Column(name = "tienda_id", nullable = false)
    private UUID tiendaId;

    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, length = 30)
    private String canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenVenta origen;

    @Column(name = "vendedor_id")
    private UUID vendedorId;

    @Column(nullable = false)
    private boolean forzada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVenta estado;

    @Column(name = "timestamp_venta", nullable = false)
    private Instant timestampVenta;

    @Column(name = "anulada_por")
    private UUID anuladaPor;

    @Column(name = "anulada_en")
    private Instant anuladaEn;

    @Column(name = "motivo_anulacion", length = 500)
    private String motivoAnulacion;
}
