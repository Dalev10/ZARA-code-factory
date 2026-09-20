package com.codefactory.supplychain.catalogo.domain.model;

/**
 * Representa una categoría de productos dentro del catálogo.
 * <p>
 * Una Categoria agrupa uno o varios {@link Template}. No tiene
 * responsabilidades de persistencia ni de infraestructura: es un
 * objeto de dominio puro.
 */
public class Categoria {

    private final Long id;
    private String nombre;

    /**
     * Constructor para crear una nueva Categoria que todavía no ha sido
     * persistida (por lo tanto no tiene id asignado).
     */
    public Categoria(String nombre) {
        this(null, nombre);
    }

    /**
     * Constructor para reconstituir una Categoria ya existente
     * (por ejemplo, a partir de un adaptador de persistencia).
     */
    public Categoria(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    /**
     * Permite modificar el nombre de la categoría.
     */
    public void cambiarNombre(String nuevoNombre) {
        this.nombre = nuevoNombre;
    }
}
