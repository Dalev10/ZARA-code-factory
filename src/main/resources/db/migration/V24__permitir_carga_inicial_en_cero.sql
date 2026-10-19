-- ============================================================================
-- V24__permitir_carga_inicial_en_cero.sql
-- HU-28: una carga inicial con 0 unidades es válida (la variante queda
-- "cargada" con a_la_mano 0 y su movimiento CARGA_INICIAL va al kardex).
--
-- V17 exigía cantidad <> 0 para todo movimiento, lo que hacía fallar ese
-- INSERT. El resto de movimientos (venta, ajuste, reposición, traspaso...)
-- sigue sin poder tener cantidad 0.
-- ============================================================================

ALTER TABLE movimiento_inventario
    DROP CONSTRAINT IF EXISTS movimiento_inventario_cantidad_check;

ALTER TABLE movimiento_inventario
    ADD CONSTRAINT movimiento_inventario_cantidad_check
        CHECK (cantidad <> 0 OR tipo = 'CARGA_INICIAL');
