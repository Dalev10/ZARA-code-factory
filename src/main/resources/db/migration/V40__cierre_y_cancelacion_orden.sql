-- ============================================================================
-- V40__cierre_y_cancelacion_orden.sql
-- HU-53 (Sprint 3): cerrar con faltante una orden REABIERTA o cancelar una
-- GENERADA sin CD.
--
-- * Estado CANCELADA. Como GENERADA, no tiene CD: nunca reservó nada (D-102).
-- * motivo_cierre, cerrada_por y fecha_cierre: quién decidió terminar la orden
--   antes de recibirla completa, y por qué. Una CERRADA por recepción total
--   no los tiene; una CANCELADA siempre.
--
-- Es V40: siguiente número libre tras V39 (HU-47).
-- ============================================================================

ALTER TABLE orden_reabastecimiento
    DROP CONSTRAINT orden_reabastecimiento_estado_check,
    ADD CONSTRAINT orden_reabastecimiento_estado_check CHECK (estado IN (
        'GENERADA', 'APROBADA', 'EN_DESPACHO', 'DESPACHADA', 'EN_TRANSITO', 'RECIBIDA_TOTAL',
        'RECIBIDA_PARCIAL', 'REABIERTA', 'CERRADA', 'CANCELADA')),
    DROP CONSTRAINT chk_orden_cd_segun_estado,
    ADD CONSTRAINT chk_orden_cd_segun_estado
        CHECK ((estado IN ('GENERADA', 'CANCELADA') AND cd_asignado_id IS NULL)
            OR (estado NOT IN ('GENERADA', 'CANCELADA') AND cd_asignado_id IS NOT NULL)),
    ADD COLUMN motivo_cierre VARCHAR(500),
    ADD COLUMN cerrada_por UUID REFERENCES usuario(id),
    ADD COLUMN fecha_cierre TIMESTAMPTZ,
    ADD CONSTRAINT chk_orden_cierre_completo
        CHECK ((motivo_cierre IS NULL) = (cerrada_por IS NULL) AND (motivo_cierre IS NULL) = (fecha_cierre IS NULL)),
    ADD CONSTRAINT chk_orden_cancelada_con_motivo CHECK (estado <> 'CANCELADA' OR motivo_cierre IS NOT NULL);
