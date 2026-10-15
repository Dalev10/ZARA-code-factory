package com.codefactory.supplychain.inventario.api;

import java.util.UUID;

/**
 * Lo que otros módulos pueden saber de una tienda. No expone el modelo de dominio Tienda.
 */
public record TiendaResumen(UUID id, String nombre, boolean activa) {
}
