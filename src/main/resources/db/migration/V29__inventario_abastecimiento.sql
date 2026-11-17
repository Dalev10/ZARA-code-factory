-- ============================================================================
-- V29__inventario_abastecimiento.sql
-- HD-01: inventario para el abastecimiento CD -> Bodega_Tienda (Sprint 3).
--
-- 1. entrante y saliente dejan de valer siempre 0: reservar suma saliente en el
--    CD y entrante en la Bodega_Tienda (D-81, reemplaza a D-23). Se quitan los
--    CHECK de V17 que los fijaban en 0.
-- 2. disponible_para_uso pasa a ser a_la_mano - saliente. No hay CHECK de
--    disponible >= 0: un ajuste de conteo puede dejarlo negativo (D-105).
-- 3. El kardex suma DESPACHO y RECEPCION, los dos únicos movimientos nuevos que
--    cambian a_la_mano; reservar y liberar no entran al kardex (D-93).
--
-- Las filas existentes tienen entrante = saliente = 0 y disponible = a_la_mano,
-- así que cumplen los CHECK nuevos sin un UPDATE previo.
-- ============================================================================

ALTER TABLE inventario
    DROP CONSTRAINT chk_inventario_sin_entrante,
    DROP CONSTRAINT chk_inventario_sin_saliente,
    DROP CONSTRAINT chk_inventario_disponible_coherente;

ALTER TABLE inventario
    ADD CONSTRAINT chk_inventario_entrante_no_negativo
        CHECK (entrante >= 0),
    ADD CONSTRAINT chk_inventario_saliente_no_negativo
        CHECK (saliente >= 0),
    ADD CONSTRAINT chk_inventario_disponible_coherente
        CHECK (disponible_para_uso = a_la_mano - saliente);

ALTER TABLE movimiento_inventario
    DROP CONSTRAINT movimiento_inventario_tipo_check,
    ADD CONSTRAINT movimiento_inventario_tipo_check CHECK (
        tipo IN ('CARGA_INICIAL', 'AJUSTE', 'VENTA', 'ANULACION_VENTA', 'TRASPASO_SALIDA', 'TRASPASO_ENTRADA',
                 'DESPACHO', 'RECEPCION')
    );
