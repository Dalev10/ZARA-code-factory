-- ============================================================================
-- V34__sembrar_scopes_y_roles_abastecimiento.sql
-- HD-03 (Sprint 3): scopes y roles del abastecimiento CD → Bodega_Tienda
-- (decisión D-94 y sección 4.5 del plan de trabajo del Sprint 3).
--
-- - 4 scopes nuevos. Son sensibles todos salvo el de registrar (misma regla de
--   D-61 que en V14).
-- - ADMIN recibe los 4. OPERADOR_CD suma despacho:operar.
-- - 3 roles nuevos: PLANIFICADOR_ABASTECIMIENTO, COORDINADOR_TRANSPORTE y
--   ENCARGADO_BODEGA. Sus nombres quedan reservados.
--
-- Es V34 y no V30, como proponía el plan: V32 y V33 ya están aplicadas y Flyway
-- rechaza una versión menor que llega después.
--
-- INSERT simple a propósito (igual que V14): si alguno de estos nombres ya
-- existiera en la base, la migración falla de forma visible.
-- ============================================================================

INSERT INTO scope (codigo, descripcion, sensible)
VALUES
    ('reposicion:planificar', 'Evaluar, aprobar y rechazar recomendaciones; ver y gestionar órdenes de reabastecimiento', true),
    ('despacho:operar', 'Despachar órdenes de reabastecimiento desde el CD y ver sus ciclos de despacho', true),
    ('transporte:operar', 'Armar viajes y registrar su salida y su llegada a cada tienda', true),
    ('recepcion:registrar', 'Registrar recepciones en la Bodega_Tienda y ver las paradas por recibir', false);

INSERT INTO rol (nombre, descripcion)
VALUES
    ('PLANIFICADOR_ABASTECIMIENTO', 'Decide el abastecimiento desde los CD: recomendaciones, órdenes y niveles de reposición'),
    ('COORDINADOR_TRANSPORTE', 'Arma los viajes y registra su salida y su llegada'),
    ('ENCARGADO_BODEGA', 'Recibe la mercancía en la Bodega_Tienda de sus tiendas asignadas');

INSERT INTO rol_scope (rol_id, scope_id)
SELECT r.id, s.id
FROM rol r, scope s
WHERE r.nombre = 'ADMIN'
  AND s.codigo IN ('reposicion:planificar', 'despacho:operar', 'transporte:operar', 'recepcion:registrar');

INSERT INTO rol_scope (rol_id, scope_id)
SELECT r.id, s.id
FROM rol r, scope s
WHERE r.nombre = 'OPERADOR_CD'
  AND s.codigo = 'despacho:operar';

INSERT INTO rol_scope (rol_id, scope_id)
SELECT r.id, s.id
FROM rol r, scope s
WHERE r.nombre = 'PLANIFICADOR_ABASTECIMIENTO'
  AND s.codigo IN ('reposicion:planificar', 'reposicion:administrar', 'inventario:consultar');

INSERT INTO rol_scope (rol_id, scope_id)
SELECT r.id, s.id
FROM rol r, scope s
WHERE r.nombre = 'COORDINADOR_TRANSPORTE'
  AND s.codigo IN ('transporte:operar', 'inventario:consultar');

INSERT INTO rol_scope (rol_id, scope_id)
SELECT r.id, s.id
FROM rol r, scope s
WHERE r.nombre = 'ENCARGADO_BODEGA'
  AND s.codigo IN ('recepcion:registrar', 'inventario:consultar');
