package com.codefactory.supplychain.catalogo.domain.model;

import com.codefactory.supplychain.catalogo.domain.exception.TemplateInvalidoException;
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
 * propio; el inventario se maneja a nivel de Variante. Objeto de dominio
 * inmutable — igual que {@code Tienda}/{@code Nodo} (HU-23).
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Template {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final String nombre;
    private final String temporada;
    private final String proveedor;
    private final BigDecimal precioBase;
    private final Categoria categoria;

    private Template(UUID id, String nombre, String temporada, String proveedor,
                      BigDecimal precioBase, Categoria categoria) {
        this.id = id;
        this.nombre = validarNombre(nombre);
        this.temporada = temporada;
        this.proveedor = proveedor;
        this.precioBase = validarPrecioBase(precioBase);
        this.categoria = validarCategoria(categoria);
    }

    public static Template crear(String nombre, String temporada, String proveedor,
                                  BigDecimal precioBase, Categoria categoria) {
        return new Template(UUID.randomUUID(), nombre, temporada, proveedor, precioBase, categoria);
    }

    public static Template reconstruir(UUID id, String nombre, String temporada, String proveedor,
                                        BigDecimal precioBase, Categoria categoria) {
        return new Template(id, nombre, temporada, proveedor, precioBase, categoria);
    }

    /**
     * La categoría no se incluye aquí porque no hay un requisito que
     * establezca que un Template pueda reasignarse a otra categoría.
     */
    public Template actualizarInformacion(String nombre, String temporada,
                                           String proveedor, BigDecimal precioBase) {
        return new Template(id, nombre, temporada, proveedor, precioBase, categoria);
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new TemplateInvalidoException("El nombre del template no puede estar vacío");
        }
        if (nombre.length() > 200) {
            throw new TemplateInvalidoException("El nombre del template no puede superar 200 caracteres");
        }
        return nombre;
    }

    private static BigDecimal validarPrecioBase(BigDecimal precioBase) {
        if (precioBase != null && precioBase.compareTo(BigDecimal.ZERO) < 0) {
            throw new TemplateInvalidoException("El precio base no puede ser negativo");
        }
        return precioBase;
    }

    private static Categoria validarCategoria(Categoria categoria) {
        if (categoria == null) {
            throw new TemplateInvalidoException("El template debe pertenecer a una categoría");
        }
        return categoria;
    }
}
