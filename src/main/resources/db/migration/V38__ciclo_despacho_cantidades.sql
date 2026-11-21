-- ============================================================================
-- V38__ciclo_despacho_cantidades.sql
-- HU-46 (Sprint 3): despachar una orden con la disponibilidad real del CD.
--
-- * cantidad_solicitada: lo que pedía el ciclo (el pendiente de la orden al
--   despachar). Con ella se calculan el pendiente del ciclo y el fill rate
--   (despachada / solicitada); por eso no se guardan aparte.
-- * 0 < despachada <= solicitada: un despacho sin stock no deja ciclo (409).
-- * resultado coherente con las cantidades: COMPLETO solo si salió todo.
-- * fecha_despacho pasa a TIMESTAMPTZ (Hibernate trabaja en UTC). HU-48 la usa
--   para rechazar una salida del viaje anterior al despacho.
--
-- Es V38 y no V35, como proponía el plan: la convención del equipo es tomar el
-- siguiente número libre (V36 es HU-49 y V37 es HU-44, que entran antes).
--
-- La tabla viene de V1 y ninguna versión anterior la escribe. Si tuviera filas,
-- cantidad_solicitada NOT NULL no tendría con qué llenarse: se detiene con un
-- error explícito en lugar de inventar un valor.
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM ciclo_despacho) THEN
        RAISE EXCEPTION 'V38: la tabla ciclo_despacho tiene filas sin cantidad solicitada; hay que tratarlas antes de migrar';
    END IF;
END
$$;

ALTER TABLE ciclo_despacho
    ALTER COLUMN fecha_despacho TYPE TIMESTAMPTZ USING fecha_despacho AT TIME ZONE 'UTC',
    ADD COLUMN cantidad_solicitada INT NOT NULL,
    ADD CONSTRAINT chk_ciclo_numero_positivo CHECK (numero_ciclo > 0),
    ADD CONSTRAINT chk_ciclo_cantidades CHECK (cantidad_despachada > 0 AND cantidad_despachada <= cantidad_solicitada),
    ADD CONSTRAINT chk_ciclo_resultado_coherente
        CHECK ((resultado = 'COMPLETO') = (cantidad_despachada = cantidad_solicitada));
