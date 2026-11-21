-- ============================================================================
-- V36__recepcion_discrepancias.sql
-- HU-49 (Sprint 3): recepción de una parada en la Bodega_Tienda.
--
-- * Una sola recepción por parada: el UNIQUE da la idempotencia (D-91) y
--   serializa dos registros simultáneos de la misma parada.
-- * 0 <= cantidad_recibida y discrepancia >= 0 (discrepancia = despachada -
--   recibida, pérdida en tránsito sin kardex, D-90). El tope recibida <=
--   despachada lo valida el dominio: la despachada vive en ciclo_despacho.
-- * registrado_por: el usuario sale del token, como en venta y en
--   solicitud_no_satisfecha.
-- * timestamp_recepcion pasa a TIMESTAMPTZ (Hibernate trabaja en UTC).
--
-- Es V36 y no V37, como proponía el plan: V30 y V31 no existen, HD-04 tomó la
-- V35 y Flyway rechaza una versión menor que llega después de una mayor.
--
-- La tabla viene de V1 y ninguna versión anterior la escribe. Si tuviera filas,
-- registrado_por NOT NULL no tendría con qué llenarse: se detiene con un error
-- explícito en lugar de inventar un autor.
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM recepcion) THEN
        RAISE EXCEPTION 'V36: la tabla recepcion tiene filas sin autor; hay que tratarlas antes de migrar';
    END IF;
END
$$;

ALTER TABLE recepcion
    ALTER COLUMN timestamp_recepcion TYPE TIMESTAMPTZ USING timestamp_recepcion AT TIME ZONE 'UTC',
    ADD COLUMN registrado_por UUID NOT NULL REFERENCES usuario(id),
    ADD CONSTRAINT uq_recepcion_viaje_parada UNIQUE (viaje_parada_id),
    ADD CONSTRAINT chk_recepcion_cantidad_no_negativa CHECK (cantidad_recibida >= 0),
    ADD CONSTRAINT chk_recepcion_discrepancia_no_negativa CHECK (discrepancia >= 0);
