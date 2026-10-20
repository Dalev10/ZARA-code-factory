package com.codefactory.supplychain.ventas.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface VarianteVentaPort {

    Optional<UUID> buscarIdPorSku(String sku);
}
