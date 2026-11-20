-- ============================================================================
-- V33__orden_reabastecimiento_fallback_cd.sql
-- HU-45: orden de reabastecimiento con fallback de CD.
--
-- 1. orden_reabastecimiento:
--    * variante_id pasa a NOT NULL.
--    * cantidad_despachada_acumulada.
--    * version para el bloqueo optimista.
--    * fecha_creacion en TIMESTAMPTZ.
--    * CHECK de cantidades y de coherencia estado/CD: GENERADA sin CD; cualquier
--      otro estado con CD.
-- 2. intento_cd:
--    * motivo del descarte (disponible frente a requerido).
--    * timestamp_intento en TIMESTAMPTZ.
--    * UNIQUE (orden_id, orden_evaluado): una posición de la lista por intento.
--
-- Las tablas no tienen filas en uso; los UPDATE de V1 no aplican.
-- ============================================================================

ALTER TABLE orden_reabastecimiento
    ALTER COLUMN variante_id SET NOT NULL,
    ADD COLUMN cantidad_despachada_acumulada INT NOT NULL DEFAULT 0,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ALTER COLUMN fecha_creacion TYPE TIMESTAMPTZ USING fecha_creacion AT TIME ZONE 'UTC',
    ADD CONSTRAINT chk_orden_cantidad_total_positiva
        CHECK (cantidad_total_solicitada > 0),
    ADD CONSTRAINT chk_orden_cantidad_pendiente_rango
        CHECK (cantidad_pendiente >= 0 AND cantidad_pendiente <= cantidad_total_solicitada),
    ADD CONSTRAINT chk_orden_cantidad_despachada_no_negativa
        CHECK (cantidad_despachada_acumulada >= 0),
    ADD CONSTRAINT chk_orden_cd_segun_estado
        CHECK ((estado = 'GENERADA' AND cd_asignado_id IS NULL)
            OR (estado <> 'GENERADA' AND cd_asignado_id IS NOT NULL));

CREATE INDEX idx_orden_reabastecimiento_recomendacion
    ON orden_reabastecimiento (recomendacion_id);

ALTER TABLE intento_cd
    ADD COLUMN motivo VARCHAR(255),
    ALTER COLUMN timestamp_intento TYPE TIMESTAMPTZ USING timestamp_intento AT TIME ZONE 'UTC',
    ADD CONSTRAINT uq_intento_cd_orden_posicion UNIQUE (orden_id, orden_evaluado),
    ADD CONSTRAINT chk_intento_cd_orden_evaluado_positivo CHECK (orden_evaluado > 0);
