-- ============================================================================
-- V1__crear_esquema_inicial.sql
-- Esquema inicial derivado del ERD de arquitectura del MVP de cadena de
-- suministro retail (informe_arquitectura_y_plan_trabajo.pdf).
--
-- Alcance de esta migración: SOLO las entidades del dominio de negocio.
-- El esquema del módulo "identity" (usuarios/autenticación) queda
-- deliberadamente fuera — se agrega en una migración futura cuando ese
-- módulo se diseñe en detalle.
--
-- Reescrita (todavía sin salir de entornos de desarrollo, sin datos reales)
-- para unificar la estrategia de identificadores con el módulo identity:
-- UUID en lugar de BIGSERIAL en todo el dominio de negocio. Motivo:
-- defensa en profundidad ante fallas de autorización (un id secuencial
-- vuelve trivialmente enumerable cualquier endpoint mal protegido) y
-- porque este es el único momento del proyecto donde el costo de migrar
-- es prácticamente nulo (todavía no existe código ni datos sobre la
-- mayoría de estas tablas).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Catálogo
-- ----------------------------------------------------------------------------
CREATE TABLE categoria (
    id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(150) NOT NULL
);

CREATE TABLE template (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    categoria_id  UUID REFERENCES categoria(id),
    nombre        VARCHAR(200) NOT NULL,
    temporada     VARCHAR(50),
    proveedor     VARCHAR(150),
    precio_base   NUMERIC(12,2)
);

CREATE TABLE variante (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id  UUID NOT NULL REFERENCES template(id),
    talla        VARCHAR(20),
    color        VARCHAR(50),
    sku          VARCHAR(60) NOT NULL UNIQUE
);

-- ----------------------------------------------------------------------------
-- Nodos: Centros de Distribución, Tiendas, y el nodo polimórfico
-- ----------------------------------------------------------------------------
CREATE TABLE cd (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre    VARCHAR(150) NOT NULL,
    ubicacion VARCHAR(255)
);

-- estado/creado_en/actualizado_en anticipan FEAT-02 (Gestión de Tienda):
-- "eliminar" una tienda es desactivarla (estado), nunca un DELETE físico,
-- dado que venta/inventario/etc. la referencian sin ON DELETE CASCADE.
CREATE TABLE tienda (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre        VARCHAR(150) NOT NULL UNIQUE,
    ubicacion     VARCHAR(255),
    estado        VARCHAR(20) NOT NULL DEFAULT 'ACTIVA'
                    CHECK (estado IN ('ACTIVA', 'INACTIVA')),
    creado_en     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- "Nodo" resuelve el polimorfismo de Inventario con integridad referencial
-- real: cada fila representa exactamente un CD, o una Bodega_Tienda/Almacén
-- de una tienda específica.
CREATE TABLE nodo (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo      VARCHAR(20) NOT NULL CHECK (tipo IN ('CD', 'BODEGA_TIENDA', 'ALMACEN')),
    cd_id     UUID REFERENCES cd(id),
    tienda_id UUID REFERENCES tienda(id),
    CONSTRAINT chk_nodo_referencia_coherente CHECK (
        (tipo = 'CD' AND cd_id IS NOT NULL AND tienda_id IS NULL)
        OR
        (tipo IN ('BODEGA_TIENDA', 'ALMACEN') AND tienda_id IS NOT NULL AND cd_id IS NULL)
    )
);

CREATE TABLE tienda_cd_prioridad (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tienda_id        UUID NOT NULL REFERENCES tienda(id),
    cd_id            UUID NOT NULL REFERENCES cd(id),
    orden_prioridad  INT NOT NULL,
    UNIQUE (tienda_id, cd_id),
    UNIQUE (tienda_id, orden_prioridad)
);

-- ----------------------------------------------------------------------------
-- Inventario (polimórfico sobre Nodo)
-- ----------------------------------------------------------------------------
CREATE TABLE inventario (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nodo_id               UUID NOT NULL REFERENCES nodo(id),
    variante_id           UUID NOT NULL REFERENCES variante(id),
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
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variante_id      UUID NOT NULL REFERENCES variante(id),
    tienda_id        UUID NOT NULL REFERENCES tienda(id),
    cantidad         INT NOT NULL,
    precio           NUMERIC(12,2) NOT NULL,
    canal            VARCHAR(30) NOT NULL DEFAULT 'FISICO',
    timestamp_venta  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE solicitud_no_satisfecha (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variante_id           UUID NOT NULL REFERENCES variante(id),
    tienda_id             UUID NOT NULL REFERENCES tienda(id),
    timestamp_solicitud   TIMESTAMP NOT NULL DEFAULT now(),
    registrado_por        VARCHAR(150)
);

CREATE TABLE ventana_quiebre (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variante_id   UUID NOT NULL REFERENCES variante(id),
    tienda_id     UUID NOT NULL REFERENCES tienda(id),
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
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variante_id      UUID REFERENCES variante(id),
    categoria_id     UUID REFERENCES categoria(id),
    cantidad_minima  INT NOT NULL,
    CONSTRAINT chk_umbral_tiene_alcance CHECK (
        variante_id IS NOT NULL OR categoria_id IS NOT NULL
    )
);

CREATE TABLE traspaso_interno (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variante_id         UUID NOT NULL REFERENCES variante(id),
    tienda_id           UUID NOT NULL REFERENCES tienda(id),
    cantidad            INT NOT NULL,
    timestamp_traspaso  TIMESTAMP NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- Reposición (recomendación) y Órdenes de Reabastecimiento
-- ----------------------------------------------------------------------------
CREATE TABLE recomendacion (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variante_id         UUID NOT NULL REFERENCES variante(id),
    tienda_id           UUID NOT NULL REFERENCES tienda(id),
    cd_sugerido_id      UUID REFERENCES cd(id), -- sugerencia blanda, no valida stock
    cantidad_sugerida   INT NOT NULL,
    cantidad_aprobada   INT, -- puede diferir de la sugerida (ver NFR de mantenibilidad)
    estado              VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                          CHECK (estado IN ('PENDIENTE', 'APROBADA', 'DESCARTADA')),
    aprobado_por        VARCHAR(150),
    fecha_aprobacion    TIMESTAMP
);

CREATE TABLE orden_reabastecimiento (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recomendacion_id            UUID REFERENCES recomendacion(id),
    tienda_id                   UUID NOT NULL REFERENCES tienda(id),
    cd_asignado_id              UUID REFERENCES cd(id), -- resultado final del fallback
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
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    orden_id          UUID NOT NULL REFERENCES orden_reabastecimiento(id),
    cd_id             UUID NOT NULL REFERENCES cd(id),
    orden_evaluado    INT NOT NULL, -- posición en la lista de prioridad probada
    resultado         VARCHAR(20) NOT NULL CHECK (resultado IN ('SIN_STOCK', 'ASIGNADO')),
    timestamp_intento TIMESTAMP NOT NULL DEFAULT now()
);

-- Soporta "Recibida (parcial) -> Reabierta": una orden, múltiples ciclos
CREATE TABLE ciclo_despacho (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    orden_id              UUID NOT NULL REFERENCES orden_reabastecimiento(id),
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
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cd_id         UUID NOT NULL REFERENCES cd(id),
    fecha_salida  TIMESTAMP,
    fecha_cierre  TIMESTAMP,
    vehiculo      VARCHAR(100)
);

CREATE TABLE viaje_parada (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    viaje_id                    UUID NOT NULL REFERENCES viaje(id),
    ciclo_despacho_id           UUID NOT NULL REFERENCES ciclo_despacho(id),
    tienda_id                   UUID NOT NULL REFERENCES tienda(id),
    orden_parada                INT NOT NULL,
    timestamp_salida_cd         TIMESTAMP,
    timestamp_llegada_tienda    TIMESTAMP, -- sin checkpoints intermedios (MVP)
    estado                      VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                                  CHECK (estado IN ('PENDIENTE', 'EN_TRANSITO', 'ENTREGADO'))
);

CREATE TABLE recepcion (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    viaje_parada_id       UUID NOT NULL REFERENCES viaje_parada(id),
    ciclo_despacho_id     UUID NOT NULL REFERENCES ciclo_despacho(id),
    cantidad_recibida     INT NOT NULL,
    discrepancia          INT NOT NULL DEFAULT 0, -- cantidad_despachada - cantidad_recibida
    timestamp_recepcion   TIMESTAMP NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- Índices de apoyo para las consultas operativas más frecuentes
-- ----------------------------------------------------------------------------
CREATE INDEX idx_venta_tienda_variante_fecha ON venta (tienda_id, variante_id, timestamp_venta);
CREATE INDEX idx_inventario_variante ON inventario (variante_id);
