-- Completa la tabla preexistente de demanda perdida con idempotencia y auditoría de HU-34/35.
ALTER TABLE solicitud_no_satisfecha
    ALTER COLUMN timestamp_solicitud TYPE TIMESTAMP WITH TIME ZONE
        USING timestamp_solicitud AT TIME ZONE 'UTC',
    ADD COLUMN cantidad INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN stock_sistema INTEGER,
    ADD COLUMN registrado_por_id UUID REFERENCES usuario(id),
    ADD COLUMN clave_idempotencia VARCHAR(100),
    ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    ADD COLUMN anulada_por UUID REFERENCES usuario(id),
    ADD COLUMN anulada_en TIMESTAMP WITH TIME ZONE;

UPDATE solicitud_no_satisfecha
SET registrado_por_id = CASE
        WHEN registrado_por ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
            THEN registrado_por::UUID
        ELSE NULL
    END,
    clave_idempotencia = 'legacy-' || id::TEXT;

ALTER TABLE solicitud_no_satisfecha
    ALTER COLUMN clave_idempotencia SET NOT NULL,
    ADD CONSTRAINT uq_solicitud_no_satisfecha_clave UNIQUE (clave_idempotencia),
    ADD CONSTRAINT chk_solicitud_no_satisfecha_cantidad CHECK (cantidad > 0),
    ADD CONSTRAINT chk_solicitud_no_satisfecha_estado CHECK (estado IN ('ACTIVA', 'ANULADA')),
    ADD CONSTRAINT chk_solicitud_no_satisfecha_anulacion CHECK (
        (estado = 'ACTIVA' AND anulada_por IS NULL AND anulada_en IS NULL)
        OR (estado = 'ANULADA' AND anulada_por IS NOT NULL AND anulada_en IS NOT NULL)
    );
