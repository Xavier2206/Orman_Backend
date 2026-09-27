DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pagos WHERE metodo = 'TRANSFERENCIA')
       OR EXISTS (SELECT 1 FROM pagos WHERE referencia_externa IS NOT NULL) THEN
        RAISE EXCEPTION
            'V20 abortada: pagos históricos con TRANSFERENCIA o referencia_externa requieren una migración de compatibilidad';
    END IF;

    IF EXISTS (SELECT 1 FROM recibos) THEN
        RAISE EXCEPTION
            'V20 abortada: recibos existentes requieren una decisión de conservación antes de retirarlos';
    END IF;

    IF EXISTS (SELECT 1 FROM cuentas_pago)
       OR EXISTS (SELECT 1 FROM pagos WHERE codcta IS NOT NULL) THEN
        RAISE EXCEPTION
            'V20 abortada: cuentas_pago/codcta existentes no tienen una vigencia QR inequívoca para migrar';
    END IF;

    IF EXISTS (SELECT 1 FROM pago_comprobantes) THEN
        RAISE EXCEPTION
            'V20 abortada: comprobantes existentes requieren conservar sus referencias de archivo antes de cambiar el modelo';
    END IF;
END
$$;

CREATE TABLE qr_cobro (
    codqr INTEGER GENERATED ALWAYS AS IDENTITY,
    codper_propietaria INTEGER NOT NULL,
    ruta_archivo VARCHAR(500) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    tipo_contenido VARCHAR(20) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    estado VARCHAR(10) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_qr_cobro PRIMARY KEY (codqr),
    CONSTRAINT fk_qr_cobro_personas_propietaria
        FOREIGN KEY (codper_propietaria)
        REFERENCES personas (codper)
        ON DELETE RESTRICT,
    CONSTRAINT ck_qr_cobro_tipo_contenido
        CHECK (tipo_contenido IN ('image/png', 'image/jpeg')),
    CONSTRAINT ck_qr_cobro_vigencia
        CHECK (fecha_inicio <= fecha_fin),
    CONSTRAINT ck_qr_cobro_estado
        CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX ix_qr_cobro_propietaria_vigencia
    ON qr_cobro (codper_propietaria, estado, fecha_inicio, fecha_fin);

ALTER TABLE pagos
    DROP CONSTRAINT fk_pagos_cuentas_pago,
    DROP CONSTRAINT ck_pagos_cuenta_por_metodo,
    DROP CONSTRAINT ck_pagos_metodo,
    DROP CONSTRAINT ck_pagos_origen_metodo,
    ADD COLUMN codqr INTEGER;

ALTER TABLE pagos
    ADD CONSTRAINT fk_pagos_qr_cobro
        FOREIGN KEY (codqr) REFERENCES qr_cobro (codqr) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_pagos_metodo
        CHECK (metodo IN ('EFECTIVO', 'QR')),
    ADD CONSTRAINT ck_pagos_qr_por_metodo
        CHECK ((metodo = 'EFECTIVO' AND codqr IS NULL)
            OR (metodo = 'QR' AND codqr IS NOT NULL)),
    ADD CONSTRAINT ck_pagos_origen_metodo
        CHECK (origen_registro <> 'INQUILINO' OR metodo = 'QR');

DROP INDEX ix_pagos_codcta;
DROP INDEX uk_pagos_codcta_referencia_externa;

ALTER TABLE pagos
    DROP COLUMN codcta,
    DROP COLUMN referencia_externa;

CREATE INDEX ix_pagos_codqr ON pagos (codqr);

ALTER TABLE pago_comprobantes
    DROP CONSTRAINT uk_pago_comprobantes_codpag_orden,
    DROP CONSTRAINT ck_pago_comprobantes_orden,
    DROP COLUMN url,
    DROP COLUMN orden,
    ADD COLUMN ruta_archivo VARCHAR(500);

ALTER TABLE pago_comprobantes
    ALTER COLUMN ruta_archivo SET NOT NULL,
    ADD CONSTRAINT uk_pago_comprobantes_codpag UNIQUE (codpag),
    ADD CONSTRAINT ck_pago_comprobantes_tipo_contenido
        CHECK (tipo_contenido IN ('image/png', 'image/jpeg'));

DROP INDEX ix_pago_comprobantes_codpag;

DROP TABLE recibos;
DROP TABLE cuentas_pago;
