-- ============================================================================
-- V27__eliminar_sin_categoria_vacia.sql
-- HU-35 (D-52): SIN_CATEGORIA solo debe existir donde hubo templates sin
-- categoría. V16 la creó siempre; aquí se elimina donde nadie la usa (ni
-- templates ni umbrales). Donde agrupa templates se conserva, y la aplicación
-- la protege como nombre reservado.
-- ============================================================================

DELETE FROM categoria c
WHERE c.nombre = 'SIN_CATEGORIA'
  AND NOT EXISTS (SELECT 1 FROM template t WHERE t.categoria_id = c.id)
  AND NOT EXISTS (SELECT 1 FROM umbral_reposicion_interna u WHERE u.categoria_id = c.id);
