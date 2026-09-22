package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code template}, definida en
 * {@code V1__crear_esquema_inicial.sql}.
 * <p>
 * Es independiente del modelo de dominio {@code Template}: la
 * conversión entre ambas se realiza en {@code TemplatePersistenceMapper}.
 * <p>
 * No se expone una colección de variantes desde esta entidad (evitando
 * relaciones bidireccionales innecesarias), tal como se pidió para este
 * MVP.
 */
@Entity
@Table(name = "template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TemplateEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    // categoria_id es nullable en el esquema existente (no tiene NOT NULL),
    // por lo que la relación se mapea como opcional.
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "categoria_id")
    private CategoriaEntity categoria;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "temporada", length = 50)
    private String temporada;

    @Column(name = "proveedor", length = 150)
    private String proveedor;

    @Column(name = "precio_base", precision = 12, scale = 2)
    private BigDecimal precioBase;
}
