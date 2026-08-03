CREATE TABLE sesiones_usuario (
    sid UUID NOT NULL,
    login VARCHAR(30) NOT NULL,
    refresh_token_hash VARCHAR(255) NOT NULL,
    refresh_token_version INTEGER NOT NULL DEFAULT 1,
    device_id VARCHAR(100) NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    client_type VARCHAR(10) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP NOT NULL,
    ultimo_uso TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_revocacion TIMESTAMP,
    motivo_revocacion VARCHAR(40),

    CONSTRAINT pk_sesiones_usuario PRIMARY KEY (sid),
    CONSTRAINT fk_sesiones_usuario_login
        FOREIGN KEY (login)
        REFERENCES usuarios (login)
        ON DELETE CASCADE,
    CONSTRAINT ck_sesiones_usuario_client_type
        CHECK (client_type IN ('WEB', 'MOBILE')),
    CONSTRAINT ck_sesiones_usuario_refresh_token_version
        CHECK (refresh_token_version > 0),
    CONSTRAINT ck_sesiones_usuario_revocacion
        CHECK (
            (fecha_revocacion IS NULL AND motivo_revocacion IS NULL)
            OR
            (fecha_revocacion IS NOT NULL AND motivo_revocacion IS NOT NULL)
        )
);

CREATE INDEX ix_sesiones_usuario_login
    ON sesiones_usuario (login);

CREATE UNIQUE INDEX uk_sesiones_usuario_login_device_active
    ON sesiones_usuario (login, device_id)
    WHERE fecha_revocacion IS NULL;
