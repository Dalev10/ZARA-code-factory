package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.out.CentroDistribucionRepository;
import com.codefactory.supplychain.inventario.application.port.out.PrioridadTiendaCdRepository;
import com.codefactory.supplychain.inventario.application.port.out.TiendaRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.CdEnUsoPorPrioridadException;
import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import com.codefactory.supplychain.inventario.domain.model.PrioridadTiendaCd;
import com.codefactory.supplychain.inventario.domain.model.Tienda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrioridadTiendaCdServiceTest {

    private PrioridadTiendaCdRepository repositorio;
    private TiendaRepositoryPort tiendas;
    private CentroDistribucionRepository centros;
    private PrioridadTiendaCdService servicio;

    private static final UUID TIENDA = UUID.randomUUID();
    private static final UUID CD_1 = UUID.randomUUID();
    private static final UUID CD_2 = UUID.randomUUID();
    private static final UUID CD_3 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repositorio = mock(PrioridadTiendaCdRepository.class);
        tiendas = mock(TiendaRepositoryPort.class);
        centros = mock(CentroDistribucionRepository.class);
        when(tiendas.buscarPorId(any())).thenReturn(Optional.of(mock(Tienda.class)));
        when(centros.buscarPorId(any())).thenReturn(Optional.of(mock(CentroDistribucion.class)));
        servicio = new PrioridadTiendaCdService(repositorio, tiendas, centros);
    }

    @Test
    void reemplazarAsignaOrdenesConsecutivosDesdeUno() {
        List<PrioridadTiendaCd> resultado = servicio.reemplazar(TIENDA, List.of(CD_1, CD_2, CD_3));

        assertThat(resultado).hasSize(3);
        assertThat(resultado.get(0).getCdId()).isEqualTo(CD_1);
        assertThat(resultado.get(0).getOrdenPrioridad()).isEqualTo(1);
        assertThat(resultado.get(1).getOrdenPrioridad()).isEqualTo(2);
        assertThat(resultado.get(2).getOrdenPrioridad()).isEqualTo(3);
        verify(repositorio).eliminarPorTienda(TIENDA);
        verify(repositorio).guardarTodas(resultado);
    }

    @Test
    void reemplazarConListaVaciaEliminaTodasLasPrioridades() {
        List<PrioridadTiendaCd> resultado = servicio.reemplazar(TIENDA, List.of());

        assertThat(resultado).isEmpty();
        verify(repositorio).eliminarPorTienda(TIENDA);
        verify(repositorio).guardarTodas(List.of());
    }

    @Test
    void reemplazarAsignaTiendaIdCorrectoATodas() {
        List<PrioridadTiendaCd> resultado = servicio.reemplazar(TIENDA, List.of(CD_1, CD_2));

        assertThat(resultado).allSatisfy(p -> assertThat(p.getTiendaId()).isEqualTo(TIENDA));
    }

    @Test
    void listarDelegaEnElRepositorio() {
        PrioridadTiendaCd p = PrioridadTiendaCd.crear(TIENDA, CD_1, 1);
        when(repositorio.listarPorTienda(TIENDA)).thenReturn(List.of(p));

        assertThat(servicio.listar(TIENDA)).containsExactly(p);
    }

    @Test
    void verificarQueNoEstaEnUsoConCdSinPrioridadesNoLanzaNada() {
        when(repositorio.existePorCd(CD_1)).thenReturn(false);

        servicio.verificarQueNoEstaEnUso(CD_1);
    }

    @Test
    void verificarQueNoEstaEnUsoConCdEnUsoPorPrioridadLanzaExcepcion() {
        when(repositorio.existePorCd(CD_1)).thenReturn(true);

        assertThatThrownBy(() -> servicio.verificarQueNoEstaEnUso(CD_1))
                .isInstanceOf(CdEnUsoPorPrioridadException.class)
                .hasMessageContaining(CD_1.toString());
    }
}
