# Modelo inicial analizado

## Alcance

Este documento conserva el análisis del núcleo de personas, usuarios y roles. No representa una migración ni autoriza la creación de tablas fuera de la fase correspondiente.

## Tablas del núcleo

### `personas`

Representa los datos de una persona. `tipo_persona` es una clasificación de negocio y no sustituye roles, permisos ni autorización.

### `usuarios`

Representa la identidad de acceso asociada a una persona. La Fase 06 confirmó `login VARCHAR(30)` como clave primaria y `passwd VARCHAR(255)` como columna reservada para un hash seguro. La Fase 07 introdujo BCrypt mediante `PasswordEncoder`; nunca se almacena texto plano ni se expone o registra el hash.

### `roles`

Representa agrupaciones de permisos asignables a usuarios. Su catálogo, restricciones y estados se definirán en la Fase 08.

### Relación Usuario–Rol

Representa la relación de muchos a muchos entre usuarios y roles. Antes de la Fase 08 se debe elegir un único nombre definitivo entre `usuarios_roles`, `rol_usuario` u otro nombre aprobado. No hay un nombre definitivo todavía.

## Relaciones confirmadas

- Una persona puede tener como máximo un usuario.
- Un usuario puede tener varios roles.
- Un rol puede pertenecer a varios usuarios.
- La tabla relacional que se elija en la Fase 08 materializará la relación entre `usuarios` y `roles`.

La relación Persona–Usuario se implementó en V3: `usuarios.codper` es obligatorio, único y referencia `personas.codper` con `ON DELETE RESTRICT`.

## Decisiones preliminares

- Usar nombres SQL en minúsculas y `snake_case`.
- Mantener `tipo_persona` separada del sistema de roles.
- Usar `passwd` como columna persistida para el futuro hash; nunca almacenar texto plano.
- Introducir BCrypt en la Fase 07 sin activar todavía Spring Security HTTP completo.
- Evitar `ON DELETE CASCADE` entre `personas` y `usuarios`.
- Definir restricciones, índices y nulabilidad a partir de reglas confirmadas.
- Diseñar `sesiones_usuario` únicamente en la Fase 10.

## Estado de implementación

La Fase 04 completó `personas` con V1 y V2; la Fase 06 completó `usuarios` con V3. Sus modelos definitivos están documentados en `docs/fases/04-modelo-migracion-persona.md` y `docs/fases/06-modelo-migracion-usuario.md`. Roles, sesiones y las demás tablas futuras no están implementados.
