DO $migration$
DECLARE
    v_codm_dashboard INTEGER;
    v_codp_resumen INTEGER;
    v_codr_propietario INTEGER;
BEGIN
    SELECT codr
    INTO v_codr_propietario
    FROM roles
    WHERE nombre = 'PROPIETARIO'
      AND estado = 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'V27 requiere un rol PROPIETARIO activo';
    END IF;

    INSERT INTO menus (nombre, icono, estado)
    VALUES ('DASHBOARD', 'dashboard', 1)
    ON CONFLICT (nombre) DO UPDATE
        SET icono = EXCLUDED.icono,
            estado = EXCLUDED.estado;

    SELECT codm
    INTO v_codm_dashboard
    FROM menus
    WHERE nombre = 'DASHBOARD';

    IF NOT FOUND THEN
        RAISE EXCEPTION 'V27 no pudo resolver el menú DASHBOARD';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM rolme
        WHERE codm = v_codm_dashboard
          AND codr <> v_codr_propietario
    ) THEN
        RAISE EXCEPTION 'V27 encontró el menú DASHBOARD asignado a otro rol';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM procesos
        WHERE (nombre = 'RESUMEN FINANCIERO' AND enlace <> 'dashboard/resumen-financiero')
           OR (enlace = 'dashboard/resumen-financiero' AND nombre <> 'RESUMEN FINANCIERO')
    ) THEN
        RAISE EXCEPTION 'V27 encontró una colisión para el proceso RESUMEN FINANCIERO';
    END IF;

    INSERT INTO procesos (nombre, enlace, estado)
    VALUES ('RESUMEN FINANCIERO', 'dashboard/resumen-financiero', 1)
    ON CONFLICT (nombre) DO UPDATE
        SET estado = EXCLUDED.estado;

    SELECT codp
    INTO v_codp_resumen
    FROM procesos
    WHERE nombre = 'RESUMEN FINANCIERO'
      AND enlace = 'dashboard/resumen-financiero';

    IF NOT FOUND THEN
        RAISE EXCEPTION 'V27 no pudo resolver el proceso RESUMEN FINANCIERO';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM mepro
        WHERE codp = v_codp_resumen
          AND codm <> v_codm_dashboard
    ) THEN
        RAISE EXCEPTION 'V27 encontró RESUMEN FINANCIERO asociado a otro menú';
    END IF;

    INSERT INTO mepro (codm, codp)
    VALUES (v_codm_dashboard, v_codp_resumen)
    ON CONFLICT (codm, codp) DO NOTHING;

    INSERT INTO rolme (codr, codm)
    VALUES (v_codr_propietario, v_codm_dashboard)
    ON CONFLICT (codr, codm) DO NOTHING;
END
$migration$;
