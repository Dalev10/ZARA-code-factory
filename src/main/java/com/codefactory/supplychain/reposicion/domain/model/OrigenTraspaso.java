package com.codefactory.supplychain.reposicion.domain.model;

/**
 * Discrimina si un traspaso fue generado por el sistema o ejecutado
 * manualmente por un operario.
 *
 * Módulo: reposicion — Reposición interna.
 */
public enum OrigenTraspaso {
    AUTOMATICO,
    MANUAL
}
