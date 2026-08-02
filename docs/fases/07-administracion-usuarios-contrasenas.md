# Fase 07 — Administración de Usuarios y Contraseñas

## Estado final

`COMPLETADA` el 2026-08-02. Se implementó la administración REST de Usuarios asociados a Personas existentes, con contraseñas almacenadas exclusivamente mediante BCrypt. No se modificó el esquema ni se inició la Fase 08.

## Objetivo y alcance

La fase incorpora `UsuarioController`, DTO, mapper, `UsuarioService`, `UsuarioServiceImpl`, BCrypt mediante `PasswordEncoder`, listado paginado, consulta por login, creación, actualización administrativa de estado, activación, desactivación y cambio de contraseña.

Quedan excluidos login/autenticación, JWT, refresh token, sesiones, logout, roles, permisos, autorización, OTP y Spring Security HTTP completo.

## Arquitectura y contrato

El módulo `user` contiene `controller`, `dto`, `entity`, `mapper`, `repository` y `service`. Reutiliza `PageResponse<T>` desde `common.dto`; el contrato de Persona conserva el mismo formato paginado.

| Método | Ruta | Resultado |
|---|---|---|
| POST | `/api/v1/usuarios` | Crea Usuario, `201 Created` y `Location`. |
| GET | `/api/v1/usuarios/{login}` | Consulta un Usuario. |
| GET | `/api/v1/usuarios` | Lista paginada por `login` ascendente. |
| PUT | `/api/v1/usuarios/{login}` | Actualiza únicamente `estado`. |
| PATCH | `/api/v1/usuarios/{login}/desactivar` | Desactiva de forma idempotente. |
| PATCH | `/api/v1/usuarios/{login}/activar` | Activa de forma idempotente. |
| PUT | `/api/v1/usuarios/{login}/password` | Reemplaza contraseña y responde `204`. |

No existe `DELETE` físico. La eliminación funcional es la desactivación (`estado = 0`).

## DTO y validaciones

- `CreateUsuarioRequest`: `login`, `password`, `estado` opcional y `codper`.
- `UpdateUsuarioRequest`: solo `estado`; `login` y `codper` permanecen inmutables porque el plan no autorizó reasignar Persona.
- `ChangePasswordRequest`: solo `newPassword`.
- `UsuarioResponse`: `login`, `estado`, `codper`, `fechaCreacion` y `ultimoAcceso`.

El login se recorta antes de persistir, conserva mayúsculas/minúsculas y la PK garantiza su unicidad. No se recorta ni transforma contraseña. Login admite hasta 30 caracteres; contraseña requiere entre 8 y 72 caracteres; `codper` debe ser positivo; estado solo admite 0 o 1.

## Seguridad de contraseñas

Se añadió `org.springframework.security:spring-security-crypto`, con versión gestionada por Spring Boot. `PasswordEncodingConfig` expone `BCryptPasswordEncoder` como `PasswordEncoder` con costo seguro por defecto de la biblioteca (10). No se añadió `spring-boot-starter-security`, filtros, `SecurityFilterChain`, `AuthenticationManager` ni configuración HTTP de seguridad.

El hash se genera en el servicio antes de persistir y el mapper no recibe ni genera hashes. Ninguna contraseña, hash o campo `passwd` se devuelve, registra o incluye en mensajes de error.

## Reglas y conflictos

La Persona debe existir y solo puede tener un Usuario. Se validan previamente login y Persona, y las condiciones de carrera de `pk_usuarios` y `uk_usuarios_codper` se traducen de forma segura a `409 CONFLICT`, sin revelar SQL ni nombres de constraints. Usuario o Persona inexistente devuelve `404 RESOURCE_NOT_FOUND`; Bean Validation devuelve `400 VALIDATION_ERROR`.

Activar o desactivar Usuario no cambia Persona. La futura autenticación exigirá ambos estados activos y contraseña válida, pero esa lógica no forma parte de esta fase.

## Transacciones y pruebas

Las escrituras usan `@Transactional`; consulta y listado usan `@Transactional(readOnly = true)`. Se crearon pruebas de mapper, servicio con mocks, MVC e integración real contra PostgreSQL. Las pruebas validan BCrypt, inexistentes, conflictos, paginación, inmutabilidad de login, ausencia de secretos y conservación del registro al desactivar.

Comando ejecutado:

```powershell
.\mvnw.cmd clean test
```

Resultado: **BUILD SUCCESS**; 83 pruebas, 0 fallos, 0 errores y 0 omitidas. PostgreSQL 17.6 conectó; Flyway validó V1–V3 sin migraciones nuevas; Hibernate validó el esquema con `ddl-auto=validate`; `contextLoads` pasó.

## Archivos y decisiones

Se crearon el módulo REST de Usuario, configuración BCrypt, cuatro DTO, mapper, servicio, tres clases de prueba, guía Postman y este documento. Se movió `PageResponse<T>` a `common.dto` para reutilizarlo sin duplicarlo.

No se crearon ni modificaron migraciones, tablas o constraints. No se implementaron autenticación, JWT, sesiones, roles ni autorización. La siguiente fase autorizable es la Fase 08 — Roles y relación Usuario–Rol; no se inició. El usuario debe revisar los cambios y ejecutar Git manualmente.
