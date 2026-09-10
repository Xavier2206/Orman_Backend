CREATE TABLE notificaciones (
    codnot BIGINT GENERATED ALWAYS AS IDENTITY,
    login_destinatario VARCHAR(30) NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    referencia_tipo VARCHAR(20) NOT NULL,
    referencia_id INTEGER NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_lectura TIMESTAMP,

    CONSTRAINT pk_notificaciones PRIMARY KEY (codnot),
    CONSTRAINT fk_notificaciones_usuarios_destinatario
        FOREIGN KEY (login_destinatario)
        REFERENCES usuarios (login)
        ON DELETE RESTRICT,
    CONSTRAINT uk_notificaciones_destinatario_tipo_referencia
        UNIQUE (login_destinatario, tipo, referencia_tipo, referencia_id),
    CONSTRAINT ck_notificaciones_tipo CHECK (tipo IN (
        'CUOTA_PROXIMA_VENCER',
        'CUOTA_VENCIDA',
        'COMPROBANTE_RECIBIDO',
        'PAGO_CONFIRMADO',
        'PAGO_RECHAZADO'
    )),
    CONSTRAINT ck_notificaciones_referencia_tipo CHECK (referencia_tipo IN ('CUOTA', 'PAGO')),
    CONSTRAINT ck_notificaciones_tipo_referencia CHECK (
        (tipo IN ('CUOTA_PROXIMA_VENCER', 'CUOTA_VENCIDA') AND referencia_tipo = 'CUOTA')
        OR (tipo IN ('COMPROBANTE_RECIBIDO', 'PAGO_CONFIRMADO', 'PAGO_RECHAZADO')
            AND referencia_tipo = 'PAGO')
    ),
    CONSTRAINT ck_notificaciones_titulo CHECK (btrim(titulo) <> ''),
    CONSTRAINT ck_notificaciones_mensaje CHECK (btrim(mensaje) <> ''),
    CONSTRAINT ck_notificaciones_fecha_lectura CHECK (
        fecha_lectura IS NULL OR fecha_lectura >= fecha_creacion
    )
);

CREATE INDEX ix_notificaciones_destinatario_fecha_creacion
    ON notificaciones (login_destinatario, fecha_creacion DESC);

CREATE INDEX ix_notificaciones_destinatario_no_leidas
    ON notificaciones (login_destinatario, fecha_creacion DESC)
    WHERE fecha_lectura IS NULL;
