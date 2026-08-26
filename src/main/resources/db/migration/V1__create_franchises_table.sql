-- ============================================================================
-- V1 — Tabla raíz del agregado Franquicia
--
-- Decisión de modelado: una fila por franquicia, con el árbol completo de
-- sucursales y productos embebido en una columna JSONB.
--
-- Motivo: Franquicia es el aggregate root y la frontera de consistencia. Al
-- guardar el árbol completo en una sola fila, cualquier operación sobre una
-- sucursal o un producto es una única escritura atómica, sin transacciones
-- que abarquen varias tablas ni JOINs de reconstrucción en cada lectura.
--
-- El trade-off (documento grande si una franquicia acumula decenas de miles
-- de productos) y la ruta de evolución están descritos en el README.
-- ============================================================================

CREATE TABLE franchises (
    id          UUID        PRIMARY KEY,
    name        TEXT        NOT NULL,

    -- Estructura: [{ "id", "name", "products": [{ "id", "name", "stock" }] }]
    branches    JSONB       NOT NULL DEFAULT '[]'::jsonb,

    -- Bloqueo optimista. El agregado se lee, se modifica en memoria y se
    -- vuelve a escribir completo; sin esta columna, dos escrituras
    -- concurrentes sobre la misma franquicia harían que la última pisara a
    -- la primera. Spring Data R2DBC incrementa y verifica este valor en cada
    -- UPDATE, y devuelve un fallo si la versión ya cambió.
    version     BIGINT      NOT NULL DEFAULT 0,

    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- El nombre de la franquicia es su identificador de negocio: se exige único
-- sin distinguir mayúsculas para que "Vive Fresh" y "vive fresh" colisionen.
CREATE UNIQUE INDEX ux_franchises_name_lower ON franchises (lower(name));

-- Índice GIN sobre el árbol embebido. Habilita búsquedas por contenido
-- (operador @>) sobre sucursales y productos sin recorrer toda la tabla,
-- que es lo que sostiene el crecimiento del catálogo.
CREATE INDEX ix_franchises_branches_gin ON franchises USING GIN (branches jsonb_path_ops);

COMMENT ON TABLE  franchises          IS 'Aggregate root Franquicia con su árbol de sucursales y productos';
COMMENT ON COLUMN franchises.branches IS 'Árbol embebido de sucursales y sus productos (JSONB)';
COMMENT ON COLUMN franchises.version  IS 'Contador de bloqueo optimista gestionado por Spring Data R2DBC';
