-- ============================================================================
-- V18__restringir_motivos_de_ajuste.sql
-- HU-29: ajuste de inventario con motivo cerrado (D-29).
--
-- Solo restringe filas de tipo AJUSTE: los demás movimientos conservan el
-- motivo libre. movimiento_inventario nace en V17 y todavía no tiene ajustes
-- productivos, así que no hay datos que tratar antes de los CHECK.
-- ============================================================================

ALTER TABLE movimiento_inventario
    ADD CONSTRAINT chk_movimiento_inventario_motivo_ajuste
        CHECK (tipo <> 'AJUSTE'
               OR motivo IN ('CONTEO_FISICO', 'INGRESO_MANUAL', 'MERMA', 'CORRECCION')),
    ADD CONSTRAINT chk_movimiento_inventario_comentario_correccion
        CHECK (tipo <> 'AJUSTE'
               OR motivo <> 'CORRECCION'
               OR btrim(coalesce(comentario, '')) <> '');
