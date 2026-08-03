# Modelo inicial analizado

## Alcance

Este documento conserva el análisis del núcleo de personas, usuarios y roles. No representa una migración ni autoriza la creación de tablas fuera de la fase correspondiente.

## Tablas del núcleo

### `personas`

Representa los datos de una persona. `tipo_persona` es una clasificación de negocio y no sustituye roles, permisos ni autorización.

### `usuarios`

Representa la identidad de acceso asociada a una persona. La Fase 06 confirmó `login VARCHAR(30)` como clave primaria y `passwd VARCHAR(255)` como columna reservada para un hash seguro. La Fase 07 introdujo BCrypt mediante `PasswordEncoder`; nunca se almacena texto plano ni se expone o registra el hash.

### `roles`

Representa agrupaciones asignables a usuarios. La Fase 08 implementó `codr` como identidad, `nombre` único y `estado` como `SMALLINT` (`1` activo, `0` inactivo).

### Relación Usuario–Rol

La tabla `rolusu` materializa la relación de muchos a muchos entre usuarios y roles. Usa la clave primaria compuesta `(login, codr)`, conserva `fecha_asignacion`, elimina asignaciones al eliminar físicamente un Usuario y restringe la eliminación de un Rol asignado.

### `sesiones_usuario`

V6 incorpora sesiones persistentes. `sid UUID` es PK y `login VARCHAR(30)` referencia `usuarios(login)` con `ON DELETE CASCADE`. Solo existe una fila activa por `(login, device_id)` mediante índice único parcial. La fila conserva cliente WEB/MOBILE, fechas UTC, versión, revocación y exclusivamente el hash SHA-256 del refresh token; nunca el token original.

## Relaciones confirmadas

- Una persona puede tener como máximo un usuario.
- Un usuario puede tener varios roles.
- Un rol puede pertenecer a varios usuarios.
- Un usuario puede tener varias sesiones, con máximo una activa por dispositivo.
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

La Fase 04 completó `personas` con V1 y V2; la Fase 06 completó `usuarios` con V3; la Fase 08 completó `roles` y `rolusu` con V4 y V5; la subfase 10.1 completó `sesiones_usuario` con V6. No se creó V7 ni tablas de fases futuras.
