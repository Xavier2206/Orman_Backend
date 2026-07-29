CREATE TABLE personas (
    codper INTEGER GENERATED ALWAYS AS IDENTITY CONSTRAINT pk_personas PRIMARY KEY,
    ci VARCHAR(20) NOT NULL,
    nombre VARCHAR(60) NOT NULL,
    ap VARCHAR(40),
    am VARCHAR(40),
    genero CHAR(1) NOT NULL,
    estado SMALLINT NOT NULL DEFAULT 1,
    correo VARCHAR(100),
    telefono VARCHAR(20) NOT NULL,
    tipo_persona CHAR(1) NOT NULL,
    foto VARCHAR(255),
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_personas_ci UNIQUE (ci),
    CONSTRAINT ck_personas_genero CHECK (genero IN ('M', 'F')),
    CONSTRAINT ck_personas_estado CHECK (estado IN (0, 1)),
    CONSTRAINT ck_personas_tipo_persona CHECK (tipo_persona IN ('A', 'I'))
);
