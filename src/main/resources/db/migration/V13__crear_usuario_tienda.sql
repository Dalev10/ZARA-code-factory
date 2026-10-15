-- ============================================================================
-- V13__crear_usuario_tienda.sql
-- HU-32: asignación N:M de usuarios a tiendas (decisiones D-09, D-14, D-70).
-- Define en qué tiendas puede operar cada usuario; quien tiene el scope
-- "tiendas:operar-todas" opera en todas sin necesidad de asignaciones.
--
-- - Borrar un usuario borra sus asignaciones (mismo criterio que usuario_rol).
-- - Las tiendas nunca se borran (se desactivan), así que la FK hacia tienda no
--   necesita CASCADE. Una tienda inactiva puede tener asignaciones: la
--   asignación no habilita a operar, porque cada operación valida que la
--   tienda esté ACTIVA.
-- ============================================================================

CREATE TABLE usuario_tienda (
    usuario_id UUID NOT NULL
        REFERENCES usuario(id) ON DELETE CASCADE,

    tienda_id UUID NOT NULL
        REFERENCES tienda(id),

    PRIMARY KEY (usuario_id, tienda_id)
);
