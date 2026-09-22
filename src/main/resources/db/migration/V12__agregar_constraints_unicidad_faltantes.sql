-- ============================================================================
-- V12__agregar_constraints_unicidad_faltantes.sql
-- HU-21: las reglas de "sin duplicados" para CentroDistribucion, Categoria y
-- Nodo (una BodegaTienda/Almacén por tienda) se validaban solo con un
-- check-then-act desde la capa de aplicación (SELECT y después INSERT), sin
-- ningún constraint de base de datos detrás. Bajo dos requests concurrentes
-- eso permite colar duplicados. Tienda y Variante ya tenían su UNIQUE
-- correspondiente (V1); esta migración cierra la brecha para el resto.
-- ============================================================================

ALTER TABLE cd
    ADD CONSTRAINT uq_cd_nombre UNIQUE (nombre);

ALTER TABLE categoria
    ADD CONSTRAINT uq_categoria_nombre UNIQUE (nombre);

-- Parcial (solo tienda_id IS NOT NULL) porque los nodos de tipo CD siempre
-- tienen tienda_id NULL y no participan de esta regla — cubre tanto
-- "una BodegaTienda por tienda" como "un Almacén por tienda".
CREATE UNIQUE INDEX ux_nodo_tienda_tipo ON nodo (tienda_id, tipo) WHERE tienda_id IS NOT NULL;
