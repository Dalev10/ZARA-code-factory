-- ============================================================================
-- V5__crear_refresh_token.sql
-- Soporte para HU-03 (emisión de refresh token en el login). Solo guarda el
-- HASH del token, nunca el valor en texto plano (igual que password_hash).
-- Los campos de revocación/rotación (HU-05/HU-06) se agregan en una migración
-- posterior cuando esas HU los necesiten.
-- ============================================================================

CREATE TABLE refresh_token (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    usuario_id  UUID NOT NULL
        REFERENCES usuario(id) ON DELETE CASCADE,

    token_hash  VARCHAR(255) NOT NULL UNIQUE,

    creado_en   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_en   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_refresh_token_usuario
    ON refresh_token (usuario_id);
