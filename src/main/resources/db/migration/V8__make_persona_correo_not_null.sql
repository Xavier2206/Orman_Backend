DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM personas WHERE correo IS NULL) THEN
        RAISE EXCEPTION
            'V8 abortada: existen Personas con correo NULL; corregirlas manualmente antes de aplicar NOT NULL';
    END IF;
END $$;

ALTER TABLE personas
    ALTER COLUMN correo SET NOT NULL;
