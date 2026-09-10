CREATE TABLE propiedades (
    codprop INTEGER GENERATED ALWAYS AS IDENTITY,
    nombre VARCHAR(120) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    direccion VARCHAR(200) NOT NULL,
    ciudad VARCHAR(100) NOT NULL,
    referencia VARCHAR(255),
    latitud NUMERIC(9, 6),
    longitud NUMERIC(9, 6),
    portada_url VARCHAR(500),
    codper_propietaria INTEGER NOT NULL,
    inversion_inicial NUMERIC(14, 2) NOT NULL DEFAULT 0,
    estado SMALLINT NOT NULL DEFAULT 1,

    CONSTRAINT pk_propiedades PRIMARY KEY (codprop),
    CONSTRAINT fk_propiedades_personas_propietaria
        FOREIGN KEY (codper_propietaria)
        REFERENCES personas (codper)
        ON DELETE RESTRICT,
    CONSTRAINT ck_propiedades_tipo CHECK (tipo IN ('CASA', 'EDIFICIO')),
    CONSTRAINT ck_propiedades_coordenadas
        CHECK ((latitud IS NULL AND longitud IS NULL)
            OR (latitud IS NOT NULL AND longitud IS NOT NULL)),
    CONSTRAINT ck_propiedades_latitud CHECK (latitud IS NULL OR latitud BETWEEN -90 AND 90),
    CONSTRAINT ck_propiedades_longitud CHECK (longitud IS NULL OR longitud BETWEEN -180 AND 180),
    CONSTRAINT ck_propiedades_inversion_inicial CHECK (inversion_inicial >= 0),
    CONSTRAINT ck_propiedades_estado CHECK (estado IN (0, 1))
);

CREATE INDEX ix_propiedades_codper_propietaria ON propiedades (codper_propietaria);

CREATE TABLE unidades (
    coduni INTEGER GENERATED ALWAYS AS IDENTITY,
    codprop INTEGER NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    tipo_unidad VARCHAR(50) NOT NULL,
    descripcion VARCHAR(500),
    area NUMERIC(10, 2) NOT NULL,
    dormitorios SMALLINT NOT NULL DEFAULT 0,
    banos SMALLINT NOT NULL DEFAULT 0,
    piso INTEGER NOT NULL DEFAULT 0,
    ubicacion_interna VARCHAR(150),
    precio_base NUMERIC(14, 2) NOT NULL,
    estado_operativo SMALLINT NOT NULL DEFAULT 1,

    CONSTRAINT pk_unidades PRIMARY KEY (coduni),
    CONSTRAINT fk_unidades_propiedades
        FOREIGN KEY (codprop)
        REFERENCES propiedades (codprop)
        ON DELETE RESTRICT,
    CONSTRAINT uk_unidades_codprop_nombre UNIQUE (codprop, nombre),
    CONSTRAINT ck_unidades_area CHECK (area >= 0),
    CONSTRAINT ck_unidades_dormitorios CHECK (dormitorios >= 0),
    CONSTRAINT ck_unidades_banos CHECK (banos >= 0),
    CONSTRAINT ck_unidades_piso CHECK (piso >= 0),
    CONSTRAINT ck_unidades_precio_base CHECK (precio_base >= 0),
    CONSTRAINT ck_unidades_estado_operativo CHECK (estado_operativo IN (0, 1))
);

CREATE INDEX ix_unidades_codprop ON unidades (codprop);

CREATE TABLE unidad_fotos (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    coduni INTEGER NOT NULL,
    url VARCHAR(500) NOT NULL,
    titulo VARCHAR(150),
    ambiente VARCHAR(100),
    orden INTEGER NOT NULL,
    portada BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_unidad_fotos PRIMARY KEY (id),
    CONSTRAINT fk_unidad_fotos_unidades
        FOREIGN KEY (coduni)
        REFERENCES unidades (coduni)
        ON DELETE RESTRICT,
    CONSTRAINT uk_unidad_fotos_coduni_orden UNIQUE (coduni, orden),
    CONSTRAINT ck_unidad_fotos_orden CHECK (orden >= 0)
);

CREATE INDEX ix_unidad_fotos_coduni ON unidad_fotos (coduni);

CREATE UNIQUE INDEX uk_unidad_fotos_coduni_portada
    ON unidad_fotos (coduni)
    WHERE portada = TRUE;
