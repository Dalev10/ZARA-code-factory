-- ============================================================================
-- V14__sembrar_scopes_y_roles_operativos.sql
-- HU-33: scopes y roles operativos del Sprint 2 (decisiones D-61 y D-62).
--
-- - 9 scopes nuevos. Son sensibles todos salvo los de consultar y registrar.
-- - ADMIN recibe los 9 (se suman a sus 6 scopes actuales).
-- - 3 roles operativos: VENDEDOR, SUPERVISOR_TIENDA y OPERADOR_CD. El vendedor
--   puede forzar una venta sin stock para no perderla: cada venta forzada queda
--   marcada y con autor.
--
-- INSERT simple a propósito (igual que V8, V10 y V11): si alguno de estos
-- nombres ya existiera en la base, la migración falla de forma visible en vez de
-- mezclar scopes en silencio sobre un rol creado a mano.
-- ============================================================================

INSERT INTO scope (codigo, descripcion, sensible)
VALUES
    ('tiendas:operar-todas', 'Operar en todas las tiendas sin necesidad de tenerlas asignadas', true),
    ('inventario:administrar', 'Carga inicial, ajustes y traspasos manuales en tiendas; reconciliación de ventanas de quiebre', true),
    ('inventario-cd:administrar', 'Carga inicial y ajustes de inventario en centros de distribución', true),
    ('inventario:consultar', 'Consultar stock, movimientos (kardex) y traspasos', false),
    ('ventas:registrar', 'Registrar ventas y solicitudes no satisfechas', false),
    ('ventas:forzar-sin-stock', 'Registrar una venta sin stock suficiente (queda marcada y con autor)', true),
    ('ventas:anular', 'Anular ventas y solicitudes no satisfechas', true),
    ('ventas:consultar', 'Consultar ventas, solicitudes no satisfechas y ventanas de quiebre', false),
    ('reposicion:administrar', 'Configurar umbrales de reposición interna y ejecutar la evaluación masiva', true);

INSERT INTO rol (nombre, descripcion)
VALUES
    ('VENDEDOR', 'Registra ventas y demanda no satisfecha en sus tiendas asignadas'),
    ('SUPERVISOR_TIENDA', 'Supervisa la operación de sus tiendas: ventas, anulaciones e inventario'),
    ('OPERADOR_CD', 'Gestiona el inventario de los centros de distribución');

INSERT INTO rol_scope (rol_id, scope_id)
SELECT r.id, s.id
FROM rol r, scope s
WHERE r.nombre = 'ADMIN'
  AND s.codigo IN ('tiendas:operar-todas', 'inventario:administrar', 'inventario-cd:administrar',
                   'inventario:consultar', 'ventas:registrar', 'ventas:forzar-sin-stock',
                   'ventas:anular', 'ventas:consultar', 'reposicion:administrar');

INSERT INTO rol_scope (rol_id, scope_id)
SELECT r.id, s.id
FROM rol r, scope s
WHERE r.nombre = 'VENDEDOR'
  AND s.codigo IN ('ventas:registrar', 'ventas:consultar', 'inventario:consultar', 'ventas:forzar-sin-stock');

INSERT INTO rol_scope (rol_id, scope_id)
SELECT r.id, s.id
FROM rol r, scope s
WHERE r.nombre = 'SUPERVISOR_TIENDA'
  AND s.codigo IN ('ventas:registrar', 'ventas:consultar', 'inventario:consultar', 'ventas:forzar-sin-stock',
                   'ventas:anular', 'inventario:administrar');

INSERT INTO rol_scope (rol_id, scope_id)
SELECT r.id, s.id
FROM rol r, scope s
WHERE r.nombre = 'OPERADOR_CD'
  AND s.codigo IN ('inventario-cd:administrar', 'inventario:consultar');
