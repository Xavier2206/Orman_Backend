ALTER TABLE pagos
    DROP CONSTRAINT ck_pagos_origen;

ALTER TABLE pagos
    RENAME COLUMN origen TO origen_registro;

ALTER TABLE pagos
    ADD COLUMN registrado_por VARCHAR(30),
    ADD COLUMN revisado_por VARCHAR(30);

UPDATE pagos
SET origen_registro = 'PROPIETARIA';

UPDATE pagos p
SET registrado_por = u.login
FROM cuotas q
JOIN contratos c ON c.codcon = q.codcon
JOIN unidades un ON un.coduni = c.coduni
JOIN propiedades pr ON pr.codprop = un.codprop
JOIN usuarios u ON u.codper = pr.codper_propietaria
WHERE p.codcuo = q.codcuo;

UPDATE pagos p
SET revisado_por = p.registrado_por
WHERE p.estado IN ('CONFIRMADO', 'RECHAZADO', 'ANULADO');

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pagos WHERE registrado_por IS NULL) THEN
        RAISE EXCEPTION
            'V17 abortada: existe un pago histórico sin Usuario propietario resoluble';
    END IF;
END $$;

ALTER TABLE pagos
    ALTER COLUMN origen_registro DROP DEFAULT,
    ALTER COLUMN registrado_por SET NOT NULL,
    ADD CONSTRAINT fk_pagos_usuarios_registrador
        FOREIGN KEY (registrado_por) REFERENCES usuarios (login) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_pagos_usuarios_revisor
        FOREIGN KEY (revisado_por) REFERENCES usuarios (login) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_pagos_origen_registro
        CHECK (origen_registro IN ('PROPIETARIA', 'INQUILINO')),
    ADD CONSTRAINT ck_pagos_origen_metodo
        CHECK (origen_registro <> 'INQUILINO' OR metodo IN ('TRANSFERENCIA', 'QR')),
    ADD CONSTRAINT ck_pagos_actor_revision CHECK (
        (estado = 'PENDIENTE_REVISION' AND revisado_por IS NULL)
        OR (estado IN ('CONFIRMADO', 'RECHAZADO', 'ANULADO') AND revisado_por IS NOT NULL)
    );

CREATE INDEX ix_pagos_registrado_por ON pagos (registrado_por);
CREATE INDEX ix_pagos_revisado_por ON pagos (revisado_por);
