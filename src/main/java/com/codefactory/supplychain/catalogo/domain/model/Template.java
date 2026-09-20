package com.codefactory.supplychain.catalogo.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representa un Template: la agregación conceptual que describe un
 * producto a nivel de categoría, temporada, proveedor y precio base.
 * <p>
 * Un Template pertenece a una {@link Categoria} y agrupa una o varias
 * {@link Variante} (SKU). El Template en sí mismo no tiene inventario
 * propio; el inventario se maneja a nivel de Variante.
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Template {

    @EqualsAndHashCode.Include
    private final UUID id;
    private String nombre;
    private String temporada;
    private String proveedor;
    private BigDecimal precioBase;
    private final Categoria categoria;

    /**
     * Crea un nuevo Template (id asignado en dominio, todavía sin persistir).
     */
    public Template(String nombre, String temporada, String proveedor,
                     BigDecimal precioBase, Categoria categoria) {
        this(UUID.randomUUID(), nombre, temporada, proveedor, precioBase, categoria);
    }

    /**
     * Reconstituye un Template ya existente (por ejemplo, a partir de un
     * adaptador de persistencia).
     */
    public Template(UUID id, String nombre, String temporada, String proveedor,
                     BigDecimal precioBase, Categoria categoria) {
        this.id = id;
        this.nombre = nombre;
        this.temporada = temporada;
        this.proveedor = proveedor;
        this.precioBase = precioBase;
        this.categoria = categoria;
    }

    /**
     * Permite modificar la información editable del Template.
     * La categoría no se incluye aquí porque no hay un requisito que
     * establezca que un Template pueda reasignarse a otra categoría.
     */
    public void actualizarInformacion(String nombre, String temporada,
                                       String proveedor, BigDecimal precioBase) {
        this.nombre = nombre;
        this.temporada = temporada;
        this.proveedor = proveedor;
        this.precioBase = precioBase;
    }
}
