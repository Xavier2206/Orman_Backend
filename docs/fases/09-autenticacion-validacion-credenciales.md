# Fase 09 — Autenticación y validación de credenciales

## Estado final

`COMPLETADA` el 2026-08-03. La fase implementa la validación inicial de credenciales con BCrypt, sin JWT, sesiones, logout ni autorización.

## Alcance implementado

Se agregó `POST /api/v1/auth/login`. El inicio de sesión es válido únicamente cuando existe el Usuario, su Persona asociada existe y está activa, el Usuario está activo y la contraseña coincide mediante `PasswordEncoder.matches`.

El contrato de éxito es `200 OK`:

```json
{
  "login": "usuario.demo",
  "codper": 1
}
```

La respuesta no expone contraseña, `passwd`, hash BCrypt, Persona completa, información personal, Roles, authorities, tokens, sesiones ni datos de dispositivo.

## Seguridad y errores

Usuario inexistente, contraseña incorrecta, Usuario inactivo, Persona inactiva y relación Persona inconsistente responden el mismo `401 Unauthorized` con `application/problem+json`, título `Credenciales inválidas`, detalle `Las credenciales no son válidas.` y código `INVALID_CREDENTIALS`.

Para reducir enumeración de Usuarios, cuando el login no existe se ejecuta `PasswordEncoder.matches` contra un hash BCrypt señuelo constante interno. El hash no se genera por solicitud, no se registra, no se devuelve y no representa una credencial real.

El login se recorta antes de buscarse y conserva mayúsculas/minúsculas. La contraseña se valida entre 8 y 72 caracteres y se compara exactamente como llega; nunca se recorta ni transforma.

## Arquitectura y persistencia

Se creó el módulo `auth` con controller, DTO de entrada/salida, servicio, implementación y excepción específica. Reutiliza exclusivamente `UsuarioRepository`; la Persona se accede mediante la relación existente dentro de la transacción. Roles y `rolusu` no se cargan ni participan en el login: un Usuario sin Roles puede autenticarse.

`AuthServiceImpl` es transaccional. Tras validar todas las reglas, actualiza `usuarios.ultimo_acceso` con `LocalDateTime.now(clock)` usando un `Clock` UTC inyectado. Un error al persistir revierte la transacción y no devuelve autenticación exitosa.

No se creó ni modificó esquema, tabla, columna, constraint o migración. Flyway conserva V1–V5 y Hibernate permanece en `ddl-auto=validate`.

## Exclusiones confirmadas

No se implementaron JWT, access token, refresh token, `sesiones_usuario`, `sid`, logout, cookies, revocación, dispositivos, IP, user-agent, filtros, `SecurityFilterChain`, `AuthenticationManager`, `UserDetailsService`, autorización, authorities, permisos, menús, procesos u OTP.

## Validación

La implementación incorpora pruebas unitarias, MVC e integración contra PostgreSQL real para login correcto, BCrypt, estados, normalización del login, preservación de password, ausencia de Roles, Roles activos/inactivos, `ultimo_acceso`, errores seguros, validaciones, JSON inválido y ausencia de datos sensibles.

Se ejecutó `./mvnw.cmd clean test`: **BUILD SUCCESS**; 114 pruebas, 0 fallos, 0 errores y 0 omitidas. PostgreSQL 17.6 conectó, Flyway validó V1–V5 sin migraciones nuevas y Hibernate validó el esquema.

## Archivos

Se crearon el módulo `auth`, `ClockConfig`, pruebas de autenticación, esta documentación y la guía Postman. Se actualizaron `ErrorCode`, `GlobalExceptionHandler`, plan, índices, README, arquitectura y CHANGELOG.

La siguiente fase autorizable es la Fase 10 — JWT y control de sesiones. No se inició en esta fase.
