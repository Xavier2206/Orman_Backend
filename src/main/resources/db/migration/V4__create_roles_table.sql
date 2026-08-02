CREATE TABLE roles (
    codr INTEGER GENERATED ALWAYS AS IDENTITY,
    nombre VARCHAR(50) NOT NULL,
    estado SMALLINT NOT NULL DEFAULT 1,

    CONSTRAINT pk_roles PRIMARY KEY (codr),
    CONSTRAINT uk_roles_nombre UNIQUE (nombre),
    CONSTRAINT ck_roles_estado CHECK (estado IN (0, 1))
);
