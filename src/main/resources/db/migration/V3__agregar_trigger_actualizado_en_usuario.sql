-- ============================================================================
-- V3__agregar_trigger_actualizado_en_usuario.sql
-- Mantiene usuario.actualizado_en consistente ante cualquier UPDATE sobre la
-- fila, sin depender de que el código de aplicación recuerde setearlo.
-- ============================================================================

CREATE OR REPLACE FUNCTION fn_actualizar_timestamp_actualizado_en()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuario_actualizado_en
    BEFORE UPDATE ON usuario
    FOR EACH ROW
    EXECUTE FUNCTION fn_actualizar_timestamp_actualizado_en();
