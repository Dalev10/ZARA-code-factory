package com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(VentaRepositoryAdapter.class)
class VentaRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private VentaRepositoryAdapter adapter;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void bloqueoDeTransaccionEsExclusivoHastaQueConfirmaPostgres() throws Exception {
        UUID transaccionId = UUID.randomUUID();
        CountDownLatch primerBloqueoAdquirido = new CountDownLatch(1);
        CountDownLatch liberarPrimeraTransaccion = new CountDownLatch(1);
        CountDownLatch segundoIntentoIniciado = new CountDownLatch(1);
        CountDownLatch segundoBloqueoAdquirido = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> primera = executor.submit(() -> transactionTemplate.execute(status -> {
                adapter.bloquearTransaccion(transaccionId);
                primerBloqueoAdquirido.countDown();
                esperar(liberarPrimeraTransaccion);
                return null;
            }));
            assertThat(primerBloqueoAdquirido.await(10, TimeUnit.SECONDS)).isTrue();

            Future<?> segunda = executor.submit(() -> {
                segundoIntentoIniciado.countDown();
                return transactionTemplate.execute(status -> {
                    adapter.bloquearTransaccion(transaccionId);
                    segundoBloqueoAdquirido.countDown();
                    return null;
                });
            });
            assertThat(segundoIntentoIniciado.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(segundoBloqueoAdquirido.await(250, TimeUnit.MILLISECONDS)).isFalse();

            liberarPrimeraTransaccion.countDown();
            assertThat(segundoBloqueoAdquirido.await(10, TimeUnit.SECONDS)).isTrue();
            primera.get(10, TimeUnit.SECONDS);
            segunda.get(10, TimeUnit.SECONDS);
        } finally {
            liberarPrimeraTransaccion.countDown();
            executor.shutdownNow();
        }
    }

    private static void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release the PostgreSQL lock test");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while holding the PostgreSQL lock test", e);
        }
    }
}
