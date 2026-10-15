package com.codefactory.supplychain.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * HU-29: fija el comportamiento de la infraestructura transversal sobre la que se
 * apoyan las HU siguientes (D-16, D-17, D-18, D-24, D-27). Los beans de prueba usan
 * la tabla categoria solo como lugar donde escribir.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class EventosReintentosZonaHorariaTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OperacionDeOrigen origen;

    @Autowired
    private ListenerQueFalla listener;

    @Autowired
    private ServicioInestable servicioInestable;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManagerFactory emf;

    @Test
    void listenerAfterCommitQueFallaNoRevierteElOrigenNiPropagaLaExcepcion() {
        String nombreOrigen = "origen-" + UUID.randomUUID();
        String nombreListener = "listener-" + UUID.randomUUID();
        int ejecucionesPrevias = listener.ejecuciones();

        assertDoesNotThrow(() -> origen.registrar(nombreOrigen, nombreListener));

        // Corrió en el mismo hilo, antes de que registrar() devolviera ("tiempo real").
        assertThat(listener.ejecuciones()).isEqualTo(ejecucionesPrevias + 1);
        assertThat(contarCategorias(nombreOrigen)).isEqualTo(1);
        // Su propia transacción (REQUIRES_NEW) se revirtió al fallar.
        assertThat(contarCategorias(nombreListener)).isZero();
    }

    @Test
    void retryableReintentaHastaQueLaOperacionFunciona() {

        assertThat(servicioInestable.fallaDosVecesYLuegoResponde()).isEqualTo("ok");
        assertThat(servicioInestable.intentos()).isEqualTo(3);
    }

    @Test
    void hibernateRecibeLaZonaHorariaUtc() {
        assertThat(emf.getProperties()).containsEntry("hibernate.jdbc.time_zone", "UTC");
    }

    private Integer contarCategorias(String nombre) {
        return jdbc.queryForObject("SELECT count(*) FROM categoria WHERE nombre = ?", Integer.class, nombre);
    }

    record CategoriaDePruebaRegistrada(String nombreParaElListener) {
    }

    static class OperacionDeOrigen {

        private final JdbcTemplate jdbc;
        private final ApplicationEventPublisher eventos;

        OperacionDeOrigen(JdbcTemplate jdbc, ApplicationEventPublisher eventos) {
            this.jdbc = jdbc;
            this.eventos = eventos;
        }

        @Transactional
        public void registrar(String nombre, String nombreParaElListener) {
            jdbc.update("INSERT INTO categoria (nombre) VALUES (?)", nombre);
            eventos.publishEvent(new CategoriaDePruebaRegistrada(nombreParaElListener));
        }
    }

    static class ListenerQueFalla {

        // Los beans de prueba son proxies de Spring: el estado se lee por método, no por campo.
        private final AtomicInteger ejecuciones = new AtomicInteger();
        private final JdbcTemplate jdbc;

        ListenerQueFalla(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void alRegistrar(CategoriaDePruebaRegistrada evento) {
            ejecuciones.incrementAndGet();
            jdbc.update("INSERT INTO categoria (nombre) VALUES (?)", evento.nombreParaElListener());
            throw new IllegalStateException("falla simulada del listener");
        }

        public int ejecuciones() {
            return ejecuciones.get();
        }
    }

    static class ServicioInestable {

        private final AtomicInteger intentos = new AtomicInteger();

        @Retryable(maxRetries = 2, delay = 10)
        public String fallaDosVecesYLuegoResponde() {
            if (intentos.incrementAndGet() < 3) {
                throw new IllegalStateException("falla transitoria simulada");
            }
            return "ok";
        }

        public int intentos() {
            return intentos.get();
        }
    }

    @TestConfiguration
    static class BeansDePrueba {

        @Bean
        OperacionDeOrigen operacionDeOrigen(JdbcTemplate jdbc, ApplicationEventPublisher eventos) {
            return new OperacionDeOrigen(jdbc, eventos);
        }

        @Bean
        ListenerQueFalla listenerQueFalla(JdbcTemplate jdbc) {
            return new ListenerQueFalla(jdbc);
        }

        @Bean
        ServicioInestable servicioInestable() {
            return new ServicioInestable();
        }
    }
}
