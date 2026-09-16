ALTER TABLE contrato_archivos
    ALTER COLUMN url DROP NOT NULL,
    ADD COLUMN nombre_almacenado VARCHAR(200),
    ADD COLUMN ruta_ref VARCHAR(500),
    ADD COLUMN tamano_original BIGINT,
    ADD COLUMN tamano_final BIGINT,
    ADD COLUMN fecha_subida TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN login_subio VARCHAR(30);

ALTER TABLE contrato_archivos
    ADD CONSTRAINT uk_contrato_archivos_ruta_ref UNIQUE (ruta_ref),
    ADD CONSTRAINT fk_contrato_archivos_usuarios
        FOREIGN KEY (login_subio)
        REFERENCES usuarios (login)
        ON DELETE SET NULL,
    ADD CONSTRAINT ck_contrato_archivos_tamanos CHECK (
        (tamano_original IS NULL OR tamano_original > 0)
        AND (tamano_final IS NULL OR tamano_final > 0)
        AND (tamano_final IS NULL OR tamano_original IS NULL OR tamano_final <= tamano_original)
    ),
    ADD CONSTRAINT ck_contrato_archivos_ruta_privada CHECK (
        ruta_ref IS NULL OR ruta_ref LIKE 'contratos/%'
    );
