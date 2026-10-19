package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity;

import com.codefactory.supplychain.reposicion.domain.model.OrigenTraspaso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code traspaso_interno}.
 * <p>
 * Las referencias a usuario son UUID simples: identity es otro módulo.
 * La integridad la garantiza la FK de la base de datos.
 *
 * Módulo: reposicion — Reposición interna.
 */
@Entity
@Table(name = "traspaso_interno")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TraspasoInternoEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "variante_id", nullable = false)
    private UUID varianteId;

    @Column(name = "tienda_id", nullable = false)
    private UUID tiendaId;

    @Column(nullable = false)
    private int cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrigenTraspaso origen;

    @Column(name = "registrado_por")
    private UUID registradoPor;

    @Column(name = "timestamp_traspaso", nullable = false)
    private Instant timestampTraspaso;
}
