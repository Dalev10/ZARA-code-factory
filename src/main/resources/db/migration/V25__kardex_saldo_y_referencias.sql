-- ============================================================================
-- V25__kardex_saldo_y_referencias.sql
-- Corrección de integración del Sprint 2 (HU-36 / HU-46).
--
-- 1. saldo_resultante: el a_la_mano después de cada movimiento. Lo necesita
--    la reconciliación de ventanas (D-49) y antes solo vivía en memoria. Las
--    filas existentes se completan con la suma acumulada de su kardex.
-- 2. Tipos: REPOSICION no forma parte del plan (el Almacén se repone con
--    TRASPASO_ENTRADA) y ningún caso de uso lo registra.
-- 3. Motivo: lista cerrada de D-29, solo en ajustes. La anulación de venta
--    guardaba su propio tipo como motivo; queda en NULL.
-- ============================================================================

ALTER TABLE movimiento_inventario
    ADD COLUMN saldo_resultante INT;

UPDATE movimiento_inventario m
SET saldo_resultante = acumulado.saldo
FROM (
    SELECT id,
           SUM(cantidad) OVER (PARTITION BY nodo_id, variante_id ORDER BY ocurrido_en, id) AS saldo
    FROM movimiento_inventario
) acumulado
WHERE acumulado.id = m.id;

ALTER TABLE movimiento_inventario
    ALTER COLUMN saldo_resultante SET NOT NULL;

ALTER TABLE movimiento_inventario
    DROP CONSTRAINT movimiento_inventario_tipo_check,
    ADD CONSTRAINT movimiento_inventario_tipo_check CHECK (
        tipo IN ('CARGA_INICIAL', 'AJUSTE', 'VENTA', 'ANULACION_VENTA', 'TRASPASO_SALIDA', 'TRASPASO_ENTRADA')
    );

UPDATE movimiento_inventario
SET motivo = NULL
WHERE tipo <> 'AJUSTE';

ALTER TABLE movimiento_inventario
    DROP CONSTRAINT movimiento_inventario_motivo_check,
    ADD CONSTRAINT movimiento_inventario_motivo_check CHECK (
        (tipo = 'AJUSTE' AND motivo IN ('CONTEO_FISICO', 'INGRESO_MANUAL', 'MERMA', 'CORRECCION'))
        OR (tipo <> 'AJUSTE' AND motivo IS NULL)
    );
