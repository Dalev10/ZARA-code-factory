package com.codefactory.supplychain.ventas.infrastructure.adapter.out.catalogo;

import com.codefactory.supplychain.ventas.application.port.out.VarianteVentaPort;
import com.codefactory.supplychain.catalogo.api.ConsultaVariantes;
import com.codefactory.supplychain.catalogo.api.VarianteResumen;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class VarianteVentaAdapter implements VarianteVentaPort {

    private final ConsultaVariantes consultaVariantes;

    @Override
    public Optional<UUID> buscarIdPorSku(String sku) {
        return consultaVariantes.buscarPorSku(sku).map(VarianteResumen::id);
    }
}
