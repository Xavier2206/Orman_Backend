# Fase 11.2 — Matriz de autorización y protección del propietario

## Estado y objetivo

Implementación completada el 2026-08-06; estado documental actual: `EN DESARROLLO` hasta finalizar la validación manual de Postman. Esta subfase aplica autorización real a autenticación/sesiones, Personas, Usuarios, Roles y asignaciones Usuario–Rol. La Fase 12 no se inició.

La decisión combina cuatro dimensiones: Rol, módulo, tarea y alcance comprobable. Como todavía no existen propiedades ni una relación Administrador–Propiedad, no se simula alcance territorial, por ciudad o inmueble.

## Precondición operativa

El despliegue necesita un Rol activo con nombre exacto `PROPIETARIO`, al menos una asignación a un Usuario activo y una Persona asociada activa. Durante la implementación, la base local contenía `ABOGADO`, `ADMINISTRADOR` e `INQUILINO` activos, pero no contenía `PROPIETARIO` ni propietarios activos.

No se insertaron datos ni se creó una migración. Antes de ejecutar la versión protegida en ese entorno se debe usar la API de la versión anterior para crear o activar Persona y Usuario, crear el Rol exacto, asignarlo y comprobar un login nuevo. No se usa `tipo_persona` para identificar propietarios.

Preparación manual, sin valores reales en este documento:

1. Con la versión 11.1 en ejecución, crear o activar una Persona mediante `/api/v1/personas`.
2. Crear o activar su Usuario mediante `/api/v1/usuarios`.
3. Crear `{"nombre":"PROPIETARIO","estado":1}` mediante `POST /api/v1/roles`.
4. Asignar el código devuelto con `POST /api/v1/usuarios/{login}/roles/{codr}`.
5. Iniciar sesión nuevamente y comprobar que la siguiente petición carga `ROLE_PROPIETARIO`.
6. Solo entonces desplegar la matriz 11.2.

Si 11.2 ya fue desplegada sin propietario, los endpoints administrativos quedan correctamente cerrados; la recuperación requiere coordinación administrativa sobre los datos, fuera de esta ejecución y sin modificar el esquema ni Flyway.

## Jerarquía

- `PROPIETARIO`: control de todos los módulos actuales, delegación y administración de otros propietarios, sujeto al mínimo de uno activo.
- `ADMINISTRADOR`: operación sobre Personas y Usuarios comunes; no administra Roles, asignaciones ni objetivos propietarios.
- `INQUILINO`: sesiones propias y cambio de su propia contraseña. Los datos propios de contratos, pagos o propiedades pertenecen a fases futuras.
- Usuario sin Roles: conserva login, refresh, sesiones propias y cambio de contraseña propia; no accede a CRUD administrativos.

## Matriz por endpoint

| Endpoint | PROPIETARIO | ADMINISTRADOR | INQUILINO/sin Rol |
|---|---|---|---|
| `POST /api/v1/auth/login` | Público | Público | Público |
| `POST /api/v1/auth/refresh` | Público | Público | Público |
| `POST /api/v1/auth/logout` | Propio | Propio | Propio |
| `POST /api/v1/auth/logout-all` | Propio | Propio | Propio |
| `GET /api/v1/auth/sessions` | Propias | Propias | Propias |
| `DELETE /api/v1/auth/sessions/{sid}` | Propia | Propia | Propia |
| `POST /api/v1/personas` | Sí | Sí | No |
| `GET /api/v1/personas` | Todas | Listado disponible | No |
| `GET /api/v1/personas/{codper}` | Cualquiera | Solo común | No |
| `PUT /api/v1/personas/{codper}` | Cualquiera | Solo común | No |
| `PATCH .../activar` | Cualquiera | Solo común | No |
| `PATCH .../desactivar` | Sí, con mínimo | Solo común | No |
| `DELETE /api/v1/personas/{codper}` | Sí, con mínimo | Solo común | No |
| `POST /api/v1/usuarios` | Sí | Usuario inicialmente común | No |
| `GET /api/v1/usuarios` | Todos | Solo comunes | No |
| `GET /api/v1/usuarios/{login}` | Cualquiera | Solo común | No |
| `PUT/PATCH /api/v1/usuarios/{login}` | Cualquiera | Solo común | No |
| `PUT /api/v1/usuarios/{login}/password` | Cualquiera | Solo propia | Solo propia |
| Todas las rutas actuales de `/api/v1/roles` | Sí | No | No |
| Rutas Usuario–Rol | Sí | No | No |

Una Persona o un Usuario se consideran objetivo propietario para la restricción del ADMINISTRADOR cuando existe una asignación a un Rol activo llamado exactamente `PROPIETARIO`. Esto protege también la reactivación de una identidad propietaria actualmente inactiva.

## Matriz HTTP de contratos reales

La siguiente matriz se obtuvo de los controladores actuales. En listados paginados, los query parameters son `page`, `size` y `sort` (Spring `Pageable`). Las respuestas JSON son DTO/`PageResponse`; las operaciones de activación/desactivación y cambio de contraseña conservan los códigos indicados en las guías Postman.

| Método y ruta | Auth/regla | Body/parámetros | Éxito | Errores relevantes |
|---|---|---|---:|---|
| `POST /api/v1/auth/login` | Público | `login`, `password`, `deviceId`, `deviceName`, `clientType` (`WEB`/`MOBILE`) | 200 | 400 `INVALID_REQUEST`, 401 credenciales/sesión |
| `POST /api/v1/auth/refresh` | Público; cookie+CSRF en WEB o JSON en MOBILE | WEB sin body; MOBILE `{"refreshToken":"..."}` | 200 | 400 `INVALID_REQUEST`, 401 `INVALID_REFRESH_TOKEN`, 403 `INVALID_REQUEST` por CSRF WEB |
| `POST /api/v1/auth/logout` | Autenticado, `sid` propio | Sin body | 204 | 401, 404 `RESOURCE_NOT_FOUND` |
| `POST /api/v1/auth/logout-all` | Autenticado | Sin body | 204 | 401 |
| `GET /api/v1/auth/sessions` | Autenticado | Sin body | 200 | 401 |
| `DELETE /api/v1/auth/sessions/{sid}` | Autenticado, sesión propia | `sid` de ruta | 204 | 401, 404 |
| `POST /api/v1/personas` | `PROPIETARIO` o `ADMINISTRADOR` | DTO de Persona | 201 | 400, 403, 409 |
| `GET /api/v1/personas` | `PROPIETARIO` o `ADMINISTRADOR`; `page,size,sort` | Query opcional | 200 | 401, 403 |
| `GET/PUT/PATCH/DELETE /api/v1/personas/{codper}` | `PROPIETARIO` o ADMIN sobre objetivo común | PUT DTO; PATCH/DELETE sin body; `codper` de ruta | 200/204 | 401, 403, 404, 409 `LAST_OWNER_REQUIRED` |
| `POST /api/v1/usuarios` | `PROPIETARIO` o `ADMINISTRADOR` (no crea propietario) | `login`, `password`, `estado`, `codper` | 201 | 400, 403, 404, 409 |
| `GET /api/v1/usuarios` | `PROPIETARIO` o `ADMINISTRADOR`; ADMIN lista comunes; `page,size,sort` | Query opcional | 200 | 401, 403 |
| `GET/PUT/PATCH /api/v1/usuarios/{login}` | `PROPIETARIO` o ADMIN sobre Usuario común | PUT `estado`; PATCH sin body; `login` de ruta | 200 | 401, 403, 404, 409 |
| `PUT /api/v1/usuarios/{login}/password` | Propio o `PROPIETARIO` | `{"newPassword":"..."}` | 204 | 400, 401, 403, 404 |
| `POST/GET/PUT/PATCH /api/v1/roles` y `/{codr}` | Solo `PROPIETARIO` | Crear/actualizar DTO; activar/desactivar sin body; listados `page,size,sort` | 200/201 | 400, 403, 404, 409 `LAST_OWNER_REQUIRED` |
| `POST/DELETE /api/v1/usuarios/{login}/roles/{codr}` | Solo `PROPIETARIO` | Parámetros de ruta, sin body | 201/204 | 403, 404, 409 `LAST_OWNER_REQUIRED` |
| `GET /api/v1/usuarios/{login}/roles` | Solo `PROPIETARIO` | `login` de ruta | 200 | 403, 404 |
| `GET /api/v1/roles/{codr}/usuarios` | Solo `PROPIETARIO` | `codr` de ruta | 200 | 403, 404 |

No existe `DELETE /api/v1/roles/{codr}`. Los nombres persistidos son `PROPIETARIO`, `ADMINISTRADOR` e `INQUILINO`; las authorities son `ROLE_<NOMBRE>` y nunca se guardan dentro del JWT.

## Seguridad por método

Los controladores usan `@PreAuthorize` para reglas simples y expresiones delegadas:

```java
hasRole('PROPIETARIO')
hasAnyRole('PROPIETARIO', 'ADMINISTRADOR')
@authorizationService.isSelfOrOwner(authentication, #login)
@authorizationService.canManageUser(authentication, #login)
@authorizationService.canManagePerson(authentication, #codper)
```

`AuthorizationService` interpreta exclusivamente el principal autenticado y authorities actuales; las consultas de objetivo permanecen fuera de SpEL. El listado de Usuarios usa una consulta paginada que excluye asignaciones activas a `PROPIETARIO` cuando el actor es ADMINISTRADOR.

## Defensa transaccional del propietario

Un propietario activo requiere simultáneamente asignación, Rol `PROPIETARIO` activo, Usuario activo y Persona activa. No cuentan Roles, Usuarios o Personas inactivos.

`OwnerProtectionService` se ejecuta dentro de las transacciones de Persona, Usuario, Rol y RolUsu. Antes de una operación reductora obtiene un `PESSIMISTIC_WRITE` sobre la fila `PROPIETARIO`. Esta fila funciona como mutex global y se bloquea siempre primero. Después se comprueba el objetivo, se cuenta a los propietarios activos y se modifica sin abandonar la transacción.

Se protegen:

- retiro de la asignación del último propietario;
- desactivación de su Usuario mediante `PATCH` o `PUT`;
- desactivación de su Persona mediante `PATCH` o `PUT`;
- eliminación de su Persona;
- renombrado o desactivación del Rol reservado.

El Rol `PROPIETARIO` no puede modificarse ni desactivarse. No existe eliminación física de Roles y no se añadió. Con dos propietarios activos puede desactivarse o retirarse uno. La prueba concurrente ejecuta dos desactivaciones paralelas y confirma que nunca quedan cero.

## Contratos de error

| Caso | HTTP | `errorCode` |
|---|---:|---|
| Sin autenticación, JWT o sesión inválida | 401 | Código de autenticación correspondiente |
| Autenticado sin Rol/facultad | 403 | `ACCESS_DENIED` |
| Recurso inexistente o sesión ajena | 404 | `RESOURCE_NOT_FOUND` |
| Operación dejaría cero propietarios o altera el Rol reservado | 409 | `LAST_OWNER_REQUIRED` |
| Entrada inválida | 400 | `VALIDATION_ERROR` o `INVALID_REQUEST` |

`LAST_OWNER_REQUIRED` devuelve el detalle `Debe permanecer al menos un propietario activo.`. Ninguna respuesta expone authorities internas, contraseñas, hashes, JWT, SQL o trazas.

## Sesiones, JWT, WEB y MOBILE

La propiedad de sesiones permanece sin cambios: ningún actor administra sesiones ajenas. Los cambios de Rol no revocan sesiones y se reflejan con el mismo JWT en la siguiente petición. Desactivar Usuario/Persona y cambiar contraseña mantienen las revocaciones de Fase 10.

El JWT conserva solo `sub`, `sid`, `iss`, `iat`, `exp`. CORS, CSRF, cookie WEB HttpOnly, `X-XSRF-TOKEN` y refresh MOBILE JSON no cambiaron.

## Pruebas manuales mínimas

Use datos ficticios, variables Postman y tres Usuarios separados:

1. Iniciar sesión como PROPIETARIO.
2. Iniciar sesión como ADMINISTRADOR.
3. Iniciar sesión como INQUILINO.
4. Confirmar acceso del PROPIETARIO a Personas.
5. Confirmar operación del ADMINISTRADOR sobre Persona común.
6. Intentar modificar Persona propietaria como ADMINISTRADOR y esperar `403 ACCESS_DENIED`.
7. Listar Personas como INQUILINO y esperar `403 ACCESS_DENIED`.
8. Cambiar la contraseña propia y comprobar revocación de sesiones.
9. Cambiar contraseña ajena como ADMINISTRADOR y esperar 403.
10. Cambiar contraseña ajena como PROPIETARIO.
11. Administrar un Rol común como PROPIETARIO.
12. Consultar Roles como ADMINISTRADOR y esperar 403.
13. Asignar ADMINISTRADOR como PROPIETARIO.
14. Intentar asignarse PROPIETARIO como ADMINISTRADOR y esperar 403.
15. Retirar al último PROPIETARIO y esperar `409 LAST_OWNER_REQUIRED`.
16. Asignar un segundo PROPIETARIO y retirar/desactivar uno.
17. Decodificar el JWT y confirmar ausencia de Roles.
18. Asignar/retirar Rol y repetir con el mismo JWT para observar el cambio inmediato.
19. Repetir login/refresh WEB y MOBILE con sus contratos originales.
20. Revisar respuestas y evidencias para confirmar ausencia de secretos.

Estas pruebas manuales están pendientes de ejecución. La auditoría local detectó que la base no cumple la precondición de propietario; no se deben insertar datos permanentes por Flyway ni ejecutar pruebas destructivas sobre datos reales.

## Validación automatizada

Se añadieron pruebas unitarias, MVC, method security, persistencia e integración PostgreSQL para la matriz, alcance de objetivo, consultas, bloqueo, último propietario, concurrencia, sesiones, revocaciones, cambios inmediatos y respuestas seguras.

Resultado final de `./mvnw.cmd clean test`: **BUILD SUCCESS**; 165 pruebas, 0 fallos, 0 errores y 0 omitidas.

## Migraciones y exclusiones

Flyway permanece en V6; V1–V6 no se modificaron y no existe V7. No se incorporaron propiedades, ciudades, sucursales, contratos, cobros, pagos, permisos dinámicos, menús, procesos, caché, auditoría, OTP, Angular o Flutter. El futuro módulo de propiedades deberá representar explícitamente la asignación Administrador–Propiedad antes de aplicar ese alcance.
