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

import java.math.BigDecimal;

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
public class TemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

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

    protected TemplateEntity() {
        // Requerido por JPA.
    }

    public TemplateEntity(Long id, CategoriaEntity categoria, String nombre,
                           String temporada, String proveedor, BigDecimal precioBase) {
        this.id = id;
        this.categoria = categoria;
        this.nombre = nombre;
        this.temporada = temporada;
        this.proveedor = proveedor;
        this.precioBase = precioBase;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CategoriaEntity getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaEntity categoria) {
        this.categoria = categoria;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTemporada() {
        return temporada;
    }

    public void setTemporada(String temporada) {
        this.temporada = temporada;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public void setPrecioBase(BigDecimal precioBase) {
        this.precioBase = precioBase;
    }
}
