package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.in.CargaInicialComando;
import com.codefactory.supplychain.inventario.application.port.in.ItemCargaInicial;
import com.codefactory.supplychain.inventario.application.port.in.ResultadoCargaInicial;
import com.codefactory.supplychain.inventario.application.port.out.InventarioRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.VariantesCatalogoPort;
import com.codefactory.supplychain.inventario.domain.exception.AdministracionInventarioNoPermitidaException;
import com.codefactory.supplychain.inventario.domain.exception.InventarioYaCargadoException;
import com.codefactory.supplychain.inventario.domain.exception.LoteCargaInvalidoException;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CargaInicialServiceTest {

    private AccesoNodoInventario acceso;
    private VariantesCatalogoPort variantes;
    private InventarioRepositoryPort inventarioRepository;
    private InventarioService inventarioService;
    private CargaInicialService servicio;

    private final UUID nodoId = UUID.randomUUID();
    private final UsuarioActual usuario =
            new UsuarioActual(UUID.randomUUID(), Set.of("inventario:administrar"), Set.of());
    private final UUID idX = UUID.randomUUID();
    private final UUID idY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        acceso = mock(AccesoNodoInventario.class);
        variantes = mock(VariantesCatalogoPort.class);
        inventarioRepository = mock(InventarioRepositoryPort.class);
        inventarioService = mock(InventarioService.class);
        servicio = new CargaInicialService(acceso, variantes, inventarioRepository, inventarioService);
        when(variantes.resolverIdsPorSku(any())).thenReturn(Map.of("SKU-X", idX, "SKU-Y", idY));
        when(inventarioRepository.buscarVariantesCargadas(eq(nodoId), any())).thenReturn(Set.of());
    }

    private CargaInicialComando comando(ItemCargaInicial... items) {
        return new CargaInicialComando(nodoId, List.of(items), usuario);
    }

    @Test
    void cargaCadaItemConElAutorYDevuelveElResumen() {
        ResultadoCargaInicial resultado = servicio.cargar(comando(
                new ItemCargaInicial("SKU-X", 4), new ItemCargaInicial("SKU-Y", 0)));

        assertThat(resultado).isEqualTo(new ResultadoCargaInicial(nodoId, 2, 4));
        verify(inventarioService).cargar(nodoId, idX, 4, usuario.id());
        verify(inventarioService).cargar(nodoId, idY, 0, usuario.id());
    }

    @Test
    void sinAccesoAlNodoNoConsultaNiCarga() {
        doThrow(new AdministracionInventarioNoPermitidaException("inventario-cd:administrar"))
                .when(acceso).exigirAdministracion(nodoId, usuario);

        assertThatThrownBy(() -> servicio.cargar(comando(new ItemCargaInicial("SKU-X", 1))))
                .isInstanceOf(AdministracionInventarioNoPermitidaException.class);
        verifyNoInteractions(variantes, inventarioService);
    }

    @Test
    void rechazaUnLoteVacioOMayorA500Items() {
        assertThatThrownBy(() -> servicio.cargar(comando()))
                .isInstanceOf(LoteCargaInvalidoException.class);

        ItemCargaInicial[] items = IntStream.rangeClosed(0, CargaInicialService.MAXIMO_ITEMS_POR_LOTE)
                .mapToObj(i -> new ItemCargaInicial("SKU-" + i, 1))
                .toArray(ItemCargaInicial[]::new);
        assertThatThrownBy(() -> servicio.cargar(comando(items)))
                .isInstanceOf(LoteCargaInvalidoException.class);
        verifyNoInteractions(inventarioService);
    }

    @Test
    void rechazaCantidadesNegativas() {
        assertThatThrownBy(() -> servicio.cargar(comando(new ItemCargaInicial("SKU-X", -1))))
                .isInstanceOf(LoteCargaInvalidoException.class);
        verifyNoInteractions(inventarioService);
    }

    @Test
    void rechazaSkusRepetidosListandolos() {
        assertThatThrownBy(() -> servicio.cargar(comando(
                new ItemCargaInicial("SKU-X", 1), new ItemCargaInicial("SKU-X", 2))))
                .isInstanceOfSatisfying(LoteCargaInvalidoException.class,
                        e -> assertThat(e.getSkus()).containsExactly("SKU-X"));
        verifyNoInteractions(inventarioService);
    }

    @Test
    void rechazaSkusInexistentesSinCargarNinguno() {
        assertThatThrownBy(() -> servicio.cargar(comando(
                new ItemCargaInicial("SKU-X", 1), new ItemCargaInicial("SKU-FALSO", 2))))
                .isInstanceOfSatisfying(LoteCargaInvalidoException.class, e -> {
                    assertThat(e.getSkus()).containsExactly("SKU-FALSO");
                    assertThat(e.getMessage()).contains("SKU-FALSO");
                });
        verify(inventarioService, never()).cargar(any(), any(), anyInt(), any());
    }

    @Test
    void unaColisionRechazaElLoteCompletoListandoLosSkus() {
        when(inventarioRepository.buscarVariantesCargadas(nodoId, Set.of(idX, idY))).thenReturn(Set.of(idX));

        assertThatThrownBy(() -> servicio.cargar(comando(
                new ItemCargaInicial("SKU-X", 1), new ItemCargaInicial("SKU-Y", 2))))
                .isInstanceOfSatisfying(InventarioYaCargadoException.class, e -> {
                    assertThat(e.getSkus()).containsExactly("SKU-X");
                    assertThat(e.getMessage()).contains("SKU-X");
                });
        verify(inventarioService, never()).cargar(any(), any(), anyInt(), any());
    }
}
