package com.codefactory.supplychain.catalogo.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Representa una categoría de productos dentro del catálogo.
 * <p>
 * Una Categoria agrupa uno o varios {@link Template}. No tiene
 * responsabilidades de persistencia ni de infraestructura: es un
 * objeto de dominio puro.
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Categoria {

    @EqualsAndHashCode.Include
    private final UUID id;
    private String nombre;

    /**
     * Crea una nueva Categoria (id asignado en dominio, todavía sin persistir).
     */
    public Categoria(String nombre) {
        this(UUID.randomUUID(), nombre);
    }

    /**
     * Reconstituye una Categoria ya existente (por ejemplo, desde un
     * adaptador de persistencia).
     */
    public Categoria(UUID id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    /**
     * Permite modificar el nombre de la categoría.
     */
    public void cambiarNombre(String nuevoNombre) {
        this.nombre = nuevoNombre;
    }
}
