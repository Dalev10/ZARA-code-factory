package com.codefactory.supplychain.qa.soporte;

import java.util.Map;

public final class CatalogoDatos {

    private CatalogoDatos() {
    }

    public static Map<String, Object> categoria(String nombre) {
        return Map.of("nombre", nombre);
    }

    public static Map<String, Object> templateSinCategoria(String nombre) {
        return Map.of(
                "nombre", nombre,
                "temporada", "OI-2026",
                "precioBase", 49900);
    }

    public static Map<String, Object> templateConCategoria(String nombre, String categoriaId) {
        return Map.of(
                "nombre", nombre,
                "categoriaId", categoriaId);
    }

    public static Map<String, Object> templateConCategoriaInexistente(String nombre, String categoriaId) {
        return templateConCategoria(nombre, categoriaId);
    }

    public static Map<String, Object> renombrarCategoria(String nombre) {
        return Map.of("nombre", nombre);
    }

    public static Map<String, Object> variante(String sku, String talla, String color, String templateId) {
        return Map.of(
                "sku", sku,
                "talla", talla,
                "color", color,
                "templateId", templateId);
    }
}
