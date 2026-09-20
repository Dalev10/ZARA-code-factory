-- ============================================================================
-- V11__sembrar_scopes_catalogo_y_cd.sql
-- HU-20: Catálogo (FEAT-05) y CentroDistribucion (FEAT-04) quedaban abiertos
-- a cualquier usuario autenticado, sin autorización granular — a diferencia
-- de Tienda/BodegaTienda (tiendas:administrar). Se agrega un scope por
-- módulo, siguiendo el mismo patrón que V10.
-- ============================================================================

INSERT INTO scope (codigo, descripcion, sensible)
VALUES ('catalogo:administrar', 'CRUD de categorías, templates y variantes del catálogo', true);

INSERT INTO scope (codigo, descripcion, sensible)
VALUES ('cd:administrar', 'CRUD de centros de distribución', true);

INSERT INTO rol_scope (rol_id, scope_id)
SELECT r.id, s.id
FROM rol r, scope s
WHERE r.nombre = 'ADMIN'
  AND s.codigo IN ('catalogo:administrar', 'cd:administrar');
