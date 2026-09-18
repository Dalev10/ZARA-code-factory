-- ============================================================================
-- V7__crear_mfa_codigo_respaldo.sql
-- Soporte para HU-07 (activación de MFA): códigos de un solo uso que permiten
-- iniciar sesión si el usuario pierde acceso a su aplicación TOTP. Solo se
-- guarda el hash de cada código — igual que password_hash y token_hash, nunca
-- el valor en texto plano (que solo se muestra una vez, al momento de generarlos).
-- ============================================================================

CREATE TABLE mfa_codigo_respaldo (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    usuario_id   UUID NOT NULL
        REFERENCES usuario(id) ON DELETE CASCADE,

    codigo_hash  VARCHAR(255) NOT NULL UNIQUE,

    creado_en    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usado_en     TIMESTAMP WITH TIME ZONE NULL
);

CREATE INDEX idx_mfa_codigo_respaldo_usuario
    ON mfa_codigo_respaldo (usuario_id);
