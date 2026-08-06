CREATE TABLE menus (
    codm INTEGER GENERATED ALWAYS AS IDENTITY,
    nombre VARCHAR(100) NOT NULL,
    icono VARCHAR(50),
    estado SMALLINT NOT NULL DEFAULT 1,

    CONSTRAINT pk_menus PRIMARY KEY (codm),
    CONSTRAINT uk_menus_nombre UNIQUE (nombre),
    CONSTRAINT ck_menus_estado CHECK (estado IN (0, 1))
);

CREATE TABLE procesos (
    codp INTEGER GENERATED ALWAYS AS IDENTITY,
    nombre VARCHAR(100) NOT NULL,
    enlace VARCHAR(60) NOT NULL,
    estado SMALLINT NOT NULL DEFAULT 1,

    CONSTRAINT pk_procesos PRIMARY KEY (codp),
    CONSTRAINT uk_procesos_nombre UNIQUE (nombre),
    CONSTRAINT uk_procesos_enlace UNIQUE (enlace),
    CONSTRAINT ck_procesos_estado CHECK (estado IN (0, 1))
);

CREATE TABLE rolme (
    codr INTEGER NOT NULL,
    codm INTEGER NOT NULL,

    CONSTRAINT pk_rolme PRIMARY KEY (codr, codm),
    CONSTRAINT fk_rolme_codr FOREIGN KEY (codr) REFERENCES roles (codr) ON DELETE RESTRICT,
    CONSTRAINT fk_rolme_codm FOREIGN KEY (codm) REFERENCES menus (codm) ON DELETE RESTRICT
);

CREATE TABLE mepro (
    codm INTEGER NOT NULL,
    codp INTEGER NOT NULL,

    CONSTRAINT pk_mepro PRIMARY KEY (codm, codp),
    CONSTRAINT fk_mepro_codm FOREIGN KEY (codm) REFERENCES menus (codm) ON DELETE RESTRICT,
    CONSTRAINT fk_mepro_codp FOREIGN KEY (codp) REFERENCES procesos (codp) ON DELETE RESTRICT
);
