package com.codefactory.supplychain.inventario.infrastructure.adapter.out.catalogo;

import com.codefactory.supplychain.catalogo.api.ConsultaVariantes;
import com.codefactory.supplychain.catalogo.api.VarianteResumen;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VariantesCatalogoAdapterTest {

    @Test
    void traduceLasVariantesEncontradasAUnMapaSkuId() {
        ConsultaVariantes consulta = mock(ConsultaVariantes.class);
        UUID id = UUID.randomUUID();
        when(consulta.buscarPorSkus(Set.of("SKU-1", "SKU-2")))
                .thenReturn(List.of(new VarianteResumen(id, "SKU-1")));

        Map<String, UUID> resultado = new VariantesCatalogoAdapter(consulta)
                .resolverIdsPorSku(Set.of("SKU-1", "SKU-2"));

        assertThat(resultado).containsExactly(Map.entry("SKU-1", id));
    }
}
