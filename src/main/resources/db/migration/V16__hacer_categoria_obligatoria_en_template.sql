-- ============================================================================
-- V16__hacer_categoria_obligatoria_en_template.sql
-- HU-26: todo Template debe pertenecer a una Categoria.
--
-- Los templates historicos sin categoria se agrupan en SIN_CATEGORIA antes de
-- endurecer la restriccion de la columna.
-- ============================================================================

INSERT INTO categoria (id, nombre)
SELECT gen_random_uuid(), 'SIN_CATEGORIA'
WHERE NOT EXISTS (
    SELECT 1
    FROM categoria
    WHERE nombre = 'SIN_CATEGORIA'
);

UPDATE template
SET categoria_id = (
    SELECT id
    FROM categoria
    WHERE nombre = 'SIN_CATEGORIA'
)
WHERE categoria_id IS NULL;

ALTER TABLE template
    ALTER COLUMN categoria_id SET NOT NULL;
