package com.codefactory.supplychain.catalogo.domain.model;

import java.math.BigDecimal;

/**
 * Representa un Template: la agregación conceptual que describe un
 * producto a nivel de categoría, temporada, proveedor y precio base.
 * <p>
 * Un Template pertenece a una {@link Categoria} y agrupa una o varias
 * {@link Variante} (SKU). El Template en sí mismo no tiene inventario
 * propio; el inventario se maneja a nivel de Variante.
 */
public class Template {

    private final Long id;
    private String nombre;
    private String temporada;
    private String proveedor;
    private BigDecimal precioBase;
    private final Categoria categoria;

    /**
     * Constructor para crear un nuevo Template que todavía no ha sido
     * persistido (por lo tanto no tiene id asignado).
     */
    public Template(String nombre, String temporada, String proveedor,
                     BigDecimal precioBase, Categoria categoria) {
        this(null, nombre, temporada, proveedor, precioBase, categoria);
    }

    /**
     * Constructor para reconstituir un Template ya existente
     * (por ejemplo, a partir de un adaptador de persistencia).
     */
    public Template(Long id, String nombre, String temporada, String proveedor,
                     BigDecimal precioBase, Categoria categoria) {
        this.id = id;
        this.nombre = nombre;
        this.temporada = temporada;
        this.proveedor = proveedor;
        this.precioBase = precioBase;
        this.categoria = categoria;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTemporada() {
        return temporada;
    }

    public String getProveedor() {
        return proveedor;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    /**
     * Permite modificar la información editable del Template.
     * La categoría no se incluye aquí porque no hay un requisito que
     * establezca que un Template pueda reasignarse a otra categoría
     * (ver MODEL_DOMAIN_NOTES.md).
     */
    public void actualizarInformacion(String nombre, String temporada,
                                       String proveedor, BigDecimal precioBase) {
        this.nombre = nombre;
        this.temporada = temporada;
        this.proveedor = proveedor;
        this.precioBase = precioBase;
    }
}
