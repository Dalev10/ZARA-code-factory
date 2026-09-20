-- ============================================================================
-- V9__agregar_trigger_actualizado_en_tienda.sql
-- Mantiene tienda.actualizado_en consistente ante cualquier UPDATE sobre la
-- fila, reutilizando la función genérica que V3 ya creó para usuario.
-- ============================================================================

CREATE TRIGGER trg_tienda_actualizado_en
    BEFORE UPDATE ON tienda
    FOR EACH ROW
    EXECUTE FUNCTION fn_actualizar_timestamp_actualizado_en();
