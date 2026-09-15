ALTER TABLE unidad_fotos
    ADD COLUMN foto_ref VARCHAR(500);

ALTER TABLE unidad_fotos
    ALTER COLUMN url DROP NOT NULL;

ALTER TABLE unidad_fotos
    ADD CONSTRAINT ck_unidad_fotos_fuente
    CHECK ((url IS NOT NULL AND foto_ref IS NULL)
        OR (url IS NULL AND foto_ref IS NOT NULL));
