package com.codefactory.supplychain.inventario.domain.model;

import com.codefactory.supplychain.inventario.domain.exception.TiendaInvalidaException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Tienda {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final String nombre;
    private final String ubicacion;
    private final EstadoTienda estado;
    private final Instant creadoEn;
    private final Instant actualizadoEn;

    private Tienda(UUID id, String nombre, String ubicacion, EstadoTienda estado,
                    Instant creadoEn, Instant actualizadoEn) {
        this.id = id;
        this.nombre = validarNombre(nombre);
        this.ubicacion = ubicacion;
        this.estado = estado;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    public static Tienda crear(String nombre, String ubicacion) {
        Instant ahora = Instant.now();
        return new Tienda(UUID.randomUUID(), nombre, ubicacion, EstadoTienda.ACTIVA, ahora, ahora);
    }

    public static Tienda reconstruir(UUID id, String nombre, String ubicacion, EstadoTienda estado,
                                      Instant creadoEn, Instant actualizadoEn) {
        return new Tienda(id, nombre, ubicacion, estado, creadoEn, actualizadoEn);
    }

    public Tienda actualizar(String nuevoNombre, String nuevaUbicacion) {
        return new Tienda(id, nuevoNombre, nuevaUbicacion, estado, creadoEn, Instant.now());
    }

    public Tienda desactivar() {
        return new Tienda(id, nombre, ubicacion, EstadoTienda.INACTIVA, creadoEn, Instant.now());
    }

    public boolean estaActiva() {
        return estado == EstadoTienda.ACTIVA;
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new TiendaInvalidaException("El nombre de la tienda no puede estar vacío");
        }
        if (nombre.length() > 150) {
            throw new TiendaInvalidaException("El nombre de la tienda no puede superar 150 caracteres");
        }
        return nombre;
    }
}
