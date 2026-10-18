-- HU-36/37: motivo de cierre obligatorio y una sola ventana abierta por tienda/variante.
-- No se resuelven automáticamente datos incompatibles con las nuevas restricciones.
-- La prevalidación falla antes de cualquier DDL o backfill y enumera los registros
-- que requieren una decisión explícita basada en el historial de negocio.
LOCK TABLE ventana_quiebre IN ACCESS EXCLUSIVE MODE;

DO $$
DECLARE
    duplicados TEXT;
    fechas_invalidas TEXT;
BEGIN
    SELECT string_agg(
               format('tienda=%s variante=%s ventanas=[%s]',
                      tienda_id, variante_id, ids),
               E'\n'
           )
    INTO duplicados
    FROM (
        SELECT tienda_id, variante_id, string_agg(id::TEXT, ', ' ORDER BY id) AS ids
        FROM ventana_quiebre
        WHERE fecha_fin IS NULL
        GROUP BY tienda_id, variante_id
        HAVING count(*) > 1
        ORDER BY tienda_id, variante_id
        LIMIT 50
    ) conflictos;

    SELECT string_agg(
               format('id=%s inicio=%s fin=%s', id, fecha_inicio, fecha_fin),
               E'\n'
           )
    INTO fechas_invalidas
    FROM (
        SELECT id, fecha_inicio, fecha_fin
        FROM ventana_quiebre
        WHERE fecha_fin < fecha_inicio
        ORDER BY id
        LIMIT 50
    ) conflictos;

    IF duplicados IS NOT NULL OR fechas_invalidas IS NOT NULL THEN
        RAISE EXCEPTION USING
            MESSAGE = 'V20 no aplicada: hay ventanas históricas incompatibles con las nuevas restricciones.',
            DETAIL = format(
                'Duplicados abiertos (máximo 50 grupos):%s%sFechas inválidas (máximo 50 filas):%s%s',
                E'\n', coalesce(duplicados, ' ninguno'),
                E'\n', coalesce(fechas_invalidas, ' ninguna')
            ),
            HINT = 'Inspeccione los datos con el equipo de negocio y corrija explícitamente cada registro con evidencia. No borre ni cierre ventanas automáticamente; vuelva a ejecutar Flyway después de resolver los conflictos.';
    END IF;
END $$;

ALTER TABLE ventana_quiebre
    ALTER COLUMN fecha_inicio TYPE TIMESTAMP WITH TIME ZONE
        USING fecha_inicio AT TIME ZONE 'UTC',
    ALTER COLUMN fecha_fin TYPE TIMESTAMP WITH TIME ZONE
        USING fecha_fin AT TIME ZONE 'UTC',
    ADD COLUMN motivo_cierre VARCHAR(30);

-- El esquema anterior no guardaba el motivo. Clasificar como desconocido preserva
-- esa falta de información sin afirmar que hubo reposición.
UPDATE ventana_quiebre
SET motivo_cierre = 'HISTORICO_NO_CLASIFICADO'
WHERE fecha_fin IS NOT NULL;

ALTER TABLE ventana_quiebre
    ADD CONSTRAINT chk_ventana_quiebre_motivo_cierre CHECK (
        motivo_cierre IS NULL OR motivo_cierre IN (
            'REPOSICION', 'VENTA_FORZADA', 'TIENDA_DESACTIVADA', 'HISTORICO_NO_CLASIFICADO'
        )
    ),
    ADD CONSTRAINT chk_ventana_quiebre_cierre_coherente CHECK (
        (fecha_fin IS NULL AND motivo_cierre IS NULL)
        OR (fecha_fin IS NOT NULL AND motivo_cierre IS NOT NULL)
    ),
    ADD CONSTRAINT chk_ventana_quiebre_fechas CHECK (
        fecha_fin IS NULL OR fecha_fin >= fecha_inicio
    );

DROP INDEX IF EXISTS idx_ventana_quiebre_abiertas;
CREATE UNIQUE INDEX uq_ventana_quiebre_abierta
    ON ventana_quiebre (variante_id, tienda_id)
    WHERE fecha_fin IS NULL;
