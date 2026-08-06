# Fase 10.2 — Seguridad HTTP, filtro JWT y administración de sesiones

## Estado

`COMPLETADA` el 2026-08-04. Con este cierre, la Fase 10 global queda completada. La Fase 11 no fue iniciada.

## Objetivo y alcance

La subfase activa la infraestructura de 10.1 como autenticación HTTP real. Cada petición protegida usa `Authorization: Bearer <accessToken>` y valida firma HS256, issuer, expiración, `sub`, `sid`, existencia y pertenencia de la sesión, revocación, expiración y estados activos de Usuario y Persona.

La identidad interna es `AuthenticatedUser(login, sid)`. No contiene Persona, Roles, permisos ni authorities reales. No se implementaron `hasRole`, `hasAuthority`, menús, procesos, `rolme`, `mepro` u OTP.

## Cadena y filtro

`SecurityFilterChain` es stateless y no crea sesiones HTTP. Permite `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, preflight CORS y dispatch interno de error. Cualquier otra ruta exige autenticación. Se excluyó el `UserDetailsService` automático porque la autenticación procede exclusivamente de la sesión persistente asociada al JWT.

`JwtAuthenticationFilter` valida Bearer y delega la comprobación persistente al servicio de sesiones. Si la validación termina correctamente crea un `UsernamePasswordAuthenticationToken` autenticado, con `AuthenticatedUser` como principal y una colección vacía de authorities. Los fallos se resuelven mediante el mismo `GlobalExceptionHandler`, por lo que nunca se devuelve HTML.

## Sesiones y revocación

- `POST /api/v1/auth/logout`: revoca el `sid` actual con `LOGOUT`, elimina lógicamente la cookie refresh y responde 204.
- `POST /api/v1/auth/logout-all`: revoca todas las sesiones activas propias con `LOGOUT_ALL` y responde 204.
- `GET /api/v1/auth/sessions`: lista solo sesiones activas, no expiradas y propias; marca el `sid` actual sin exponer refresh ni hash.
- `DELETE /api/v1/auth/sessions/{sid}`: revoca una sesión propia con `ADMIN_REVOKED`; un `sid` inexistente o ajeno produce el mismo 404 seguro.
- Cambio de contraseña: revoca con `PASSWORD_CHANGED` después de persistir el nuevo BCrypt.
- Usuario inactivo: revoca con `USER_DISABLED` tanto por PUT de estado como por endpoint de desactivación.
- Persona inactiva: si tiene Usuario, revoca con `PERSON_DISABLED` tanto por PUT como por endpoint de desactivación.

Todas las operaciones son transaccionales. Activar nuevamente Usuario o Persona no elimina marcas de revocación ni restaura sesiones.

## Errores

Los fallos HTTP usan `application/problem+json` con `type`, `title`, `status`, `detail`, `errorCode`, `timestamp`, `traceId` e `instance`.

| Causa | HTTP | `errorCode` |
|---|---:|---|
| Token ausente, mal formado, manipulado, issuer/sub/sid inválido o cuenta inactiva | 401 | `INVALID_TOKEN` |
| Access token expirado | 401 | `TOKEN_EXPIRED` |
| Sesión revocada | 401 | `SESSION_REVOKED` |
| Sesión expirada | 401 | `SESSION_EXPIRED` |
| Sesión a revocar inexistente o ajena | 404 | `RESOURCE_NOT_FOUND` |

No se exponen tokens, hashes, contraseñas, SQL, constraints ni stack traces.

## CORS y CSRF

CORS permite credenciales únicamente desde `security.cors.allowed-origins`; `ORMAN_FRONTEND_URL` configura el origen Angular predeterminado y `*` es rechazado. Los métodos permitidos son GET, POST, PUT, PATCH, DELETE y OPTIONS; los headers admitidos son Authorization, Content-Type y X-XSRF-TOKEN.

CSRF no se desactiva globalmente. Se exige doble envío cookie/header en `POST /api/v1/auth/refresh` cuando existe la cookie HttpOnly de refresh WEB, porque esa credencial sí puede ser adjuntada automáticamente por el navegador. Login genera la cookie `XSRF-TOKEN` y expone el valor en el header CORS `X-XSRF-TOKEN`; Angular conserva temporalmente ese valor y lo reenvía con el mismo nombre. Los endpoints Bearer y el refresh MOBILE no requieren CSRF: sus credenciales se envían explícitamente y no son cookies ambientales.

## Clientes

Angular conserva el access token temporalmente, usa `Authorization: Bearer`, llama login/refresh con credenciales, no puede leer la cookie refresh HttpOnly y reenvía el token XSRF. Flutter envía el mismo header Bearer, renueva mediante JSON en `POST /api/v1/auth/refresh` y guarda el refresh en almacenamiento seguro. No se creó código Angular o Flutter.

## Archivos y esquema

Se agregaron configuración de seguridad/CORS, filtro, principal, excepciones, servicio/DTO/mapper de sesiones y pruebas HTTP. Se modificaron auth, repositorios, servicios de Usuario/Persona, configuración, errores y documentación. Flyway permanece en V6; no se modificó ninguna migración ni se creó una tabla.

## Validación

Las pruebas cubren JWT válido/expirado/manipulado/issuer incorrecto; token ausente o inválido; sesión revocada/expirada; Usuario/Persona inactivos; logout y motivos; logout-all; listado propio sin secretos; revocación propia/inexistente/ajena; cambio de contraseña y desactivaciones; CORS, CSRF y ProblemDetail.

`./mvnw.cmd clean test` finalizó con `BUILD SUCCESS`: 141 pruebas, 0 fallos, 0 errores y 0 omitidas. PostgreSQL 17.6 estuvo conectado, Flyway validó seis migraciones y confirmó el esquema en V6, e Hibernate inició con `ddl-auto=validate`.

Una ejecución previa tuvo 1 fallo de fixture: cambiar el último carácter Base64 podía conservar los mismos bytes de firma por bits no significativos. Se corrigió la prueba para alterar el primer carácter de la firma; no se modificó ni debilitó la validación productiva, y la repetición completa fue exitosa.

## Riesgos y pendientes

- Producción requiere HTTPS, cookie `Secure=true`, origen Angular exacto y secreto JWT administrado externamente.
- El listado excluye sesiones expiradas pero no las elimina; la limpieza operativa futura necesita autorización independiente.
- La autorización por Roles pertenece exclusivamente a Fase 11 y permanece pendiente.
