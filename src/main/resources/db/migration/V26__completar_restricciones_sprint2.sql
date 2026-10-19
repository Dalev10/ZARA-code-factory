-- ============================================================================
-- V26__completar_restricciones_sprint2.sql
-- Corrección de integración del Sprint 2: restricciones del plan que no
-- quedaron en V18, V19 y V22.
--
-- Las tablas venta y solicitud_no_satisfecha existen desde V1 y pueden tener
-- filas históricas en producción. Sus CHECK se crean NOT VALID: la base los
-- exige a toda fila nueva o modificada sin fallar el despliegue por datos
-- previos. Para exigirlos también al histórico, revisar las filas que no
-- cumplen y luego ejecutar ALTER TABLE ... VALIDATE CONSTRAINT <nombre>.
-- ============================================================================

-- D-36 / D-37 (HU-40): cantidad positiva y precio no negativo.
ALTER TABLE venta
    ADD CONSTRAINT chk_venta_cantidad_positiva CHECK (cantidad > 0) NOT VALID,
    ADD CONSTRAINT chk_venta_precio_no_negativo CHECK (precio >= 0) NOT VALID;

-- D-03 (HU-43): toda solicitud nueva registra a su autor. registrado_por_id
-- queda NULL en las filas históricas cuyo texto no era un UUID (V19).
ALTER TABLE solicitud_no_satisfecha
    ADD CONSTRAINT chk_solicitud_registrado_por CHECK (registrado_por_id IS NOT NULL) NOT VALID;

-- D-27: las tablas del Sprint 2 guardan fechas con zona horaria.
ALTER TABLE traspaso_interno
    ALTER COLUMN timestamp_traspaso TYPE TIMESTAMP WITH TIME ZONE
        USING timestamp_traspaso AT TIME ZONE 'UTC';
