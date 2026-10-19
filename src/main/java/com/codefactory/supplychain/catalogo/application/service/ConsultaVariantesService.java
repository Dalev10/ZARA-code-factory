package com.codefactory.supplychain.catalogo.application.service;

import com.codefactory.supplychain.catalogo.api.ConsultaVariantes;
import com.codefactory.supplychain.catalogo.api.VarianteResumen;
import com.codefactory.supplychain.catalogo.application.port.out.VarianteRepository;
import com.codefactory.supplychain.catalogo.domain.model.Variante;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Implementa el contrato público {@link ConsultaVariantes}: traduce {@link Variante} al
 * {@link VarianteResumen} publicado y no expone nada del dominio de catalogo.
 * Clase aparte de {@code VarianteService} a propósito: solo cubre lo que otros módulos
 * consumen, y es la única implementación de {@link ConsultaVariantes}.
 */
@Service
@Transactional(readOnly = true)
public class ConsultaVariantesService implements ConsultaVariantes {

    private final VarianteRepository varianteRepository;

    public ConsultaVariantesService(VarianteRepository varianteRepository) {
        this.varianteRepository = varianteRepository;
    }

    @Override
    public Optional<VarianteResumen> buscar(UUID varianteId) {
        return varianteRepository.findById(varianteId).map(ConsultaVariantesService::traducir);
    }

    @Override
    public Optional<VarianteResumen> buscarPorSku(String sku) {
        return varianteRepository.findBySku(sku).map(ConsultaVariantesService::traducir);
    }

    @Override
    public List<VarianteResumen> buscarPorSkus(Set<String> skus) {
        if (skus.isEmpty()) {
            return List.of();
        }
        return varianteRepository.findBySkuIn(skus).stream()
                .map(ConsultaVariantesService::traducir)
                .toList();
    }

    private static VarianteResumen traducir(Variante variante) {
        return new VarianteResumen(
                variante.getId(),
                variante.getSku(),
                variante.getTemplate().getCategoria().getId());
    }
}
