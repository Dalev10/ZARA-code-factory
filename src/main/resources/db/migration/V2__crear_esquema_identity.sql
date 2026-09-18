CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    email VARCHAR(255) NOT NULL UNIQUE,
    nombre_completo VARCHAR(150) NOT NULL,

    password_hash VARCHAR(255) NOT NULL,

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
        CHECK (estado IN ('ACTIVO', 'BLOQUEADO', 'INACTIVO')),

    intentos_fallidos INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta TIMESTAMP WITH TIME ZONE NULL,

    mfa_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret_encrypted VARCHAR(512) NULL,

    proveedor_externo VARCHAR(50) NULL,

    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rol (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),

    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE scope (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    codigo VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),

    sensible BOOLEAN NOT NULL DEFAULT FALSE,

    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuario_rol (
    usuario_id UUID NOT NULL
        REFERENCES usuario(id) ON DELETE CASCADE,

    rol_id UUID NOT NULL
        REFERENCES rol(id) ON DELETE CASCADE,

    PRIMARY KEY (usuario_id, rol_id)
);

CREATE INDEX idx_usuario_rol_rol
    ON usuario_rol (rol_id);


CREATE TABLE rol_scope (
    rol_id UUID NOT NULL
        REFERENCES rol(id) ON DELETE CASCADE,

    scope_id UUID NOT NULL
        REFERENCES scope(id) ON DELETE CASCADE,

    PRIMARY KEY (rol_id, scope_id)
);

CREATE INDEX idx_rol_scope_scope
    ON rol_scope (scope_id);