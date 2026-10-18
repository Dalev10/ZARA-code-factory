package com.codefactory.supplychain.inventario.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class InventarioRetryableServiceTest {

    @Test
    void mantieneElReintentoFueraDeLaTransaccion() {
        Method[] metodosDeEntrada = InventarioRetryableService.class.getDeclaredMethods();
        Method[] metodosTransaccionales = InventarioService.class.getDeclaredMethods();

        Arrays.stream(metodosDeEntrada)
                .filter(this::esOperacionDeInventario)
                .forEach(metodo -> {
                    Retryable retryable = metodo.getAnnotation(Retryable.class);
                    assertThat(retryable).isNotNull();
                    assertThat(retryable.maxRetries()).isEqualTo(3);
                    assertThat(retryable.includes())
                            .containsExactly(OptimisticLockingFailureException.class);
                    assertThat(metodo.isAnnotationPresent(Transactional.class)).isFalse();
                });

        assertThat(Arrays.stream(metodosTransaccionales)
                .filter(this::esOperacionDeInventario))
                .allMatch(metodo -> metodo.isAnnotationPresent(Transactional.class)
                        && !metodo.isAnnotationPresent(Retryable.class));
    }

    private boolean esOperacionDeInventario(Method metodo) {
        return Modifier.isPublic(metodo.getModifiers()) && !metodo.isSynthetic();
    }
}
