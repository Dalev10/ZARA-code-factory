-- ============================================================================
-- V21__ajustar_umbral_reposicion_interna.sql
-- HU-38 (mapa HU-47): umbrales de reposición interna Bodega -> Almacén.
-- ============================================================================

ALTER TABLE umbral_reposicion_interna
    DROP CONSTRAINT IF EXISTS chk_umbral_tiene_alcance;

ALTER TABLE umbral_reposicion_interna
    ADD COLUMN cantidad_objetivo INT NOT NULL;

ALTER TABLE umbral_reposicion_interna
    ADD CONSTRAINT chk_umbral_alcance_exclusivo
        CHECK (num_nonnulls(variante_id, categoria_id) = 1),
    ADD CONSTRAINT chk_umbral_minimo_positivo
        CHECK (cantidad_minima > 0),
    ADD CONSTRAINT chk_umbral_objetivo_mayor_que_minimo
        CHECK (cantidad_objetivo > cantidad_minima);

-- Un solo umbral por variante y uno solo por categoría (D-51). Parciales porque
-- la columna del otro alcance es NULL.
CREATE UNIQUE INDEX uq_umbral_variante
    ON umbral_reposicion_interna (variante_id)
    WHERE variante_id IS NOT NULL;

CREATE UNIQUE INDEX uq_umbral_categoria
    ON umbral_reposicion_interna (categoria_id)
    WHERE categoria_id IS NOT NULL;
