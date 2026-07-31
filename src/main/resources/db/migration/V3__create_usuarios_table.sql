CREATE TABLE usuarios (
    login VARCHAR(30) NOT NULL,
    passwd VARCHAR(255) NOT NULL,
    estado SMALLINT NOT NULL DEFAULT 1,
    codper INTEGER NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso TIMESTAMP,

    CONSTRAINT pk_usuarios PRIMARY KEY (login),
    CONSTRAINT uk_usuarios_codper UNIQUE (codper),
    CONSTRAINT ck_usuarios_estado
        CHECK (estado IN (0, 1)),
    CONSTRAINT fk_usuarios_personas
        FOREIGN KEY (codper)
        REFERENCES personas (codper)
        ON DELETE RESTRICT
);
