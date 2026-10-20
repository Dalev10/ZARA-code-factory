package com.codefactory.supplychain.inventario.infrastructure.adapter.out.catalogo;

import com.codefactory.supplychain.catalogo.api.ConsultaVariantes;
import com.codefactory.supplychain.catalogo.api.VarianteResumen;
import com.codefactory.supplychain.inventario.application.port.out.VariantesCatalogoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Anti-Corruption Layer hacia catalogo: único archivo de inventario que
 * importa {@code catalogo.api}. Solo traduce y delega.
 */
@Component
@RequiredArgsConstructor
public class VariantesCatalogoAdapter implements VariantesCatalogoPort {

    private final ConsultaVariantes consultaVariantes;

    @Override
    public Map<String, UUID> resolverIdsPorSku(Set<String> skus) {
        return consultaVariantes.buscarPorSkus(skus).stream()
                .collect(Collectors.toMap(VarianteResumen::sku, VarianteResumen::id));
    }
}
