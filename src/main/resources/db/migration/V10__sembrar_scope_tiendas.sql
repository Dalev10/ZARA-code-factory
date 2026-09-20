-- ============================================================================
-- V10__sembrar_scope_tiendas.sql
-- FEAT-02 (Gestión de Tienda/Almacén) y su corrección cruzada sobre
-- BodegaTienda (FEAT-03) comparten el mismo scope administrativo, dado que
-- ambas viven bajo la misma responsabilidad operativa.
-- ============================================================================

INSERT INTO scope (codigo, descripcion, sensible)
VALUES ('tiendas:administrar', 'CRUD de tiendas y gestión de sus bodegas asociadas', true);

INSERT INTO rol_scope (rol_id, scope_id)
SELECT r.id, s.id
FROM rol r, scope s
WHERE r.nombre = 'ADMIN'
  AND s.codigo = 'tiendas:administrar';
