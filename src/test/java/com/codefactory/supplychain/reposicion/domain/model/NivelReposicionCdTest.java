package com.codefactory.supplychain.reposicion.domain.model;

import com.codefactory.supplychain.reposicion.domain.exception.NivelReposicionInvalidoException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HD-04: invariantes del nivel de reposición al CD (D-74). */
class NivelReposicionCdTest {

    private final UUID varianteId = UUID.randomUUID();
    private final UUID categoriaId = UUID.randomUUID();

    @Test
    void aplicaAUnaVarianteOAUnaCategoriaNuncaAAmbasNiANinguna() {
        assertThat(NivelReposicionCd.crear(varianteId, null, 5, 15).esPorVariante()).isTrue();
        assertThat(NivelReposicionCd.crear(null, categoriaId, 5, 15).esPorVariante()).isFalse();
        assertThatThrownBy(() -> NivelReposicionCd.crear(varianteId, categoriaId, 5, 15))
                .isInstanceOf(NivelReposicionInvalidoException.class);
        assertThatThrownBy(() -> NivelReposicionCd.crear(null, null, 5, 15))
                .isInstanceOf(NivelReposicionInvalidoException.class);
    }

    @Test
    void elMinimoEsPositivoYElObjetivoLoSupera() {
        assertThatThrownBy(() -> NivelReposicionCd.crear(varianteId, null, 0, 15))
                .isInstanceOf(NivelReposicionInvalidoException.class);
        assertThatThrownBy(() -> NivelReposicionCd.crear(varianteId, null, 5, 5))
                .isInstanceOf(NivelReposicionInvalidoException.class);
    }

    @Test
    void cambiarCantidadesConservaElAlcanceYValidaElParNuevo() {
        NivelReposicionCd nivel = NivelReposicionCd.crear(varianteId, null, 5, 15);

        NivelReposicionCd cambiado = nivel.cambiarCantidades(20, 40);

        assertThat(cambiado.getId()).isEqualTo(nivel.getId());
        assertThat(cambiado.getVarianteId()).isEqualTo(varianteId);
        assertThat(cambiado.getCantidadMinima()).isEqualTo(20);
        assertThat(cambiado.getCantidadObjetivo()).isEqualTo(40);
        assertThatThrownBy(() -> nivel.cambiarCantidades(40, 20)).isInstanceOf(NivelReposicionInvalidoException.class);
    }
}
