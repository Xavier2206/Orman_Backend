INSERT INTO menus (nombre, icono, estado)
VALUES
    ('CONTROL DE ACCESO', 'admin_panel_settings', 1),
    ('GESTIÓN CONTRATOS', 'view_module', 1),
    ('GESTIÓN DE PAGOS', 'manage_accounts', 1),
    ('GESTIÓN PROPIEDADES', 'account_balance', 1),
    ('GESTIONAR PERSONAS', 'group', 1)
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO procesos (nombre, enlace, estado)
VALUES
    ('ASIGNAR MENÚS', 'asignar-menus/listar', 1),
    ('ASIGNAR PROCESOS', 'asignar-procesos/listar', 1),
    ('ASIGNAR ROLES', 'asignar-roles/listar', 1),
    ('CONTRATOS', 'contratos/listar', 1),
    ('GESTIONAR MENÚS', 'menus/listar', 1),
    ('GESTIONAR PAGOS', 'pagos/listar', 1),
    ('GESTIONAR PROCESOS', 'asigna/listar', 1),
    ('GESTIONAR ROLES', 'roles/listar', 1),
    ('LISTAR PERSONAS', 'personas/listar', 1),
    ('PROPIEDADES', 'propiedades/listar', 1),
    ('QR DE COBRO', 'pagos/qr-cobro', 1),
    ('UNIDADES', 'unidades/listar', 1)
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO mepro (codm, codp)
SELECT m.codm, p.codp
FROM (
    VALUES
        ('CONTROL DE ACCESO', 'ASIGNAR MENÚS'),
        ('CONTROL DE ACCESO', 'ASIGNAR PROCESOS'),
        ('CONTROL DE ACCESO', 'ASIGNAR ROLES'),
        ('CONTROL DE ACCESO', 'GESTIONAR MENÚS'),
        ('CONTROL DE ACCESO', 'GESTIONAR ROLES'),
        ('GESTIÓN CONTRATOS', 'CONTRATOS'),
        ('GESTIÓN DE PAGOS', 'GESTIONAR PAGOS'),
        ('GESTIÓN DE PAGOS', 'QR DE COBRO'),
        ('GESTIÓN PROPIEDADES', 'PROPIEDADES'),
        ('GESTIÓN PROPIEDADES', 'UNIDADES'),
        ('GESTIONAR PERSONAS', 'LISTAR PERSONAS')
) AS catalog(menu_nombre, proceso_nombre)
JOIN menus m ON m.nombre = catalog.menu_nombre
JOIN procesos p ON p.nombre = catalog.proceso_nombre
ON CONFLICT (codm, codp) DO NOTHING;

INSERT INTO rolme (codr, codm)
SELECT r.codr, m.codm
FROM roles r
JOIN menus m ON m.nombre IN (
    'CONTROL DE ACCESO',
    'GESTIÓN CONTRATOS',
    'GESTIÓN DE PAGOS',
    'GESTIÓN PROPIEDADES',
    'GESTIONAR PERSONAS'
)
WHERE r.nombre = 'PROPIETARIO'
ON CONFLICT (codr, codm) DO NOTHING;
