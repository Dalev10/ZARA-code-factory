-- ============================================================================
-- V22__traspaso_interno_origen_y_registrado_por.sql
-- HU-39: traspaso automático Bodega → Almacén.
--
-- La columna cantidad ya existe desde V1; se añade el CHECK > 0 que faltaba.
-- ============================================================================

-- 1. Origen con DEFAULT AUTOMATICO
ALTER TABLE traspaso_interno
    ADD COLUMN IF NOT EXISTS origen VARCHAR(10) NOT NULL DEFAULT 'AUTOMATICO';

ALTER TABLE traspaso_interno
    ADD CONSTRAINT chk_traspaso_origen
        CHECK (origen IN ('AUTOMATICO', 'MANUAL'));

-- 2. registrado_por: nullable, FK a usuario; obligatorio si origen = 'MANUAL'
ALTER TABLE traspaso_interno
    ADD COLUMN IF NOT EXISTS registrado_por UUID REFERENCES usuario(id) ON DELETE RESTRICT;

ALTER TABLE traspaso_interno
    ADD CONSTRAINT chk_traspaso_manual_requiere_registrador
        CHECK (origen <> 'MANUAL' OR registrado_por IS NOT NULL);

-- 3. cantidad > 0 (el cálculo siempre produce un entero positivo, pero la BD valida)
ALTER TABLE traspaso_interno
    ADD CONSTRAINT chk_traspaso_cantidad_positiva
        CHECK (cantidad > 0);
