-- ============================================================================
-- V23__prioridad_tienda_cd.sql
-- HU-42: prioridad de CDs para cada tienda.
--
--       cada tienda tiene su propio orden (UNIQUE(tienda_id, orden_prioridad)).
--       reordenar en un PUT con la lista completa viola el UNIQUE si Hibernate
--       inserta antes de borrar, por eso el UNIQUE es DEFERRABLE INITIALLY DEFERRED:
--       Postgres verifica la unicidad al commit y no en cada INSERT.
--       Bloquear eliminación de un CD con prioridades asignadas (409 explícito).
-- ============================================================================

-- El UNIQUE(tienda_id, cd_id) de V1 no es deferrable; se reemplaza.
ALTER TABLE tienda_cd_prioridad
    DROP CONSTRAINT IF EXISTS tienda_cd_prioridad_tienda_id_cd_id_key;

ALTER TABLE tienda_cd_prioridad
    DROP CONSTRAINT IF EXISTS tienda_cd_prioridad_tienda_id_orden_prioridad_key;

ALTER TABLE tienda_cd_prioridad
    ADD CONSTRAINT uq_prioridad_tienda_cd
        UNIQUE (tienda_id, cd_id)
        DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE tienda_cd_prioridad
    ADD CONSTRAINT uq_prioridad_tienda_orden
        UNIQUE (tienda_id, orden_prioridad)
        DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE tienda_cd_prioridad
    ADD CONSTRAINT chk_prioridad_orden_positivo
        CHECK (orden_prioridad > 0);
