-- ============================================================================
-- V15__crear_bodegas_tienda_faltantes.sql
-- HU-25: toda tienda debe tener una Bodega_Tienda.
--
-- Las tiendas nuevas reciben su bodega desde TiendaService dentro de la misma
-- transaccion. Este backfill cubre las tiendas creadas antes de HU-25.
-- ============================================================================

INSERT INTO nodo (id, tipo, cd_id, tienda_id)
SELECT gen_random_uuid(), 'BODEGA_TIENDA', NULL, t.id
FROM tienda t
WHERE NOT EXISTS (
    SELECT 1
    FROM nodo n
    WHERE n.tienda_id = t.id
      AND n.tipo = 'BODEGA_TIENDA'
);
