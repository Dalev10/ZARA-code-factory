package com.codefactory.supplychain.catalogo.domain.model;

import com.codefactory.supplychain.catalogo.domain.exception.CategoriaInvalidaException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Representa una categoría de productos dentro del catálogo.
 * <p>
 * Una Categoria agrupa uno o varios {@link Template}. No tiene
 * responsabilidades de persistencia ni de infraestructura: es un
 * objeto de dominio puro, inmutable — igual que {@code Tienda}/{@code Nodo}
 * (HU-23).
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Categoria {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final String nombre;

    private Categoria(UUID id, String nombre) {
        this.id = id;
        this.nombre = validarNombre(nombre);
    }

    public static Categoria crear(String nombre) {
        return new Categoria(UUID.randomUUID(), nombre);
    }

    public static Categoria reconstruir(UUID id, String nombre) {
        return new Categoria(id, nombre);
    }

    public Categoria cambiarNombre(String nuevoNombre) {
        return new Categoria(id, nuevoNombre);
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new CategoriaInvalidaException("El nombre de la categoria no puede estar vacío");
        }
        if (nombre.length() > 150) {
            throw new CategoriaInvalidaException("El nombre de la categoria no puede superar 150 caracteres");
        }
        return nombre;
    }
}
