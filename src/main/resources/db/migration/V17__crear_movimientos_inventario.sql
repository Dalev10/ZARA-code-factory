-- ============================================================================
-- V17__crear_movimientos_inventario.sql
-- HU-27: agregado Inventario y kardex de movimientos.
--
-- La carga inicial crea la fila de inventario desde la aplicación. Esta
-- migración no crea inventarios para variantes existentes.
-- ============================================================================

ALTER TABLE inventario
    ALTER COLUMN fecha_actualizacion TYPE TIMESTAMP WITH TIME ZONE
    USING fecha_actualizacion AT TIME ZONE 'UTC';

ALTER TABLE inventario
    ADD CONSTRAINT chk_inventario_disponible_coherente
        CHECK (disponible_para_uso = a_la_mano),
    ADD CONSTRAINT chk_inventario_pronosticado_coherente
        CHECK (pronosticado = a_la_mano + entrante - saliente),
    ADD CONSTRAINT chk_inventario_sin_entrante
        CHECK (entrante = 0),
    ADD CONSTRAINT chk_inventario_sin_saliente
        CHECK (saliente = 0);

CREATE TABLE movimiento_inventario (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nodo_id       UUID NOT NULL REFERENCES nodo(id),
    variante_id   UUID NOT NULL REFERENCES variante(id),
    tipo          VARCHAR(30) NOT NULL CHECK (
        tipo IN (
            'CARGA_INICIAL',
            'AJUSTE',
            'VENTA',
            'ANULACION_VENTA',
            'REPOSICION',
            'TRASPASO_SALIDA',
            'TRASPASO_ENTRADA'
        )
    ),
    -- D-30: la carga inicial en 0 registra "se contó y no había".
    cantidad      INT NOT NULL CHECK (cantidad <> 0 OR tipo = 'CARGA_INICIAL'),
    motivo        VARCHAR(100) CHECK (motivo IS NULL OR btrim(motivo) <> ''),
    comentario    VARCHAR(500),
    referencia_id UUID,
    usuario_id    UUID REFERENCES usuario(id),
    forzado       BOOLEAN NOT NULL DEFAULT FALSE,
    ocurrido_en   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_movimiento_inventario_forzado_solo_venta
        CHECK (NOT forzado OR tipo = 'VENTA')
);

CREATE INDEX idx_movimiento_inventario_nodo_variante_fecha
    ON movimiento_inventario (nodo_id, variante_id, ocurrido_en);

CREATE INDEX idx_movimiento_inventario_referencia
    ON movimiento_inventario (referencia_id);
