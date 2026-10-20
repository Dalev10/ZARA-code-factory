package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.catalogo;

import com.codefactory.supplychain.catalogo.api.ConsultaVariantes;
import com.codefactory.supplychain.catalogo.api.VarianteResumen;
import com.codefactory.supplychain.reposicion.application.port.out.VarianteConsultaPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Capa anticorrupción hacia catalogo: el único archivo de
 * reposicion que importa {@code catalogo.api}. Solo traduce y delega.
 */
@Component
public class VarianteConsultaAdapter implements VarianteConsultaPort {

    private final ConsultaVariantes consultaVariantes;

    public VarianteConsultaAdapter(ConsultaVariantes consultaVariantes) {
        this.consultaVariantes = consultaVariantes;
    }

    @Override
    public Optional<VarianteConsultada> buscar(UUID varianteId) {
        return consultaVariantes.buscar(varianteId).map(VarianteConsultaAdapter::traducir);
    }

    private static VarianteConsultada traducir(VarianteResumen resumen) {
        return new VarianteConsultada(resumen.id(), resumen.categoriaId());
    }
}
