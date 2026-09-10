CREATE TABLE contratos (
    codcon INTEGER GENERATED ALWAYS AS IDENTITY,
    coduni INTEGER NOT NULL,
    codper_inquilino INTEGER NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    monto_mensual NUMERIC(14, 2) NOT NULL,
    garantia NUMERIC(14, 2) NOT NULL DEFAULT 0,
    estado VARCHAR(15) NOT NULL DEFAULT 'BORRADOR',
    fecha_confirmacion TIMESTAMP,
    fecha_rescision DATE,
    motivo_rescision VARCHAR(500),
    codcon_origen INTEGER,

    CONSTRAINT pk_contratos PRIMARY KEY (codcon),
    CONSTRAINT fk_contratos_unidades
        FOREIGN KEY (coduni)
        REFERENCES unidades (coduni)
        ON DELETE RESTRICT,
    CONSTRAINT fk_contratos_personas_inquilino
        FOREIGN KEY (codper_inquilino)
        REFERENCES personas (codper)
        ON DELETE RESTRICT,
    CONSTRAINT fk_contratos_contrato_origen
        FOREIGN KEY (codcon_origen)
        REFERENCES contratos (codcon)
        ON DELETE RESTRICT,
    CONSTRAINT ck_contratos_fechas CHECK (fecha_inicio < fecha_fin),
    CONSTRAINT ck_contratos_inicio_mes CHECK (EXTRACT(DAY FROM fecha_inicio) = 1),
    CONSTRAINT ck_contratos_fin_mes CHECK (EXTRACT(DAY FROM fecha_fin) = 1),
    CONSTRAINT ck_contratos_monto_mensual CHECK (monto_mensual >= 0),
    CONSTRAINT ck_contratos_garantia CHECK (garantia >= 0),
    CONSTRAINT ck_contratos_estado CHECK (estado IN ('BORRADOR', 'VIGENTE', 'FINALIZADO', 'RESCINDIDO')),
    CONSTRAINT ck_contratos_confirmacion CHECK (
        (estado = 'BORRADOR' AND fecha_confirmacion IS NULL)
        OR (estado IN ('VIGENTE', 'FINALIZADO', 'RESCINDIDO') AND fecha_confirmacion IS NOT NULL)
    ),
    CONSTRAINT ck_contratos_rescision CHECK (
        (estado = 'RESCINDIDO'
         AND fecha_rescision IS NOT NULL
         AND motivo_rescision IS NOT NULL
         AND fecha_rescision >= fecha_inicio
         AND fecha_rescision < fecha_fin)
        OR (estado <> 'RESCINDIDO'
            AND fecha_rescision IS NULL
            AND motivo_rescision IS NULL)
    )
);

CREATE INDEX ix_contratos_coduni ON contratos (coduni);
CREATE INDEX ix_contratos_codper_inquilino ON contratos (codper_inquilino);
CREATE INDEX ix_contratos_estado ON contratos (estado);

CREATE UNIQUE INDEX uk_contratos_coduni_vigente
    ON contratos (coduni)
    WHERE estado = 'VIGENTE';

CREATE TABLE contrato_archivos (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    codcon INTEGER NOT NULL,
    url VARCHAR(500) NOT NULL,
    nombre_archivo VARCHAR(200) NOT NULL,
    tipo_contenido VARCHAR(100),
    orden INTEGER NOT NULL,

    CONSTRAINT pk_contrato_archivos PRIMARY KEY (id),
    CONSTRAINT fk_contrato_archivos_contratos
        FOREIGN KEY (codcon)
        REFERENCES contratos (codcon)
        ON DELETE RESTRICT,
    CONSTRAINT uk_contrato_archivos_codcon_orden UNIQUE (codcon, orden),
    CONSTRAINT ck_contrato_archivos_orden CHECK (orden >= 0)
);

CREATE INDEX ix_contrato_archivos_codcon ON contrato_archivos (codcon);

CREATE TABLE cuotas (
    codcuo INTEGER GENERATED ALWAYS AS IDENTITY,
    codcon INTEGER NOT NULL,
    periodo DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    monto NUMERIC(14, 2) NOT NULL,
    estado VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE',

    CONSTRAINT pk_cuotas PRIMARY KEY (codcuo),
    CONSTRAINT fk_cuotas_contratos
        FOREIGN KEY (codcon)
        REFERENCES contratos (codcon)
        ON DELETE RESTRICT,
    CONSTRAINT uk_cuotas_codcon_periodo UNIQUE (codcon, periodo),
    CONSTRAINT ck_cuotas_periodo_mes CHECK (EXTRACT(DAY FROM periodo) = 1),
    CONSTRAINT ck_cuotas_vencimiento_mes CHECK (fecha_vencimiento = periodo),
    CONSTRAINT ck_cuotas_monto CHECK (monto >= 0),
    CONSTRAINT ck_cuotas_estado CHECK (estado IN ('PENDIENTE', 'PARCIAL', 'PAGADA', 'ANULADA'))
);

CREATE INDEX ix_cuotas_codcon ON cuotas (codcon);
CREATE INDEX ix_cuotas_periodo ON cuotas (periodo);
