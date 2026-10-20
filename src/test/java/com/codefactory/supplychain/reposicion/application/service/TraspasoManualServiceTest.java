package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.out.EjecutarTraspasoPort;
import com.codefactory.supplychain.reposicion.application.port.out.TraspasoInternoRepository;
import com.codefactory.supplychain.reposicion.application.port.out.VarianteConsultaPort;
import com.codefactory.supplychain.reposicion.application.port.out.VarianteConsultaPort.VarianteConsultada;
import com.codefactory.supplychain.reposicion.domain.exception.BodegaInsuficienteException;
import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.reposicion.domain.exception.TiendaNoPermitidaException;
import com.codefactory.supplychain.reposicion.domain.model.OrigenTraspaso;
import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("TraspasoManualService (HU-49)")
@ExtendWith(MockitoExtension.class)
class TraspasoManualServiceTest {

    private static final UUID TIENDA_ID = UUID.randomUUID();
    private static final UUID VARIANTE_ID = UUID.randomUUID();
    private static final UsuarioActual OPERARIO =
            new UsuarioActual(UUID.randomUUID(), Set.of("reposicion:administrar"), Set.of(TIENDA_ID));

    @Mock VarianteConsultaPort variantes;
    @Mock EjecutarTraspasoPort ejecutarTraspaso;
    @Mock TraspasoInternoRepository traspasoRepository;

    @InjectMocks
    TraspasoManualService servicio;

    @BeforeEach
    void setUp() {
        lenient().when(traspasoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(variantes.buscar(VARIANTE_ID))
                .thenReturn(Optional.of(new VarianteConsultada(VARIANTE_ID, UUID.randomUUID())));
    }

    @Test
    @DisplayName("persiste un traspaso MANUAL con el usuario del token como registrador")
    void persisteConRegistradorYOrigenManual() {
        when(ejecutarTraspaso.traspasar(eq(TIENDA_ID), eq(VARIANTE_ID), eq(7), any())).thenReturn(true);

        TraspasoInterno resultado = servicio.registrar(OPERARIO, TIENDA_ID, VARIANTE_ID, 7);

        assertThat(resultado.getOrigen()).isEqualTo(OrigenTraspaso.MANUAL);
        assertThat(resultado.getRegistradoPor()).isEqualTo(OPERARIO.id());
        assertThat(resultado.getCantidad()).isEqualTo(7);
        verify(ejecutarTraspaso).traspasar(TIENDA_ID, VARIANTE_ID, 7, resultado.getId());
        verify(traspasoRepository).guardar(resultado);
    }

    @Test
    @DisplayName("si la Bodega no alcanza, responde conflicto y no guarda el traspaso (D-56)")
    void bodegaInsuficienteNoGuarda() {
        when(ejecutarTraspaso.traspasar(eq(TIENDA_ID), eq(VARIANTE_ID), eq(10), any())).thenReturn(false);

        assertThatThrownBy(() -> servicio.registrar(OPERARIO, TIENDA_ID, VARIANTE_ID, 10))
                .isInstanceOf(BodegaInsuficienteException.class);
        verifyNoInteractions(traspasoRepository);
    }

    @Test
    @DisplayName("una tienda no asignada se rechaza antes de tocar inventario")
    void tiendaNoAsignadaSeRechaza() {
        UUID otraTienda = UUID.randomUUID();

        assertThatThrownBy(() -> servicio.registrar(OPERARIO, otraTienda, VARIANTE_ID, 1))
                .isInstanceOf(TiendaNoPermitidaException.class);
        verifyNoInteractions(ejecutarTraspaso, traspasoRepository);
    }

    @Test
    @DisplayName("una variante inexistente responde 404")
    void varianteInexistente() {
        UUID desconocida = UUID.randomUUID();
        when(variantes.buscar(desconocida)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(OPERARIO, TIENDA_ID, desconocida, 1))
                .isInstanceOf(ReposicionRecursoNoEncontradoException.class);
        verifyNoInteractions(ejecutarTraspaso, traspasoRepository);
    }
}
