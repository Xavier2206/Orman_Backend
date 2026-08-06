# Fase 11.1 — Carga de Roles activos y base de autorización

## Estado

`COMPLETADA` el 2026-08-06. La Fase 11 global permanece `EN DESARROLLO`; la matriz concreta de la subfase 11.2 no se inició.

## Objetivo y alcance

Esta subfase conecta los Roles persistidos en `roles` y `rolusu` con las authorities de Spring Security. Una petición protegida sigue esta secuencia:

```text
JWT válido
  -> sesión válida
  -> Usuario activo
  -> Persona activa
  -> Roles activos consultados en PostgreSQL
  -> authorities actuales
  -> comprobación de autorización
```

Autenticación responde quién es el usuario y si su sesión es válida. Autorización decide si esa identidad autenticada posee las authorities requeridas para una operación. La cadena HTTP continúa exigiendo solo autenticación de manera general; la matriz de acceso por módulos pertenece a 11.2.

## Consulta de Roles activos

`RolUsuRepository.findActiveRoleNamesByLogin` ejecuta una consulta JPQL escalar equivalente a:

```sql
SELECT DISTINCT r.nombre
FROM rolusu ru
JOIN roles r ON r.codr = ru.codr
WHERE ru.login = :login
  AND r.estado = 1
ORDER BY r.nombre;
```

La consulta devuelve solo nombres, usa un `INNER JOIN`, evita cargar entidades completas y N+1, elimina duplicados en base de datos y excluye Roles inactivos. Las claves foráneas existentes impiden asignaciones huérfanas persistentes.

## Conversión a authorities

`UserAuthorityService` recibe el login y devuelve una lista inmutable de `GrantedAuthority`. La implementación:

- descarta valores nulos, vacíos y el valor aislado `ROLE_`;
- aplica `trim` y mayúsculas con `Locale.ROOT`;
- conserva un prefijo `ROLE_` ya presente y no lo duplica;
- antepone `ROLE_` a cualquier otro nombre activo;
- elimina duplicados después de normalizar y aplica orden estable.

Ejemplos: `PROPIETARIO` se convierte en `ROLE_PROPIETARIO`, `ADMINISTRADOR` en `ROLE_ADMINISTRADOR` e `INQUILINO` en `ROLE_INQUILINO`. La regla es general y no limita los Roles a esos tres nombres.

## Integración con autenticación

`JwtAuthenticationFilter` conserva el flujo de Fase 10: valida el Bearer JWT y delega en `SessionService.authenticate(login, sid)` la comprobación de sesión, Usuario y Persona. Solo después carga las authorities actuales y crea:

```text
principal   = AuthenticatedUser(login, sid)
credentials = null
authorities = colección inmutable de Roles activos
```

`AuthenticatedUser` continúa conteniendo únicamente `login` y `sid`. No almacena contraseña, Persona, refresh token, hash ni Roles duplicados. No se crean sesiones HTTP y `SessionCreationPolicy.STATELESS` permanece activo.

## Roles fuera del JWT y cambios inmediatos

El JWT mantiene exactamente `sub`, `sid`, `iss`, `iat` y `exp`. No contiene Roles, authorities, permisos, Persona, menús ni procesos.

Los Roles se leen de PostgreSQL en cada petición protegida. Por eso, después de confirmar la transacción correspondiente:

- una asignación nueva aparece en la siguiente petición;
- una asignación retirada desaparece en la siguiente petición;
- desactivar un Rol conserva `rolusu`, pero deja de conceder authority;
- reactivar el Rol vuelve a conceder authority a sus asignaciones existentes.

Ninguno de estos cambios revoca sesiones ni obliga a renovar o sustituir el access token. No se añadió caché.

## Usuario sin Roles

Un Usuario sin Roles activos mantiene un `Authentication` válido con authorities vacías. Puede usar las operaciones propias de autenticación y sesiones habilitadas en Fase 10; la ausencia de Roles no bloquea login ni refresh.

## Seguridad por métodos y errores

`@EnableMethodSecurity` habilita expresiones como:

```java
@PreAuthorize("hasRole('PROPIETARIO')")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'ADMINISTRADOR')")
```

No se agregaron esas reglas a controladores de negocio en esta subfase. Una identidad autenticada que no cumple una regla por método recibe `403 Forbidden`, `application/problem+json`, `errorCode=ACCESS_DENIED` y el detalle seguro `No tiene autorización para realizar esta operación.`. Un JWT inválido, una sesión inválida o la falta de autenticación continúan usando `401`.

El error CSRF de Fase 10 conserva su contrato separado `403 INVALID_REQUEST`; CORS, CSRF, cookies WEB y refresh MOBILE no se debilitaron ni cambiaron.

## Archivos principales

- `auth/service/UserAuthorityService.java`: contrato de carga de authorities.
- `auth/service/impl/UserAuthorityServiceImpl.java`: normalización y colección inmutable.
- `role/repository/RolUsuRepository.java`: consulta escalar de Roles activos.
- `auth/security/JwtAuthenticationFilter.java`: carga por petición y creación de `Authentication`.
- `auth/config/SecurityConfig.java`: habilitación de method security.
- `common/error/ErrorCode.java` y `GlobalExceptionHandler.java`: respuesta `ACCESS_DENIED`.

`SessionService`, `AuthenticatedUser`, el contenido del JWT, CORS, CSRF y las rutas de `SecurityFilterChain` conservaron sus responsabilidades y contratos.

## Pruebas y validación

Se incorporaron pruebas unitarias del servicio, MVC del `ProblemDetail`, verificación de claims JWT e integración con PostgreSQL real. La integración usa un servicio de prueba protegido por `@PreAuthorize`; no crea endpoints de producción.

Cobertura específica:

- Roles principales, múltiples Roles, deduplicación, normalización y colección inmutable;
- Rol inactivo, nombre vacío/inconsistente y Usuario sin Roles;
- asignación, retiro, desactivación y reactivación reflejados con el mismo JWT y la misma sesión;
- sesión no revocada por cambios de Rol;
- JWT sin Roles;
- ruta pública, ruta autenticada, `401` y `403 ACCESS_DENIED` sin datos sensibles.

Validación final: `./mvnw.cmd clean test` terminó con **BUILD SUCCESS** y 149 pruebas, 0 fallos, 0 errores y 0 omitidas contra PostgreSQL real. Flyway validó V1–V6 y mantuvo el esquema en versión 6; Hibernate permaneció con `ddl-auto=validate`. No se creó V7 ni se modificaron migraciones existentes.

## Exclusiones y pendientes

No se implementaron matriz de Personas, Usuarios, Roles o asignaciones; protección especial del propietario; último propietario; alcance por propiedades; permisos dinámicos; menús; procesos; auditoría; OTP; caché; Roles en JWT; ni código Angular o Flutter. Estas exclusiones permanecen pendientes de sus fases expresamente autorizadas.
