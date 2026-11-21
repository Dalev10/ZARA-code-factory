-- ============================================================================
-- V39__viaje_estados_y_paradas.sql
-- HU-47 (Sprint 3): armar un viaje con varias paradas. HU-48 usa los mismos
-- campos para la salida y la llegada (sin migración propia).
--
-- viaje
-- * estado ARMADO -> EN_CURSO (salida) -> CERRADO (última llegada). Con
--   default ARMADO: un viaje insertado sin estado está recién armado.
-- * version: bloqueo optimista. Dos llegadas simultáneas a paradas distintas
--   tocan el mismo viaje y una reintenta; así ninguna deja de cerrarlo.
-- * fecha_creacion para ordenar el listado.
-- * Coherencia: fuera de ARMADO hay fecha de salida; solo CERRADO tiene cierre.
--
-- viaje_parada
-- * UNIQUE (ciclo_despacho_id): un ciclo va en un solo viaje (409).
-- * UNIQUE (viaje_id, orden_parada) y orden_parada > 0.
--
-- Fechas a TIMESTAMPTZ (Hibernate trabaja en UTC).
-- Es V39 y no V36, como proponía el plan: siguiente número libre tras V38.
-- Las tablas vienen de V1 y nada las escribe antes; los default cubren
-- cualquier fila sin necesidad de inventar datos.
-- ============================================================================

ALTER TABLE viaje
    ALTER COLUMN fecha_salida TYPE TIMESTAMPTZ USING fecha_salida AT TIME ZONE 'UTC',
    ALTER COLUMN fecha_cierre TYPE TIMESTAMPTZ USING fecha_cierre AT TIME ZONE 'UTC',
    ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'ARMADO'
        CHECK (estado IN ('ARMADO', 'EN_CURSO', 'CERRADO')),
    ADD COLUMN fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_viaje_salida_segun_estado CHECK (estado = 'ARMADO' OR fecha_salida IS NOT NULL),
    ADD CONSTRAINT chk_viaje_cierre_segun_estado CHECK ((estado = 'CERRADO') = (fecha_cierre IS NOT NULL));

ALTER TABLE viaje_parada
    ALTER COLUMN timestamp_salida_cd TYPE TIMESTAMPTZ USING timestamp_salida_cd AT TIME ZONE 'UTC',
    ALTER COLUMN timestamp_llegada_tienda TYPE TIMESTAMPTZ USING timestamp_llegada_tienda AT TIME ZONE 'UTC',
    ADD CONSTRAINT uq_viaje_parada_ciclo UNIQUE (ciclo_despacho_id),
    ADD CONSTRAINT uq_viaje_parada_orden UNIQUE (viaje_id, orden_parada),
    ADD CONSTRAINT chk_viaje_parada_orden_positivo CHECK (orden_parada > 0);

CREATE INDEX ix_viaje_estado ON viaje (estado);
