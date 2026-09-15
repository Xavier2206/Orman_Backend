ALTER TABLE contratos
    DROP CONSTRAINT ck_contratos_confirmacion,
    DROP CONSTRAINT ck_contratos_estado,
    DROP CONSTRAINT ck_contratos_monto_mensual;

DROP INDEX uk_contratos_coduni_vigente;

ALTER TABLE contratos
    RENAME COLUMN fecha_confirmacion TO fecha_registro;

ALTER TABLE contratos
    ADD COLUMN moneda VARCHAR(3);

UPDATE contratos
SET moneda = 'BOB';

UPDATE contratos
SET estado = CASE
        WHEN fecha_inicio > CURRENT_DATE THEN 'PROGRAMADO'
        ELSE 'VIGENTE'
    END,
    fecha_registro = COALESCE(fecha_registro, CURRENT_TIMESTAMP)
WHERE estado = 'BORRADOR';

UPDATE contratos
SET estado = 'PROGRAMADO'
WHERE estado = 'VIGENTE'
  AND fecha_inicio > CURRENT_DATE;

INSERT INTO cuotas (codcon, periodo, fecha_vencimiento, monto, estado)
SELECT c.codcon,
       periodo::DATE,
       periodo::DATE,
       c.monto_mensual,
       'PENDIENTE'
FROM contratos c
CROSS JOIN LATERAL generate_series(
        c.fecha_inicio::TIMESTAMP,
        (c.fecha_fin - INTERVAL '1 month')::TIMESTAMP,
        INTERVAL '1 month') AS periodo
WHERE c.estado IN ('PROGRAMADO', 'VIGENTE')
  AND NOT EXISTS (
      SELECT 1
      FROM cuotas q
      WHERE q.codcon = c.codcon
        AND q.periodo = periodo::DATE
  );

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM contratos a
        JOIN contratos b
          ON a.coduni = b.coduni
         AND a.codcon < b.codcon
         AND a.estado IN ('PROGRAMADO', 'VIGENTE')
         AND b.estado IN ('PROGRAMADO', 'VIGENTE')
         AND a.fecha_inicio < b.fecha_fin
         AND b.fecha_inicio < a.fecha_fin
    ) THEN
        RAISE EXCEPTION
            'V16 abortada: existen contratos PROGRAMADO/VIGENTE solapados para una misma unidad';
    END IF;
END $$;

ALTER TABLE contratos
    ALTER COLUMN estado DROP DEFAULT,
    ALTER COLUMN fecha_registro SET NOT NULL,
    ALTER COLUMN moneda SET NOT NULL,
    ALTER COLUMN moneda SET DEFAULT 'BOB',
    ADD CONSTRAINT ck_contratos_monto_mensual CHECK (monto_mensual > 0),
    ADD CONSTRAINT ck_contratos_moneda CHECK (moneda = 'BOB'),
    ADD CONSTRAINT ck_contratos_estado
        CHECK (estado IN ('PROGRAMADO', 'VIGENTE', 'FINALIZADO', 'RESCINDIDO')),
    ADD CONSTRAINT ck_contratos_registro CHECK (fecha_registro IS NOT NULL);

CREATE INDEX ix_contratos_coduni_ocupacion
    ON contratos (coduni, fecha_inicio, fecha_fin)
    WHERE estado IN ('PROGRAMADO', 'VIGENTE');

