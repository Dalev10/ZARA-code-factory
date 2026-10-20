package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.out.ResolverUmbralPort.UmbralAplicable;
import com.codefactory.supplychain.reposicion.application.port.out.UmbralReposicionRepository;
import com.codefactory.supplychain.reposicion.application.port.out.VarianteConsultaPort;
import com.codefactory.supplychain.reposicion.application.port.out.VarianteConsultaPort.VarianteConsultada;
import com.codefactory.supplychain.reposicion.domain.model.UmbralReposicion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResolverUmbralServiceTest {

    private static final UUID TIENDA = UUID.randomUUID();
    private static final UUID VARIANTE = UUID.randomUUID();
    private static final UUID CATEGORIA = UUID.randomUUID();

    private UmbralReposicionRepository repositorio;
    private VarianteConsultaPort varianteConsulta;
    private ResolverUmbralService servicio;

    @BeforeEach
    void setUp() {
        repositorio = mock(UmbralReposicionRepository.class);
        varianteConsulta = mock(VarianteConsultaPort.class);
        servicio = new ResolverUmbralService(repositorio, varianteConsulta);
        when(repositorio.buscarPorVariante(any())).thenReturn(Optional.empty());
        when(repositorio.buscarPorCategoria(any())).thenReturn(Optional.empty());
    }

    @Test
    void elUmbralDeLaVariantePrevaleceSobreElDeLaCategoria() {
        when(repositorio.buscarPorVariante(VARIANTE))
                .thenReturn(Optional.of(UmbralReposicion.crear(VARIANTE, null, 3, 6)));

        Optional<UmbralAplicable> resuelto = servicio.resolver(TIENDA, VARIANTE);

        assertThat(resuelto).contains(new UmbralAplicable(3, 6));
        verify(varianteConsulta, never()).buscar(any());
    }

    @Test
    void sinUmbralDeVarianteUsaElDeSuCategoria() {
        when(varianteConsulta.buscar(VARIANTE)).thenReturn(Optional.of(new VarianteConsultada(VARIANTE, CATEGORIA)));
        when(repositorio.buscarPorCategoria(CATEGORIA))
                .thenReturn(Optional.of(UmbralReposicion.crear(null, CATEGORIA, 2, 5)));

        assertThat(servicio.resolver(TIENDA, VARIANTE)).contains(new UmbralAplicable(2, 5));
    }

    @Test
    void sinUmbralAplicableDevuelveVacio() {
        when(varianteConsulta.buscar(VARIANTE)).thenReturn(Optional.of(new VarianteConsultada(VARIANTE, CATEGORIA)));

        assertThat(servicio.resolver(TIENDA, VARIANTE)).isEmpty();
    }

    @Test
    void conVarianteDesconocidaDevuelveVacio() {
        when(varianteConsulta.buscar(VARIANTE)).thenReturn(Optional.empty());

        assertThat(servicio.resolver(TIENDA, VARIANTE)).isEmpty();
    }

    @Test
    void hoyLaTiendaNoCambiaElResultado() {
        when(repositorio.buscarPorVariante(VARIANTE))
                .thenReturn(Optional.of(UmbralReposicion.crear(VARIANTE, null, 3, 6)));

        assertThat(servicio.resolver(TIENDA, VARIANTE))
                .isEqualTo(servicio.resolver(UUID.randomUUID(), VARIANTE));
    }
}
