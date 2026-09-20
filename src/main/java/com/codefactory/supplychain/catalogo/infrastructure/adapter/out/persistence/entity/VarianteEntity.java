package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad JPA que mapea la tabla {@code variante}, definida en
 * {@code V1__crear_esquema_inicial.sql}.
 * <p>
 * Es independiente del modelo de dominio {@code Variante}: la conversión
 * entre ambas se realiza en {@code VariantePersistenceMapper}.
 * <p>
 * IMPORTANTE: la tabla {@code variante} tiene columnas {@code talla} y
 * {@code color} que NO existen en el modelo de dominio actual. Se
 * mapean aquí porque la entidad JPA debe coincidir con el esquema
 * (ddl-auto=validate), pero el mapper de persistencia no las traduce
 * hacia/desde el dominio. Ver PERSISTENCE_NOTES.md para el detalle de
 * cómo se evita perder estos valores al actualizar.
 */
@Entity
@Table(name = "variante")
public class VarianteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

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

    protected VarianteEntity() {
        // Requerido por JPA.
    }

    public VarianteEntity(Long id, TemplateEntity template, String talla,
                           String color, String sku) {
        this.id = id;
        this.template = template;
        this.talla = talla;
        this.color = color;
        this.sku = sku;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TemplateEntity getTemplate() {
        return template;
    }

    public void setTemplate(TemplateEntity template) {
        this.template = template;
    }

    public String getTalla() {
        return talla;
    }

    public void setTalla(String talla) {
        this.talla = talla;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
}
