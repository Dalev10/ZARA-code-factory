package com.codefactory.supplychain.ventas.application.port.out;

import java.util.UUID;

/** Validación de variantes del catálogo. Respaldado por catalogo.api.ConsultaVariantes. */
public interface VarianteCatalogoPort {

    boolean existe(UUID varianteId);
}
