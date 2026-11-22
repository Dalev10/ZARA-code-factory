-- ============================================================================
-- V37__aprobacion_recomendacion.sql
-- Aprobar o rechazar una recomendación de reposición al CD.
--
-- 1. aprobado_por pasa de VARCHAR(150) a UUID: sale del token (UsuarioActual.id), que es un UUID.
-- 2. version: bloqueo optimista (@Version). El reintento va fuera de la
--    transacción, en el caso de uso de entrada. Evita que dos
--    aprobaciones simultáneas creen dos órdenes.
-- ============================================================================

ALTER TABLE recomendacion
    ALTER COLUMN aprobado_por TYPE UUID USING aprobado_por::uuid,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
