package com.codefactory.supplychain.catalogo.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Representa una Variante (SKU): la unidad operativa real del catálogo.
 * <p>
 * El inventario, las ventas, las recomendaciones y las órdenes trabajan
 * a nivel de Variante. Cada Variante pertenece a un {@link Template}.
 * <p>
 * {@code talla} y {@code color} son atributos opcionales (pueden ser
 * {@code null}): no toda Variante los define.
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Variante {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final String sku;
    private final Template template;
    private final String talla;
    private final String color;

    /**
     * Crea una nueva Variante (id asignado en dominio, todavía sin persistir).
     */
    public Variante(String sku, Template template, String talla, String color) {
        this(UUID.randomUUID(), sku, template, talla, color);
    }

    /**
     * Reconstituye una Variante ya existente (por ejemplo, a partir de un
     * adaptador de persistencia).
     */
    public Variante(UUID id, String sku, Template template, String talla, String color) {
        this.id = id;
        this.sku = sku;
        this.template = template;
        this.talla = talla;
        this.color = color;
    }

    // No se exponen métodos de modificación todavía: no hay un requisito
    // funcional en esta tarea que indique qué atributos de la Variante
    // pueden modificarse.
}
