-- ============================================================================
-- V8__sembrar_scopes_administrativos.sql
-- HU-11 reemplaza el chequeo provisorio "solo ADMIN" (AutorizacionAdmin, HU-09)
-- por el guard genérico de autorización por scope. Los endpoints de gestión de
-- Usuario, Rol y Scope ahora exigen estos tres scopes en vez de un rol
-- hardcodeado; se los asigna acá al rol ADMIN semilla para que siga
-- funcionando sin intervención manual.
-- ============================================================================

INSERT INTO scope (codigo, descripcion, sensible)
VALUES
    ('usuarios:administrar', 'Listar usuarios y gestionar sus roles asignados', true),
    ('roles:administrar', 'CRUD de roles y gestión de sus scopes asignados', true),
    ('scopes:administrar', 'CRUD de scopes', true);

INSERT INTO rol_scope (rol_id, scope_id)
SELECT r.id, s.id
FROM rol r, scope s
WHERE r.nombre = 'ADMIN'
  AND s.codigo IN ('usuarios:administrar', 'roles:administrar', 'scopes:administrar');
