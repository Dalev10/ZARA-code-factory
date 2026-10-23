package com.codefactory.supplychain.qa.nofuncionales.fiabilidad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-36 (Sprint 2). Riesgo aceptado del MVP: con ventas simultáneas del mismo SKU alguna puede
 * responder 409 tras agotar los reintentos, pero no se pierden unidades y reintentar con el mismo
 * transaccionId no duplica la venta.
 */
@Tag("no-funcional")
@Tag("fiabilidad")
class ConcurrenciaInventarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-001 — Ventas simultáneas del mismo SKU no pierden ni duplican unidades")
    void cpHu36001_ventasSimultaneasNoPierdenUnidades() throws Exception {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU36-001");
        Variante variante = crearVariante(tokenAdmin, "HU36-001");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 20);

        int ventas = 8;
        ExecutorService pool = Executors.newFixedThreadPool(ventas);
        List<Callable<Integer>> tareas = new ArrayList<>();
        for (int i = 0; i < ventas; i++) {
            tareas.add(() -> registrarVenta(tokenAdmin, cuerpoVenta(UUID.randomUUID().toString(), tienda.id(),
                    false, linea(variante.sku(), 1))).statusCode());
        }
        int creadas = 0;
        try {
            for (Future<Integer> f : pool.invokeAll(tareas)) {
                int codigo = f.get();
                assertTrue(codigo == 201 || codigo == 409, "código inesperado " + codigo);
                if (codigo == 201) {
                    creadas++;
                }
            }
        } finally {
            pool.shutdown();
        }

        assertEquals(20 - creadas, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-002 — Reintentar la misma transacción en paralelo registra la venta una sola vez")
    void cpHu36002_reintentosParalelosNoDuplican() throws Exception {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU36-002");
        Variante variante = crearVariante(tokenAdmin, "HU36-002");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 10);
        String cuerpo = cuerpoVenta(UUID.randomUUID().toString(), tienda.id(), false, linea(variante.sku(), 2));

        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Callable<Integer>> tareas = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tareas.add(() -> registrarVenta(tokenAdmin, cuerpo).statusCode());
        }
        try {
            for (Future<Integer> f : pool.invokeAll(tareas)) {
                int codigo = f.get();
                assertTrue(codigo == 201 || codigo == 200 || codigo == 409, "código inesperado " + codigo);
            }
        } finally {
            pool.shutdown();
        }

        int codigoFinal = registrarVenta(tokenAdmin, cuerpo).statusCode();
        assertTrue(codigoFinal == 200 || codigoFinal == 201, "el reintento final debe confirmar la venta");
        assertEquals(8, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }
}
