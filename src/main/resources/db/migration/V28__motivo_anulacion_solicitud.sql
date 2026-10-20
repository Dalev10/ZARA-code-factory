-- ============================================================================
-- V28__motivo_anulacion_solicitud.sql
-- HU-44 (D-44): la anulación de una solicitud sigue el patrón de la venta y
-- guarda su motivo. El CHECK se crea NOT VALID: las solicitudes anuladas
-- antes de esta migración no tienen motivo.
-- ============================================================================

ALTER TABLE solicitud_no_satisfecha
    ADD COLUMN motivo_anulacion VARCHAR(500);

ALTER TABLE solicitud_no_satisfecha
    ADD CONSTRAINT chk_solicitud_motivo_anulacion CHECK (
        (estado = 'ACTIVA' AND motivo_anulacion IS NULL)
        OR (estado = 'ANULADA' AND motivo_anulacion IS NOT NULL AND btrim(motivo_anulacion) <> '')
    ) NOT VALID;
