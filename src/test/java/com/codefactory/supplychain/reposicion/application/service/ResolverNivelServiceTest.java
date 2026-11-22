package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.out.NivelReposicionCdRepository;
import com.codefactory.supplychain.reposicion.application.port.out.ResolverNivelPort.NivelAplicable;
import com.codefactory.supplychain.reposicion.application.port.out.VarianteConsultaPort;
import com.codefactory.supplychain.reposicion.application.port.out.VarianteConsultaPort.VarianteConsultada;
import com.codefactory.supplychain.reposicion.domain.model.NivelReposicionCd;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HD-04: precedencia variante → categoría del nivel de reposición al CD (D-74, patrón D-58). */
class ResolverNivelServiceTest {

    private final NivelReposicionCdRepository niveles = mock(NivelReposicionCdRepository.class);
    private final VarianteConsultaPort variantes = mock(VarianteConsultaPort.class);
    private final ResolverNivelService resolver = new ResolverNivelService(niveles, variantes);
    private final UUID tiendaId = UUID.randomUUID();
    private final UUID varianteId = UUID.randomUUID();
    private final UUID categoriaId = UUID.randomUUID();

    @Test
    void elNivelDeLaVarianteGanaSobreElDeSuCategoria() {
        when(niveles.buscarPorVariante(varianteId))
                .thenReturn(Optional.of(NivelReposicionCd.crear(varianteId, null, 5, 15)));

        assertThat(resolver.resolver(tiendaId, varianteId)).contains(new NivelAplicable(5, 15));
        verifyNoInteractions(variantes);
    }

    @Test
    void sinNivelDeLaVarianteUsaElDeSuCategoria() {
        when(niveles.buscarPorVariante(varianteId)).thenReturn(Optional.empty());
        when(variantes.buscar(varianteId)).thenReturn(Optional.of(new VarianteConsultada(varianteId, categoriaId)));
        when(niveles.buscarPorCategoria(categoriaId))
                .thenReturn(Optional.of(NivelReposicionCd.crear(null, categoriaId, 10, 30)));

        assertThat(resolver.resolver(tiendaId, varianteId)).contains(new NivelAplicable(10, 30));
    }

    @Test
    void sinNingunNivelNoHayNivelAplicable() {
        when(niveles.buscarPorVariante(varianteId)).thenReturn(Optional.empty());
        when(variantes.buscar(varianteId)).thenReturn(Optional.of(new VarianteConsultada(varianteId, categoriaId)));
        when(niveles.buscarPorCategoria(categoriaId)).thenReturn(Optional.empty());

        assertThat(resolver.resolver(tiendaId, varianteId)).isEmpty();
    }
}
