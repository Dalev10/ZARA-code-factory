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

import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code variante}, definida en
 * {@code V1__crear_esquema_inicial.sql}.
 * <p>
 * Es independiente del modelo de dominio {@code Variante}: la conversión
 * entre ambas se realiza en {@code VariantePersistenceMapper}.
 */
@Entity
@Table(name = "variante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VarianteEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    // template_id es NOT NULL en el esquema existente.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private TemplateEntity template;

    @Column(name = "talla", length = 20)
    private String talla;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "sku", nullable = false, unique = true, length = 60)
    private String sku;
}
