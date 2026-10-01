CREATE TABLE dispositivos_push (
    coddis BIGINT GENERATED ALWAYS AS IDENTITY,
    sid UUID NOT NULL,
    installation_id VARCHAR(128) NOT NULL,
    platform VARCHAR(16) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_dispositivos_push PRIMARY KEY (coddis),
    CONSTRAINT fk_dispositivos_push_sid
        FOREIGN KEY (sid)
        REFERENCES sesiones_usuario (sid)
        ON DELETE CASCADE,
    CONSTRAINT uk_dispositivos_push_sid UNIQUE (sid),
    CONSTRAINT uk_dispositivos_push_installation_id UNIQUE (installation_id),
    CONSTRAINT ck_dispositivos_push_platform CHECK (platform IN ('ANDROID'))
);
