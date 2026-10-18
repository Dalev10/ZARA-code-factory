-- HU-31 a HU-33: ventas por transacción, vendedor y estado de anulación.
ALTER TABLE venta
    ALTER COLUMN timestamp_venta TYPE TIMESTAMP WITH TIME ZONE
        USING timestamp_venta AT TIME ZONE 'UTC',
    ADD COLUMN transaccion_id UUID,
    ADD COLUMN origen VARCHAR(20) NOT NULL DEFAULT 'IMPORTACION',
    ADD COLUMN vendedor_id UUID REFERENCES usuario(id),
    ADD COLUMN forzada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'REGISTRADA',
    ADD COLUMN anulada_por UUID REFERENCES usuario(id),
    ADD COLUMN anulada_en TIMESTAMP WITH TIME ZONE,
    ADD COLUMN motivo_anulacion VARCHAR(500),
    ADD CONSTRAINT uq_venta_transaccion_variante UNIQUE (transaccion_id, variante_id),
    ADD CONSTRAINT chk_venta_origen CHECK (origen IN ('API', 'IMPORTACION', 'INTEGRACION')),
    ADD CONSTRAINT chk_venta_estado CHECK (estado IN ('REGISTRADA', 'ANULADA')),
    ADD CONSTRAINT chk_venta_vendedor_api CHECK (origen <> 'API' OR vendedor_id IS NOT NULL),
    ADD CONSTRAINT chk_venta_auditoria_anulacion CHECK (
        (estado = 'REGISTRADA' AND anulada_por IS NULL AND anulada_en IS NULL AND motivo_anulacion IS NULL)
        OR
        (estado = 'ANULADA' AND anulada_por IS NOT NULL AND anulada_en IS NOT NULL
            AND motivo_anulacion IS NOT NULL AND btrim(motivo_anulacion) <> '')
    );
