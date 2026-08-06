# Plan general de ORMAN-BACKEND

## Objetivo general

Construir de forma incremental un backend mantenible para ORMAN, comenzando por la gestión de personas y usuarios, e incorporando posteriormente roles, autenticación, autorización, calidad y preparación para producción. La solución parte como un monolito modular con Java 21, Spring Boot, Maven y PostgreSQL.

## Forma de trabajo

El desarrollo se divide en etapas temáticas y fases acotadas. Solo una fase autorizada puede estar activa; cada fase debe documentar su alcance, cambios, validaciones y pendientes. La teoría transversal se mantiene en el documento de su etapa. Al cerrar una fase se actualizan este plan, su documento individual y `CHANGELOG.md`.

No se adelantan código, tablas, migraciones, dependencias o funcionalidades de una fase futura. Las fases 00 a 10 están cerradas; la Fase 11 permanece pendiente de autorización expresa.

## Estados permitidos

| Estado | Significado |
|---|---|
| `PENDIENTE` | Aún no iniciada. |
| `EN ANÁLISIS` | Se están confirmando alcance, requisitos o decisiones. |
| `EN DESARROLLO` | La fase está autorizada y en ejecución. |
| `BLOQUEADA` | Existe un impedimento que evita continuar o cerrar. |
| `COMPLETADA` | Cumplió su objetivo, validaciones y documentación. |
| `CANCELADA` | Se decidió no continuar con la fase. |

## Estado actual

- Etapa actual: **ETAPA 3 — Autenticación, sesiones y autorización**.
- Última subfase completada: **Fase 10.2 — Seguridad HTTP, filtro JWT y administración de sesiones**.
- Siguiente fase autorizable: **Fase 11**, únicamente mediante autorización expresa.
- Estado de la Fase 09: **COMPLETADA**.
- Estado de la Fase 10 global: **COMPLETADA**; 10.1 y 10.2 están cerradas.
- Resultado de Fase 10: sesiones por dispositivo, JWT HS256, refresh rotatorio, autenticación HTTP stateless, logout y administración de sesiones sobre Flyway V6.
- Fecha de actualización: **2026-08-04**.

## Etapas y fases previstas

### ETAPA 1 — Fundación técnica

Teoría: [Etapa 1 — Fundación técnica](etapas/etapa-01-fundacion-tecnica.md)

| Fase | Estado | Dependencia | Documento |
|---|---|---|---|
| 00 — Planificación general y estructura documental | `COMPLETADA` | Ninguna | [Documento de Fase 00](fases/00-planificacion-general.md) |
| 01 — Revisión y normalización del proyecto Spring Boot | `COMPLETADA` | Fase 00 | [Documento de Fase 01](fases/01-revision-normalizacion-spring-boot.md) |
| 02 — Configuración de PostgreSQL y Flyway | `COMPLETADA` | Fase 01 | [Documento de Fase 02](fases/02-configuracion-postgresql-flyway.md) |
| 03 — Infraestructura común y manejo de errores | `COMPLETADA` | Fases 01 y 02 | [Documento de Fase 03](fases/03-infraestructura-comun-manejo-errores.md) |

### ETAPA 2 — Personas, usuarios y roles

Teoría: [Etapa 2 — Personas, usuarios y roles](etapas/etapa-02-personas-usuarios-roles.md)

| Fase | Estado | Dependencia | Documento |
|---|---|---|---|
| 04 — Modelo y migración de Persona | `COMPLETADA` | Fases 02 y 03 | [Documento de Fase 04](fases/04-modelo-migracion-persona.md) |
| 05 — API CRUD de Persona | `COMPLETADA` | Fase 04 | [Documento de Fase 05](fases/05-crud-persona.md) |
| 06 — Modelo y migración de Usuario | `COMPLETADA` | Fases 04 y 05 | [Documento de Fase 06](fases/06-modelo-migracion-usuario.md) |
| 07 — Administración de Usuarios y Contraseñas | `COMPLETADA` | Fase 06 | [Documento de Fase 07](fases/07-administracion-usuarios-contrasenas.md) |
| 08 — Roles y relación Usuario–Rol | `COMPLETADA` | Fases 06 y 07 | [Documento de Fase 08](fases/08-roles-relacion-usuario-rol.md) |

### ETAPA 3 — Autenticación, sesiones y autorización

Teoría: pendiente de creación cuando corresponda.

| Fase | Estado | Dependencia | Documento |
|---|---|---|---|
| 09 — Autenticación y validación de credenciales | `COMPLETADA` | Fases 07 y 08 | [Documento de Fase 09](fases/09-autenticacion-validacion-credenciales.md) |
| 10 — JWT y control de sesiones | `COMPLETADA` | Fase 09 | [Subfase 10.1](fases/10-1-sesiones-jwt-refresh.md); [Subfase 10.2](fases/10-2-seguridad-sesiones.md) |
| 11 — Autorización por roles | `PENDIENTE` | Fases 08 y 10 | Documento pendiente de creación |
| 12 — Menús y procesos dinámicos | `PENDIENTE` | Fase 11 | Documento pendiente de creación |
| 13 — OTP y desafíos de autenticación | `PENDIENTE` | Fases 09 y 10 | Documento pendiente de creación |

### ETAPA 4 — Calidad y producción

Teoría: pendiente de creación cuando corresponda.

| Fase | Estado | Dependencia | Documento |
|---|---|---|---|
| 14 — Documentación OpenAPI | `PENDIENTE` | APIs principales estables | Documento pendiente de creación |
| 15 — Pruebas de integración ampliadas | `PENDIENTE` | Fases 07 a 13 | Documento pendiente de creación |
| 16 — Auditoría | `PENDIENTE` | Modelo y seguridad estables | Documento pendiente de creación |
| 17 — Preparación para producción | `PENDIENTE` | Fases 14, 15 y 16 | Documento pendiente de creación |

## Alcance aprobado de las fases futuras

### Fase 07 — Administración de Usuarios y Contraseñas

**Objetivo:** implementar la administración REST segura de Usuarios vinculados a Personas existentes, almacenando las contraseñas únicamente como hash BCrypt.

**Incluye:** creación de Usuario; consulta por login; listado paginado; actualización de datos administrativos permitidos; activación; desactivación; validación de Persona existente; un único Usuario por Persona; login duplicado; DTO, mapper, service, service.impl, controller, Bean Validation, `PasswordEncoder` con BCrypt; endpoint específico de cambio o restablecimiento de contraseña; pruebas unitarias, MVC e integración; documentación Postman; `ProblemDetail`.

**Reglas:** `login` es clave primaria y no se modifica mediante PUT; `passwd` no aparece en respuestas, logs ni PUT general; el hash tampoco se expone; crear y cambiar contraseña siempre aplica BCrypt; activar Usuario no activa Persona; desactivar Usuario no desactiva Persona.

**Excluye:** login/autenticación, JWT, refresh token, sesiones, roles, autorización, OTP y Spring Security HTTP completo.

**Dependencia:** Fase 06. **Resultado esperado:** Usuarios administrables por API y contraseñas almacenadas únicamente como BCrypt, sin autenticación todavía.

**Resultado:** completada el 2026-08-02 sin modificar el esquema. La actualización administrativa quedó limitada a `estado`; `login` y `codper` son inmutables. No existe eliminación física de Usuario.

BCrypt se introduce aquí mediante `PasswordEncoder`. Puede incorporarse `spring-security-crypto` sin activar `spring-boot-starter-security` ni el modelo HTTP completo.

### Fase 08 — Roles y relación Usuario–Rol

**Objetivo:** modelar y administrar roles y su asociación con Usuarios.

**Incluye:** tabla `roles`; tabla `rolusu`; migraciones V4 y V5; entidades; repositorios; administración REST de Roles; asignación y retiro de roles; validaciones y pruebas.

**Excluye:** login, JWT, sesiones, filtros de seguridad y autorización de endpoints.

**Dependencias:** Fases 06 y 07. `tipo_persona` sigue siendo clasificación de negocio y no sustituye roles.

**Resultado:** completada el 2026-08-02. `roles.codr` es la PK; `rolusu(login, codr)` materializa la relación N:M con `fecha_asignacion`, `ON DELETE CASCADE` desde Usuario y `ON DELETE RESTRICT` desde Rol. No se insertaron roles iniciales ni se implementó autorización.

### Fase 09 — Autenticación y validación de credenciales

**Objetivo:** validar el inicio de sesión mediante Usuario, Persona y BCrypt.

**Incluye:** endpoint de login; búsqueda de Usuario; validación BCrypt; validación de `usuarios.estado = 1` y `personas.estado = 1`; errores seguros de credenciales; actualización controlada de `ultimo_acceso` si se aprueba; pruebas.

**Excluye:** access token, refresh token, `sesiones_usuario`, logout, autorización por roles y OTP.

**Dependencias:** Fases 07 y 08. El Usuario solo podrá autenticarse cuando `personas.estado = 1`, `usuarios.estado = 1` y la contraseña sea válida.

### Fase 10 — JWT y control de sesiones

**Objetivo:** implementar autenticación basada en tokens y sesiones persistentes por dispositivo.

**Subfase 10.1 completada:** access token JWT; refresh token opaco; expiración; rotación y renovación; tabla `sesiones_usuario`; `sid`; hash SHA-256; login WEB/MOBILE; una sesión activa por `(login, device_id)`; reemplazo de la sesión del mismo dispositivo; pruebas.

**Subfase 10.2 completada el 2026-08-04:** filtro JWT, `SecurityFilterChain`, protección de endpoints, logout, logout-all, administración/revocación de sesiones, CORS/CSRF y revocación ante cambios de contraseña o desactivaciones.

**Excluye:** autorización por rol, OTP, menús y procesos. **Dependencia:** Fase 09.

Cada Usuario puede mantener varias sesiones activas, con una sola por combinación `(login, device_id)`. El JWT incluye `sid`; un nuevo login reemplaza únicamente la sesión activa del mismo dispositivo. `ultimo_acceso` no reemplaza `sesiones_usuario` y PostgreSQL almacena solo el hash del refresh token. Las revocaciones administrativas usan motivos específicos; reactivar Usuario o Persona no recupera sesiones.

### Fase 11 — Autorización por roles

**Objetivo:** proteger endpoints usando identidad autenticada y roles.

**Incluye:** reglas por rol; protección de endpoints; integración con roles; respuestas 401 y 403; pruebas de autorización.

**Excluye:** creación de roles, OTP, menús y procesos. **Dependencias:** Fases 08 y 10.

### Fase 12 — Menús y procesos dinámicos

**Objetivo:** modelar menús, procesos y relaciones de acceso según roles.

**Incluye:** tablas, migraciones, entidades, repositorios, administración, asociación con roles, consultas de navegación autorizada y pruebas.

**Excluye:** OTP, cambios de autenticación y nuevos mecanismos de sesión. **Dependencia:** Fase 11.

### Fase 13 — OTP y desafíos de autenticación

**Objetivo:** implementar desafíos temporales de autenticación.

**Incluye:** `login_challenges`, generación de OTP, expiración, consumo único, límites, intentos y pruebas.

**Excluye:** sustituir BCrypt, eliminar JWT o cambiar la sesión única sin decisión explícita. **Dependencias:** Fases 09 y 10.

### Fase 14 — Documentación OpenAPI

Documentar los contratos HTTP ya implementados. Depende de APIs principales estables.

### Fase 15 — Pruebas de integración ampliadas

Validar flujos completos de persistencia, API, autenticación, JWT, sesiones, roles, autorización y OTP. No reemplaza las pruebas creadas en cada fase anterior. Depende de las Fases 07 a 13.

### Fase 16 — Auditoría

Revisar seguridad, logs, trazabilidad, datos sensibles, sesiones y operaciones críticas. Depende del modelo y seguridad estables.

### Fase 17 — Preparación para producción

Completar configuración, perfiles, secretos externos, observabilidad, empaquetado, despliegue y controles operativos. Depende de las Fases 14, 15 y 16.

## Decisiones pendientes

`tipo_persona` es una clasificación de negocio de Persona; no representa roles, permisos, authorities ni autorización.

## Dependencias generales

- Fase 07 depende de Fase 06.
- Fase 08 depende de Fases 06 y 07.
- Fase 09 depende de Fases 07 y 08.
- Fase 10 depende de Fase 09.
- Fase 11 depende de Fases 08 y 10.
- Fase 12 depende de Fase 11.
- Fase 13 depende de Fases 09 y 10.
- Fase 14 depende de APIs principales estables.
- Fase 15 depende de Fases 07 a 13.
- Fase 16 depende del modelo y seguridad estables.
- Fase 17 depende de Fases 14, 15 y 16.

Una dependencia expresa orden técnico, pero cada fase requiere además autorización explícita del usuario.
