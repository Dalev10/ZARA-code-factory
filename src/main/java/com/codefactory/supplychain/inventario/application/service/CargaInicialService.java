package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.in.CargaInicialComando;
import com.codefactory.supplychain.inventario.application.port.in.CargarInventarioInicialUseCase;
import com.codefactory.supplychain.inventario.application.port.in.ItemCargaInicial;
import com.codefactory.supplychain.inventario.application.port.in.ResultadoCargaInicial;
import com.codefactory.supplychain.inventario.application.port.out.InventarioRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.VariantesCatalogoPort;
import com.codefactory.supplychain.inventario.domain.exception.InventarioYaCargadoException;
import com.codefactory.supplychain.inventario.domain.exception.LoteCargaInvalidoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Carga inicial en lote de un nodo (D-28, D-30, D-31, D-32).
 *
 * Valida el lote completo antes de insertar: si algo falla, no se crea
 * ninguna fila. Cada fila nueva registra su CARGA_INICIAL en el kardex y
 * publica ExistenciasActualizadas a través de {@link InventarioService}.
 *
 * No lleva reintento optimista: solo inserta filas nuevas, sin conflicto de
 * versión. Dos cargas simultáneas del mismo SKU las frena el
 * UNIQUE (nodo_id, variante_id).
 */
@Service
@RequiredArgsConstructor
public class CargaInicialService implements CargarInventarioInicialUseCase {

    static final int MAXIMO_ITEMS_POR_LOTE = 500;

    private final AccesoNodoInventario accesoNodoInventario;
    private final VariantesCatalogoPort variantesCatalogoPort;
    private final InventarioRepositoryPort inventarioRepositoryPort;
    private final InventarioService inventarioService;

    @Override
    @Transactional
    public ResultadoCargaInicial cargar(CargaInicialComando comando) {
        UUID nodoId = comando.nodoId();
        accesoNodoInventario.exigirAdministracion(nodoId, comando.usuario());
        List<ItemCargaInicial> items = comando.items();
        validarLote(items);

        Set<String> skus = skusSinRepetir(items);
        Map<String, UUID> idsPorSku = variantesCatalogoPort.resolverIdsPorSku(skus);
        rechazarSkusInexistentes(skus, idsPorSku);
        rechazarVariantesYaCargadas(nodoId, skus, idsPorSku);

        int unidadesTotales = 0;
        for (ItemCargaInicial item : items) {
            inventarioService.cargar(nodoId, idsPorSku.get(item.sku()), item.cantidad(), comando.usuario().id());
            unidadesTotales += item.cantidad();
        }
        return new ResultadoCargaInicial(nodoId, items.size(), unidadesTotales);
    }

    private static void validarLote(List<ItemCargaInicial> items) {
        if (items.isEmpty() || items.size() > MAXIMO_ITEMS_POR_LOTE) {
            throw new LoteCargaInvalidoException(
                    "El lote debe tener entre 1 y " + MAXIMO_ITEMS_POR_LOTE + " ítems");
        }
        if (items.stream().anyMatch(item -> item.cantidad() < 0)) {
            throw new LoteCargaInvalidoException("Las cantidades del lote no pueden ser negativas");
        }
    }

    private static Set<String> skusSinRepetir(List<ItemCargaInicial> items) {
        Set<String> skus = new LinkedHashSet<>();
        Set<String> repetidos = new LinkedHashSet<>();
        for (ItemCargaInicial item : items) {
            if (!skus.add(item.sku())) {
                repetidos.add(item.sku());
            }
        }
        if (!repetidos.isEmpty()) {
            throw LoteCargaInvalidoException.skusRepetidos(List.copyOf(repetidos));
        }
        return skus;
    }

    private static void rechazarSkusInexistentes(Set<String> skus, Map<String, UUID> idsPorSku) {
        List<String> inexistentes = skus.stream()
                .filter(sku -> !idsPorSku.containsKey(sku))
                .toList();
        if (!inexistentes.isEmpty()) {
            throw LoteCargaInvalidoException.skusInexistentes(inexistentes);
        }
    }

    private void rechazarVariantesYaCargadas(UUID nodoId, Set<String> skus, Map<String, UUID> idsPorSku) {
        Set<UUID> cargadas = inventarioRepositoryPort.buscarVariantesCargadas(
                nodoId, new HashSet<>(idsPorSku.values()));
        List<String> enConflicto = skus.stream()
                .filter(sku -> cargadas.contains(idsPorSku.get(sku)))
                .toList();
        if (!enConflicto.isEmpty()) {
            throw new InventarioYaCargadoException(nodoId, enConflicto);
        }
    }
}
