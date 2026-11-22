-- ============================================================================
-- V32__recomendacion_reposicion_cd.sql
-- HU-43: generar recomendación de reposición CD -> Bodega_Tienda (Sprint 3).
--
-- 1. Estados de la recomendación: BORRADOR, APROBADA, RECHAZADA (D-79). V1 los
--    llamaba PENDIENTE y DESCARTADA; la tabla no tiene filas en uso, pero se
--    renombran por si acaso antes de cambiar el CHECK.
-- 2. Timestamps en TIMESTAMPTZ (D-95): fecha_aprobacion pasa a TIMESTAMPTZ y se
--    agrega fecha_creacion para ordenar el listado.
-- 3. cantidad_sugerida > 0 (HU-43 solo recomienda si es mayor que 0).
-- 4. Una sola recomendación BORRADOR por SKU y tienda (D-77): UNIQUE parcial.
-- 5. orden_reabastecimiento.variante_id (nullable aquí): HU-43 lo necesita para
--    omitir el SKU y la tienda con una orden GENERADA sin CD (D-102, D-103).
--    Acordado adelantarlo a V32; HU-45 (V33) lo deja NOT NULL.
-- ============================================================================

ALTER TABLE recomendacion
    DROP CONSTRAINT recomendacion_estado_check;

UPDATE recomendacion SET estado = 'BORRADOR' WHERE estado = 'PENDIENTE';
UPDATE recomendacion SET estado = 'RECHAZADA' WHERE estado = 'DESCARTADA';

ALTER TABLE recomendacion
    ALTER COLUMN estado SET DEFAULT 'BORRADOR',
    ADD CONSTRAINT chk_recomendacion_estado
        CHECK (estado IN ('BORRADOR', 'APROBADA', 'RECHAZADA')),
    ADD CONSTRAINT chk_recomendacion_cantidad_sugerida_positiva
        CHECK (cantidad_sugerida > 0),
    ALTER COLUMN fecha_aprobacion TYPE TIMESTAMPTZ USING fecha_aprobacion AT TIME ZONE 'UTC',
    ADD COLUMN fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE UNIQUE INDEX uq_recomendacion_borrador_variante_tienda
    ON recomendacion (variante_id, tienda_id)
    WHERE estado = 'BORRADOR';

CREATE INDEX idx_recomendacion_tienda_estado
    ON recomendacion (tienda_id, estado);

ALTER TABLE orden_reabastecimiento
    ADD COLUMN variante_id UUID REFERENCES variante(id);

CREATE INDEX idx_orden_reabastecimiento_tienda_variante_estado
    ON orden_reabastecimiento (tienda_id, variante_id, estado);
