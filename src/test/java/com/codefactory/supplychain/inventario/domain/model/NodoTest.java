package com.codefactory.supplychain.inventario.domain.model;

import com.codefactory.supplychain.inventario.domain.exception.NodoInvalidoException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NodoTest {

    @Test
    void crearParaCdAsignaIdYReferenciaCoherente() {
        UUID cdId = UUID.randomUUID();

        Nodo nodo = Nodo.crearParaCd(cdId);

        assertThat(nodo.getId()).isNotNull();
        assertThat(nodo.getTipo()).isEqualTo(TipoNodo.CD);
        assertThat(nodo.getCdId()).isEqualTo(cdId);
        assertThat(nodo.getTiendaId()).isNull();
    }

    @Test
    void crearParaTiendaConTipoAlmacen() {
        UUID tiendaId = UUID.randomUUID();

        Nodo nodo = Nodo.crearParaTienda(tiendaId, TipoNodo.ALMACEN);

        assertThat(nodo.getTipo()).isEqualTo(TipoNodo.ALMACEN);
        assertThat(nodo.getTiendaId()).isEqualTo(tiendaId);
        assertThat(nodo.getCdId()).isNull();
    }

    @Test
    void crearParaTiendaConTipoBodegaTienda() {
        UUID tiendaId = UUID.randomUUID();

        Nodo nodo = Nodo.crearParaTienda(tiendaId, TipoNodo.BODEGA_TIENDA);

        assertThat(nodo.getTipo()).isEqualTo(TipoNodo.BODEGA_TIENDA);
        assertThat(nodo.getTiendaId()).isEqualTo(tiendaId);
    }

    @Test
    void crearParaTiendaRechazaTipoCd() {
        assertThatThrownBy(() -> Nodo.crearParaTienda(UUID.randomUUID(), TipoNodo.CD))
                .isInstanceOf(NodoInvalidoException.class);
    }

    @Test
    void reconstruirRechazaUnNodoCdSinCdId() {
        assertThatThrownBy(() -> Nodo.reconstruir(UUID.randomUUID(), TipoNodo.CD, null, null))
                .isInstanceOf(NodoInvalidoException.class);
    }

    @Test
    void reconstruirRechazaUnNodoCdConAmbasReferencias() {
        assertThatThrownBy(() -> Nodo.reconstruir(UUID.randomUUID(), TipoNodo.CD, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NodoInvalidoException.class);
    }

    @Test
    void reconstruirRechazaUnNodoDeTiendaSinTiendaId() {
        assertThatThrownBy(() -> Nodo.reconstruir(UUID.randomUUID(), TipoNodo.ALMACEN, null, null))
                .isInstanceOf(NodoInvalidoException.class);
    }

    @Test
    void reconstruirRechazaTipoNulo() {
        assertThatThrownBy(() -> Nodo.reconstruir(UUID.randomUUID(), null, UUID.randomUUID(), null))
                .isInstanceOf(NodoInvalidoException.class);
    }

    @Test
    void dosNodosConElMismoIdSonIguales() {
        UUID id = UUID.randomUUID();
        UUID cdId = UUID.randomUUID();

        Nodo a = Nodo.reconstruir(id, TipoNodo.CD, cdId, null);
        Nodo b = Nodo.reconstruir(id, TipoNodo.CD, cdId, null);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
