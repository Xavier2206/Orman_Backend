-- Los roles antiguos con usuarios requieren una resolución explícita antes de migrar.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM rolusu ru
        JOIN roles r ON r.codr = ru.codr
        WHERE r.nombre NOT IN ('PROPIETARIO', 'INQUILINO')
    ) THEN
        RAISE EXCEPTION 'Existen usuarios asignados a roles fuera del catálogo final; resolver manualmente antes de migrar';
    END IF;
END $$;

DELETE FROM rolme
WHERE codr IN (SELECT codr FROM roles WHERE nombre NOT IN ('PROPIETARIO', 'INQUILINO'));

DELETE FROM roles WHERE nombre NOT IN ('PROPIETARIO', 'INQUILINO');

INSERT INTO roles (nombre, estado)
SELECT required.nombre, 1
FROM (VALUES ('PROPIETARIO'), ('INQUILINO')) AS required(nombre)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE roles.nombre = required.nombre);

UPDATE roles SET estado = 1 WHERE nombre IN ('PROPIETARIO', 'INQUILINO');

ALTER TABLE roles ADD CONSTRAINT ck_roles_catalogo_final
    CHECK (nombre IN ('PROPIETARIO', 'INQUILINO') AND estado = 1);
