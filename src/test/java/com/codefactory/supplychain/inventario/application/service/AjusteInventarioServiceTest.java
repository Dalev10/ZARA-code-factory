package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.in.AjusteInventarioComando;
import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.inventario.application.port.out.VariantesCatalogoPort;
import com.codefactory.supplychain.inventario.domain.exception.NodoNoEncontradoException;
import com.codefactory.supplychain.inventario.domain.exception.SkuInexistenteException;
import com.codefactory.supplychain.inventario.domain.model.MotivoAjuste;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AjusteInventarioServiceTest {

    private AccesoNodoInventario acceso;
    private VariantesCatalogoPort variantes;
    private InventarioUseCase inventarioUseCase;
    private AjusteInventarioService servicio;

    private final UUID nodoId = UUID.randomUUID();
    private final UUID varianteId = UUID.randomUUID();
    private final UsuarioActual usuario =
            new UsuarioActual(UUID.randomUUID(), Set.of("inventario:administrar"), Set.of());

    @BeforeEach
    void setUp() {
        acceso = mock(AccesoNodoInventario.class);
        variantes = mock(VariantesCatalogoPort.class);
        inventarioUseCase = mock(InventarioUseCase.class);
        servicio = new AjusteInventarioService(acceso, variantes, inventarioUseCase);
        when(variantes.resolverIdsPorSku(Set.of("SKU-X"))).thenReturn(Map.of("SKU-X", varianteId));
    }

    private AjusteInventarioComando comando(String sku) {
        return new AjusteInventarioComando(nodoId, sku, -2, MotivoAjuste.MERMA, "Rotura", usuario);
    }

    @Test
    void delegaElAjusteConMotivoComentarioYAutor() {
        servicio.ajustar(comando("SKU-X"));

        verify(acceso).exigirAdministracion(nodoId, usuario);
        verify(inventarioUseCase).aplicarAjuste(
                nodoId, varianteId, -2, MotivoAjuste.MERMA, "Rotura", usuario.id());
    }

    @Test
    void sinAccesoAlNodoNoConsultaNiAjusta() {
        doThrow(new NodoNoEncontradoException(nodoId)).when(acceso).exigirAdministracion(nodoId, usuario);

        assertThatThrownBy(() -> servicio.ajustar(comando("SKU-X")))
                .isInstanceOf(NodoNoEncontradoException.class);
        verifyNoInteractions(variantes, inventarioUseCase);
    }

    @Test
    void unSkuInexistenteSeRechazaSinAjustar() {
        when(variantes.resolverIdsPorSku(Set.of("SKU-FALSO"))).thenReturn(Map.of());

        assertThatThrownBy(() -> servicio.ajustar(comando("SKU-FALSO")))
                .isInstanceOf(SkuInexistenteException.class)
                .hasMessageContaining("SKU-FALSO");
        verifyNoInteractions(inventarioUseCase);
    }
}
