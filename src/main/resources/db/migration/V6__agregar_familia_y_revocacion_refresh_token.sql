-- ============================================================================
-- V6__agregar_familia_y_revocacion_refresh_token.sql
-- Soporte para HU-05 (rotación + detección de reuse):
--
-- - familia_id: agrupa todos los refresh tokens descendientes de un mismo
--   login. Cada rotación genera un token NUEVO en la MISMA familia. Si se
--   detecta que un token ya revocado (rotado) vuelve a presentarse, se
--   interpreta como posible robo y se revoca TODA la familia de una vez.
-- - revocado_en: NULL = token activo. No-NULL = fue rotado (reemplazado por
--   uno nuevo) o revocado por detección de reuse. Nunca se borran filas —
--   sin el historial no habría forma de distinguir "token que nunca existió"
--   de "token que ya fue usado una vez" (esto último es lo que dispara la
--   detección de reuse).
-- ============================================================================

ALTER TABLE refresh_token
    ADD COLUMN familia_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN revocado_en TIMESTAMP WITH TIME ZONE NULL;

CREATE INDEX idx_refresh_token_familia
    ON refresh_token (familia_id);
