package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity;

import com.codefactory.supplychain.inventario.domain.model.TipoMovimientoInventario;
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

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA del kardex de movimientos de Inventario.
 */
@Entity
@Table(name = "movimiento_inventario")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventarioEntity {

    @Id
    private UUID id;

    @Column(name = "nodo_id", nullable = false)
    private UUID nodoId;

    @Column(name = "variante_id", nullable = false)
    private UUID varianteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoMovimientoInventario tipo;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "motivo", length = 100)
    private String motivo;

    @Column(name = "comentario", length = 500)
    private String comentario;

    @Column(name = "referencia_id")
    private UUID referenciaId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "ocurrido_en", nullable = false)
    private Instant ocurridoEn;

}
