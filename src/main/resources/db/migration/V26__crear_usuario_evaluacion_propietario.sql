-- Esta migración crea la cuenta del autor para demostración y evaluación académica de ORMAN.
DO $migration$
DECLARE
    v_codper INTEGER;
    v_codr INTEGER;
    v_persona_matches BIGINT;
    v_login_codper INTEGER;
    v_login_estado SMALLINT;
    v_login_passwd VARCHAR(255);
BEGIN
    -- El correo no tiene restricción UNIQUE; bloquear escrituras mientras
    -- se comprueba si CI o correo ya están registrados.
    LOCK TABLE personas, usuarios, roles, rolusu
        IN SHARE ROW EXCLUSIVE MODE;

    SELECT r.codr
    INTO v_codr
    FROM roles r
    WHERE r.nombre = 'PROPIETARIO'
      AND r.estado = 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'No existe un rol PROPIETARIO activo; V26 no creará uno nuevo';
    END IF;

    SELECT COUNT(DISTINCT p.codper), MIN(p.codper)
    INTO v_persona_matches, v_codper
    FROM personas p
    WHERE p.ci = '10309981'
       OR p.correo = 'ortegaxavier75@gmail.com';

    IF v_persona_matches > 1 THEN
        RAISE EXCEPTION
            'La CI o el correo de Xavier coinciden con personas distintas';
    ELSIF v_persona_matches = 1 THEN
        IF NOT EXISTS (
            SELECT 1
            FROM personas p
            WHERE p.codper = v_codper
              AND p.ci = '10309981'
              AND p.nombre = 'Xavier'
              AND p.ap = 'Ortega'
              AND p.am = 'Mancilla'
              AND p.genero = 'M'
              AND p.estado = 1
              AND p.correo = 'ortegaxavier75@gmail.com'
              AND p.telefono = '77777777'
              AND p.tipo_persona = 'A'
              AND COALESCE(p.foto, '') = ''
        ) THEN
            RAISE EXCEPTION
                'La CI o el correo ya existen con datos incompatibles; V26 no modificará esa persona';
        END IF;
    ELSE
        INSERT INTO personas (
            ci,
            nombre,
            ap,
            am,
            genero,
            estado,
            correo,
            telefono,
            tipo_persona,
            foto,
            creada_por_login
        )
        VALUES (
            '10309981',
            'Xavier',
            'Ortega',
            'Mancilla',
            'M',
            1,
            'ortegaxavier75@gmail.com',
            '77777777',
            'A',
            NULL,
            NULL
        )
        RETURNING codper INTO v_codper;
    END IF;

    SELECT u.codper, u.estado, u.passwd
    INTO v_login_codper, v_login_estado, v_login_passwd
    FROM usuarios u
    WHERE u.login = 'Propietario@orman';

    IF FOUND THEN
        IF v_login_codper <> v_codper THEN
            RAISE EXCEPTION
                'El login Propietario@orman ya está asociado a otra persona';
        END IF;

        IF v_login_estado <> 1
           OR v_login_passwd <> '$2a$10$pFl45ShYta3bWuS1kCrt4eXHxD09x7I/QKbcx9FO6CH050xM3guiK' THEN
            RAISE EXCEPTION
                'El login Propietario@orman ya existe con estado o contraseña distintos; V26 no los sobrescribirá';
        END IF;
    ELSE
        IF EXISTS (
            SELECT 1
            FROM usuarios u
            WHERE u.codper = v_codper
        ) THEN
            RAISE EXCEPTION
                'La persona de Xavier ya está asociada a otro usuario';
        END IF;

        INSERT INTO usuarios (
            login,
            passwd,
            estado,
            codper
        )
        VALUES (
            'Propietario@orman',
            '$2a$10$pFl45ShYta3bWuS1kCrt4eXHxD09x7I/QKbcx9FO6CH050xM3guiK',
            1,
            v_codper
        );
    END IF;

    INSERT INTO rolusu (login, codr)
    VALUES ('Propietario@orman', v_codr)
    ON CONFLICT (login, codr) DO NOTHING;
END
$migration$;
