package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.reposicion.application.port.in.TraspasoAutomaticoUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-48: "dos simultáneos no sobrecargan". Dos evaluaciones del mismo par tienda-variante
 * que corren a la vez leen el mismo stock; el bloqueo optimista del inventario (D-24)
 * impide que ambas apliquen su traspaso.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class TraspasoAutomaticoConcurrenciaTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private TraspasoAutomaticoUseCase traspasoAutomatico;
    @Autowired private InventarioUseCase inventario;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("""
            Escenario: dos traspasos automáticos simultáneos no sobrecargan el Almacén
              Dado un Almacén con 2 unidades, una Bodega con 20 y un umbral de mínimo 3 y objetivo 6
              Cuando dos evaluaciones de la misma variante corren a la vez
              Entonces el Almacén queda en 6, no en 10
              Y Almacén + Bodega siguen sumando 22""")
    void dosTraspasosSimultaneosNoSobrecarganElAlmacen() throws Exception {
        UUID tiendaId = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')", tiendaId, "Tienda " + tiendaId);
        UUID almacenId = crearNodo(tiendaId, "ALMACEN");
        UUID bodegaId = crearNodo(tiendaId, "BODEGA_TIENDA");
        UUID varianteId = crearVariante();
        inventario.cargar(almacenId, varianteId, 2, null);
        inventario.cargar(bodegaId, varianteId, 20, null);
        jdbc.update("""
                INSERT INTO umbral_reposicion_interna (id, variante_id, cantidad_minima, cantidad_objetivo)
                VALUES (?, ?, 3, 6)""", UUID.randomUUID(), varianteId);

        CountDownLatch salida = new CountDownLatch(1);
        Callable<Object> evaluar = () -> {
            salida.await();
            try {
                return traspasoAutomatico.evaluar(tiendaId, varianteId);
            } catch (RuntimeException conflicto) {
                return conflicto; // la evaluación que pierde la carrera no aplica nada
            }
        };
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        var resultados = List.of(hilos.submit(evaluar), hilos.submit(evaluar));
        salida.countDown();
        for (var resultado : resultados) {
            resultado.get(30, TimeUnit.SECONDS);
        }
        hilos.shutdown();

        assertThat(aLaMano(almacenId, varianteId)).isEqualTo(6);
        assertThat(aLaMano(almacenId, varianteId) + aLaMano(bodegaId, varianteId)).isEqualTo(22);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM traspaso_interno WHERE tienda_id = ?",
                Integer.class, tiendaId)).isEqualTo(1);
    }

    private int aLaMano(UUID nodoId, UUID varianteId) {
        return jdbc.queryForObject("SELECT a_la_mano FROM inventario WHERE nodo_id = ? AND variante_id = ?",
                Integer.class, nodoId, varianteId);
    }

    private UUID crearNodo(UUID tiendaId, String tipo) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, ?, ?)", id, tipo, tiendaId);
        return id;
    }

    private UUID crearVariante() {
        UUID categoriaId = UUID.randomUUID();
        jdbc.update("INSERT INTO categoria (id, nombre) VALUES (?, ?)", categoriaId, "Categoría " + categoriaId);
        UUID templateId = UUID.randomUUID();
        jdbc.update("INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)",
                templateId, categoriaId, "Template " + templateId);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)", id, templateId, "SKU-" + id);
        return id;
    }
}
