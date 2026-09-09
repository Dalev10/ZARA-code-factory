-- ============================================================================
-- V1__crear_esquema_inicial.sql
-- Esquema inicial derivado del ERD de arquitectura del MVP de cadena de
-- suministro retail (informe_final_arquitectura_mvp.md /
-- erd_cadena_suministro_mvp.mermaid).
--
-- Alcance de esta migración: SOLO las entidades del dominio de negocio.
-- El esquema del módulo "identity" (usuarios/autenticación) queda
-- deliberadamente fuera — se agrega en una migración futura cuando ese
-- módulo se diseñe en detalle.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Catálogo
-- ----------------------------------------------------------------------------
CREATE TABLE categoria (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL
);

CREATE TABLE template (
    id            BIGSERIAL PRIMARY KEY,
    categoria_id  BIGINT REFERENCES categoria(id),
    nombre        VARCHAR(200) NOT NULL,
    temporada     VARCHAR(50),
    proveedor     VARCHAR(150),
    precio_base   NUMERIC(12,2)
);

CREATE TABLE variante (
    id           BIGSERIAL PRIMARY KEY,
    template_id  BIGINT NOT NULL REFERENCES template(id),
    talla        VARCHAR(20),
    color        VARCHAR(50),
    sku          VARCHAR(60) NOT NULL UNIQUE
);

-- ----------------------------------------------------------------------------
-- Nodos: Centros de Distribución, Tiendas, y el nodo polimórfico
-- ----------------------------------------------------------------------------
CREATE TABLE cd (
    id        BIGSERIAL PRIMARY KEY,
    nombre    VARCHAR(150) NOT NULL,
    ubicacion VARCHAR(255)
);

CREATE TABLE tienda (
    id        BIGSERIAL PRIMARY KEY,
    nombre    VARCHAR(150) NOT NULL,
    ubicacion VARCHAR(255)
);

-- "Nodo" resuelve el polimorfismo de Inventario con integridad referencial
-- real: cada fila representa exactamente un CD, o una Bodega_Tienda/Almacén
-- de una tienda específica.
CREATE TABLE nodo (
    id        BIGSERIAL PRIMARY KEY,
    tipo      VARCHAR(20) NOT NULL CHECK (tipo IN ('CD', 'BODEGA_TIENDA', 'ALMACEN')),
    cd_id     BIGINT REFERENCES cd(id),
    tienda_id BIGINT REFERENCES tienda(id),
    CONSTRAINT chk_nodo_referencia_coherente CHECK (
        (tipo = 'CD' AND cd_id IS NOT NULL AND tienda_id IS NULL)
        OR
        (tipo IN ('BODEGA_TIENDA', 'ALMACEN') AND tienda_id IS NOT NULL AND cd_id IS NULL)
    )
);

CREATE TABLE tienda_cd_prioridad (
    id               BIGSERIAL PRIMARY KEY,
    tienda_id        BIGINT NOT NULL REFERENCES tienda(id),
    cd_id            BIGINT NOT NULL REFERENCES cd(id),
    orden_prioridad  INT NOT NULL,
    UNIQUE (tienda_id, cd_id),
    UNIQUE (tienda_id, orden_prioridad)
);

-- ----------------------------------------------------------------------------
-- Inventario (polimórfico sobre Nodo)
-- ----------------------------------------------------------------------------
CREATE TABLE inventario (
    id                    BIGSERIAL PRIMARY KEY,
    nodo_id               BIGINT NOT NULL REFERENCES nodo(id),
    variante_id           BIGINT NOT NULL REFERENCES variante(id),
    a_la_mano             INT NOT NULL DEFAULT 0,
    disponible_para_uso   INT NOT NULL DEFAULT 0,
    entrante              INT NOT NULL DEFAULT 0,
    saliente              INT NOT NULL DEFAULT 0,
    pronosticado          INT NOT NULL DEFAULT 0,
    version               INT NOT NULL DEFAULT 0, -- bloqueo optimista (ver NFR de escalabilidad)
    fecha_actualizacion   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (nodo_id, variante_id)
);

-- ----------------------------------------------------------------------------
-- Ventas y demanda perdida (dos métricas separadas, nunca sumables)
-- ----------------------------------------------------------------------------
CREATE TABLE venta (
    id               BIGSERIAL PRIMARY KEY,
    variante_id      BIGINT NOT NULL REFERENCES variante(id),
    tienda_id        BIGINT NOT NULL REFERENCES tienda(id),
    cantidad         INT NOT NULL,
    precio           NUMERIC(12,2) NOT NULL,
    canal            VARCHAR(30) NOT NULL DEFAULT 'FISICO',
    timestamp_venta  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE solicitud_no_satisfecha (
    id                    BIGSERIAL PRIMARY KEY,
    variante_id           BIGINT NOT NULL REFERENCES variante(id),
    tienda_id             BIGINT NOT NULL REFERENCES tienda(id),
    timestamp_solicitud   TIMESTAMP NOT NULL DEFAULT now(),
    registrado_por        VARCHAR(150)
);

CREATE TABLE ventana_quiebre (
    id            BIGSERIAL PRIMARY KEY,
    variante_id   BIGINT NOT NULL REFERENCES variante(id),
    tienda_id     BIGINT NOT NULL REFERENCES tienda(id),
    fecha_inicio  TIMESTAMP NOT NULL,
    fecha_fin     TIMESTAMP -- NULL = ventana todavía abierta
);

-- Búsqueda frecuente: "¿qué ventanas de quiebre siguen abiertas?"
CREATE INDEX idx_ventana_quiebre_abiertas ON ventana_quiebre (variante_id, tienda_id)
    WHERE fecha_fin IS NULL;

-- ----------------------------------------------------------------------------
-- Reposición interna (Bodega_Tienda -> Almacén)
-- ----------------------------------------------------------------------------
CREATE TABLE umbral_reposicion_interna (
    id               BIGSERIAL PRIMARY KEY,
    variante_id      BIGINT REFERENCES variante(id),
    categoria_id     BIGINT REFERENCES categoria(id),
    cantidad_minima  INT NOT NULL,
    CONSTRAINT chk_umbral_tiene_alcance CHECK (
        variante_id IS NOT NULL OR categoria_id IS NOT NULL
    )
);

CREATE TABLE traspaso_interno (
    id                  BIGSERIAL PRIMARY KEY,
    variante_id         BIGINT NOT NULL REFERENCES variante(id),
    tienda_id           BIGINT NOT NULL REFERENCES tienda(id),
    cantidad            INT NOT NULL,
    timestamp_traspaso  TIMESTAMP NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- Reposición (recomendación) y Órdenes de Reabastecimiento
-- ----------------------------------------------------------------------------
CREATE TABLE recomendacion (
    id                  BIGSERIAL PRIMARY KEY,
    variante_id         BIGINT NOT NULL REFERENCES variante(id),
    tienda_id           BIGINT NOT NULL REFERENCES tienda(id),
    cd_sugerido_id      BIGINT REFERENCES cd(id), -- sugerencia blanda, no valida stock
    cantidad_sugerida   INT NOT NULL,
    cantidad_aprobada   INT, -- puede diferir de la sugerida (ver NFR de mantenibilidad)
    estado              VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                          CHECK (estado IN ('PENDIENTE', 'APROBADA', 'DESCARTADA')),
    aprobado_por        VARCHAR(150),
    fecha_aprobacion    TIMESTAMP
);

CREATE TABLE orden_reabastecimiento (
    id                          BIGSERIAL PRIMARY KEY,
    recomendacion_id            BIGINT REFERENCES recomendacion(id),
    tienda_id                   BIGINT NOT NULL REFERENCES tienda(id),
    cd_asignado_id              BIGINT REFERENCES cd(id), -- resultado final del fallback
    motivo_asignacion           VARCHAR(255),
    cantidad_total_solicitada   INT NOT NULL,
    cantidad_pendiente          INT NOT NULL,
    estado                      VARCHAR(30) NOT NULL DEFAULT 'GENERADA'
                                  CHECK (estado IN (
                                      'GENERADA', 'APROBADA', 'EN_DESPACHO', 'DESPACHADA',
                                      'EN_TRANSITO', 'RECIBIDA_TOTAL', 'RECIBIDA_PARCIAL',
                                      'REABIERTA', 'CERRADA'
                                  )),
    fecha_creacion              TIMESTAMP NOT NULL DEFAULT now()
);

-- Trazabilidad del fallback de CD (Opción B: prioridad con fallback dinámico)
CREATE TABLE intento_cd (
    id                BIGSERIAL PRIMARY KEY,
    orden_id          BIGINT NOT NULL REFERENCES orden_reabastecimiento(id),
    cd_id             BIGINT NOT NULL REFERENCES cd(id),
    orden_evaluado    INT NOT NULL, -- posición en la lista de prioridad probada
    resultado         VARCHAR(20) NOT NULL CHECK (resultado IN ('SIN_STOCK', 'ASIGNADO')),
    timestamp_intento TIMESTAMP NOT NULL DEFAULT now()
);

-- Soporta "Recibida (parcial) -> Reabierta": una orden, múltiples ciclos
CREATE TABLE ciclo_despacho (
    id                    BIGSERIAL PRIMARY KEY,
    orden_id              BIGINT NOT NULL REFERENCES orden_reabastecimiento(id),
    numero_ciclo          INT NOT NULL,
    cantidad_despachada   INT NOT NULL,
    fecha_despacho        TIMESTAMP NOT NULL DEFAULT now(),
    resultado             VARCHAR(20) NOT NULL CHECK (resultado IN ('COMPLETO', 'PARCIAL')),
    UNIQUE (orden_id, numero_ciclo)
);

-- ----------------------------------------------------------------------------
-- Envío / Transporte (flota propia, multi-parada, tracking manual)
-- ----------------------------------------------------------------------------
CREATE TABLE viaje (
    id            BIGSERIAL PRIMARY KEY,
    cd_id         BIGINT NOT NULL REFERENCES cd(id),
    fecha_salida  TIMESTAMP,
    fecha_cierre  TIMESTAMP,
    vehiculo      VARCHAR(100)
);

CREATE TABLE viaje_parada (
    id                          BIGSERIAL PRIMARY KEY,
    viaje_id                    BIGINT NOT NULL REFERENCES viaje(id),
    ciclo_despacho_id           BIGINT NOT NULL REFERENCES ciclo_despacho(id),
    tienda_id                   BIGINT NOT NULL REFERENCES tienda(id),
    orden_parada                INT NOT NULL,
    timestamp_salida_cd         TIMESTAMP,
    timestamp_llegada_tienda    TIMESTAMP, -- sin checkpoints intermedios (MVP)
    estado                      VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                                  CHECK (estado IN ('PENDIENTE', 'EN_TRANSITO', 'ENTREGADO'))
);

CREATE TABLE recepcion (
    id                    BIGSERIAL PRIMARY KEY,
    viaje_parada_id       BIGINT NOT NULL REFERENCES viaje_parada(id),
    ciclo_despacho_id     BIGINT NOT NULL REFERENCES ciclo_despacho(id),
    cantidad_recibida     INT NOT NULL,
    discrepancia          INT NOT NULL DEFAULT 0, -- cantidad_despachada - cantidad_recibida
    timestamp_recepcion   TIMESTAMP NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- Índices de apoyo para las consultas operativas más frecuentes
-- ----------------------------------------------------------------------------
CREATE INDEX idx_venta_tienda_variante_fecha ON venta (tienda_id, variante_id, timestamp_venta);
CREATE INDEX idx_inventario_variante ON inventario (variante_id);
