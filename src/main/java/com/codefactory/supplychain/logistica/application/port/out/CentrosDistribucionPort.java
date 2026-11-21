package com.codefactory.supplychain.logistica.application.port.out;

import java.util.UUID;

/** Existencia de un CD. Respaldado por {@code inventario.api}. */
public interface CentrosDistribucionPort {

    boolean existe(UUID cdId);
}
