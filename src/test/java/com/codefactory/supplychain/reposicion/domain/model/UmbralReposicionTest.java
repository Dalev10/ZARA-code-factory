package com.codefactory.supplychain.reposicion.domain.model;

import com.codefactory.supplychain.reposicion.domain.exception.UmbralInvalidoException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UmbralReposicionTest {

    private static final UUID VARIANTE = UUID.randomUUID();
    private static final UUID CATEGORIA = UUID.randomUUID();

    @Test
    void crearPorVarianteAsignaIdNuevoYConservaLosDatos() {
        UmbralReposicion umbral = UmbralReposicion.crear(VARIANTE, null, 3, 6);

        assertThat(umbral.getId()).isNotNull();
        assertThat(umbral.getVarianteId()).isEqualTo(VARIANTE);
        assertThat(umbral.getCategoriaId()).isNull();
        assertThat(umbral.getCantidadMinima()).isEqualTo(3);
        assertThat(umbral.getCantidadObjetivo()).isEqualTo(6);
        assertThat(umbral.esPorVariante()).isTrue();
    }

    @Test
    void crearPorCategoriaNoEsPorVariante() {
        UmbralReposicion umbral = UmbralReposicion.crear(null, CATEGORIA, 2, 5);

        assertThat(umbral.getCategoriaId()).isEqualTo(CATEGORIA);
        assertThat(umbral.esPorVariante()).isFalse();
    }

    @Test
    void sinVarianteNiCategoriaSeRechaza() {
        assertThatThrownBy(() -> UmbralReposicion.crear(null, null, 3, 6))
                .isInstanceOf(UmbralInvalidoException.class)
                .hasMessageContaining("variante o a una categoría");
    }

    @Test
    void conVarianteYCategoriaALaVezSeRechaza() {
        assertThatThrownBy(() -> UmbralReposicion.crear(VARIANTE, CATEGORIA, 3, 6))
                .isInstanceOf(UmbralInvalidoException.class)
                .hasMessageContaining("al mismo tiempo");
    }

    @Test
    void minimoCeroSeRechaza() {
        assertThatThrownBy(() -> UmbralReposicion.crear(VARIANTE, null, 0, 6))
                .isInstanceOf(UmbralInvalidoException.class)
                .hasMessageContaining("mínima");
    }

    @Test
    void minimoNegativoSeRechaza() {
        assertThatThrownBy(() -> UmbralReposicion.crear(VARIANTE, null, -1, 6))
                .isInstanceOf(UmbralInvalidoException.class);
    }

    @Test
    void objetivoIgualAlMinimoSeRechaza() {
        assertThatThrownBy(() -> UmbralReposicion.crear(VARIANTE, null, 3, 3))
                .isInstanceOf(UmbralInvalidoException.class)
                .hasMessageContaining("objetivo");
    }

    @Test
    void objetivoMenorAlMinimoSeRechaza() {
        assertThatThrownBy(() -> UmbralReposicion.crear(VARIANTE, null, 5, 3))
                .isInstanceOf(UmbralInvalidoException.class);
    }

    @Test
    void reconstruirConservaElIdYValidaLasMismasReglas() {
        UUID id = UUID.randomUUID();

        assertThat(UmbralReposicion.reconstruir(id, VARIANTE, null, 3, 6).getId()).isEqualTo(id);
        assertThatThrownBy(() -> UmbralReposicion.reconstruir(id, VARIANTE, CATEGORIA, 3, 6))
                .isInstanceOf(UmbralInvalidoException.class);
    }

    @Test
    void cambiarCantidadesDevuelveUnaNuevaInstanciaConElMismoIdYAlcance() {
        UmbralReposicion umbral = UmbralReposicion.crear(VARIANTE, null, 3, 6);

        UmbralReposicion cambiado = umbral.cambiarCantidades(8, 12);

        assertThat(cambiado.getId()).isEqualTo(umbral.getId());
        assertThat(cambiado.getVarianteId()).isEqualTo(VARIANTE);
        assertThat(cambiado.getCantidadMinima()).isEqualTo(8);
        assertThat(cambiado.getCantidadObjetivo()).isEqualTo(12);
        assertThat(umbral.getCantidadMinima()).isEqualTo(3);
        assertThat(umbral.getCantidadObjetivo()).isEqualTo(6);
    }

    @Test
    void cambiarCantidadesSubeMinimoYObjetivoALaVezSinPasoIntermedioInvalido() {
        UmbralReposicion umbral = UmbralReposicion.crear(VARIANTE, null, 3, 6);

        assertThat(umbral.cambiarCantidades(10, 20).getCantidadMinima()).isEqualTo(10);
    }

    @Test
    void cambiarCantidadesInvalidasSeRechazaYNoAlteraElOriginal() {
        UmbralReposicion umbral = UmbralReposicion.crear(VARIANTE, null, 3, 6);

        assertThatThrownBy(() -> umbral.cambiarCantidades(6, 6)).isInstanceOf(UmbralInvalidoException.class);
        assertThat(umbral.getCantidadObjetivo()).isEqualTo(6);
    }
}
