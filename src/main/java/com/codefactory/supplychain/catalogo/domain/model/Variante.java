package com.codefactory.supplychain.catalogo.domain.model;

/**
 * Representa una Variante (SKU): la unidad operativa real del catálogo.
 * <p>
 * El inventario, las ventas, las recomendaciones y las órdenes trabajan
 * a nivel de Variante. Cada Variante pertenece a un {@link Template}.
 * <p>
 * {@code talla} y {@code color} son atributos opcionales (pueden ser
 * {@code null}): no toda Variante los define, y las Variantes existentes
 * previas a la incorporación de estos campos al dominio no los tienen.
 */
public class Variante {

    private final Long id;
    private final String sku;
    private final Template template;
    private final String talla;
    private final String color;

    /**
     * Constructor para crear una nueva Variante que todavía no ha sido
     * persistida (por lo tanto no tiene id asignado).
     */
    public Variante(String sku, Template template, String talla, String color) {
        this(null, sku, template, talla, color);
    }

    /**
     * Constructor para reconstituir una Variante ya existente
     * (por ejemplo, a partir de un adaptador de persistencia).
     */
    public Variante(Long id, String sku, Template template, String talla, String color) {
        this.id = id;
        this.sku = sku;
        this.template = template;
        this.talla = talla;
        this.color = color;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public Template getTemplate() {
        return template;
    }

    public String getTalla() {
        return talla;
    }

    public String getColor() {
        return color;
    }

    // No se exponen métodos de modificación todavía: no hay un requisito
    // funcional en esta tarea que indique qué atributos de la Variante
    // pueden modificarse (ver MODEL_DOMAIN_NOTES.md).
}
