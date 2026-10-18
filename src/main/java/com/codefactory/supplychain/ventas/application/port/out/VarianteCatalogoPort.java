package com.codefactory.supplychain.ventas.application.port.out;

import java.util.UUID;

/** Validación de variantes del catálogo. Ver MATRIZ_INTEGRACION_INVENTARIO.md (Catálogo aún sin api/). */
public interface VarianteCatalogoPort {

    boolean existe(UUID varianteId);
}
