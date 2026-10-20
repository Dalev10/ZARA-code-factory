package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.catalogo;

import com.codefactory.supplychain.catalogo.api.ConsultaVariantes;
import com.codefactory.supplychain.catalogo.api.VarianteResumen;
import com.codefactory.supplychain.reposicion.application.port.out.VarianteConsultaPort.VarianteConsultada;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VarianteConsultaAdapterTest {

    private final ConsultaVariantes consultaVariantes = mock(ConsultaVariantes.class);
    private final VarianteConsultaAdapter adaptador = new VarianteConsultaAdapter(consultaVariantes);

    @Test
    void traduceElResumenPublicadoAlLenguajeDeReposicion() {
        UUID id = UUID.randomUUID();
        UUID categoria = UUID.randomUUID();
        when(consultaVariantes.buscar(id)).thenReturn(Optional.of(new VarianteResumen(id, "SKU-1", categoria)));

        assertThat(adaptador.buscar(id)).contains(new VarianteConsultada(id, categoria));
    }

    @Test
    void sinVarianteDevuelveVacio() {
        UUID id = UUID.randomUUID();
        when(consultaVariantes.buscar(id)).thenReturn(Optional.empty());

        assertThat(adaptador.buscar(id)).isEmpty();
    }
}
