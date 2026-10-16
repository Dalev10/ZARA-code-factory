package com.codefactory.supplychain.inventario.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * SPI que Inventario consume para consultar información estable de variantes
 * publicada por Catálogo.
 *
 * Este contrato solo define firmas; el adaptador anticorrupción se agregará
 * cuando se implemente la integración con Catálogo.
 */
public interface CatalogoConsulta {

    /**
     * Busca una variante por su identificador.
     *
     * TODO: confirmar con Altamiranda el identificador y los
     * datos de variante que necesitará para sus operaciones.
     */
    Optional<VarianteResumen> buscarVariante(UUID varianteId);

    /**
     * Busca una variante por su SKU.
     *
     * TODO: confirmar con Cerquera si SKU, categoría y
     * existencia son los datos definitivos para sus reglas.
     */
    Optional<VarianteResumen> buscarVariantePorSku(String sku);

    record VarianteResumen(UUID id, String sku, UUID categoriaId) {
    }
}
