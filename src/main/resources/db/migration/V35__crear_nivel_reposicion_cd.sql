-- ============================================================================
-- V35__crear_nivel_reposicion_cd.sql
-- HD-04 (Sprint 3): nivel de reposición de la Bodega_Tienda frente al CD
-- (decisión D-74). Mismo diseño que umbral_reposicion_interna tras V21 (D-51):
-- una variante o una categoría, nunca ambas; mínimo mayor que cero; objetivo
-- mayor que el mínimo; uno por variante y uno por categoría. Sin override por
-- tienda.
--
-- Es V35 y no V31, como proponía el plan: V32 y V33 ya están aplicadas y Flyway
-- rechaza una versión menor que llega después.
-- ============================================================================

CREATE TABLE nivel_reposicion_cd (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variante_id       UUID REFERENCES variante(id),
    categoria_id      UUID REFERENCES categoria(id),
    cantidad_minima   INT NOT NULL,
    cantidad_objetivo INT NOT NULL,
    CONSTRAINT chk_nivel_cd_alcance_exclusivo CHECK (num_nonnulls(variante_id, categoria_id) = 1),
    CONSTRAINT chk_nivel_cd_minimo_positivo CHECK (cantidad_minima > 0),
    CONSTRAINT chk_nivel_cd_objetivo_mayor_que_minimo CHECK (cantidad_objetivo > cantidad_minima)
);

CREATE UNIQUE INDEX uq_nivel_cd_variante ON nivel_reposicion_cd (variante_id) WHERE variante_id IS NOT NULL;
CREATE UNIQUE INDEX uq_nivel_cd_categoria ON nivel_reposicion_cd (categoria_id) WHERE categoria_id IS NOT NULL;
