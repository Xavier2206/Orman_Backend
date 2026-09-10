# Fase 08 — Roles y relación Usuario–Rol

## Estado final

`COMPLETADA` el 2026-08-02. Se implementó el catálogo administrativo de Roles y la relación muchos a muchos entre Usuario y Rol, sin iniciar autenticación, JWT, sesiones ni autorización.

## Alcance implementado

La fase creó las migraciones Flyway V4 y V5, el módulo `role`, sus entidades JPA, repositorios, DTO, mapper, servicios, controladores, pruebas y guía Postman.

La tabla `roles` usa `codr` como identidad y `nombre` único. La tabla intermedia se llama `rolusu`, tiene clave primaria compuesta `(login, codr)` y conserva `fecha_asignacion` generada por PostgreSQL.

No se insertaron roles iniciales. `personas.tipo_persona` no se transforma ni se utiliza como Rol.

## Esquema y migraciones

V4 crea exclusivamente `roles`:

```sql
CREATE TABLE roles (
    codr INTEGER GENERATED ALWAYS AS IDENTITY,
    nombre VARCHAR(50) NOT NULL,
    estado SMALLINT NOT NULL DEFAULT 1,

    CONSTRAINT pk_roles PRIMARY KEY (codr),
    CONSTRAINT uk_roles_nombre UNIQUE (nombre),
    CONSTRAINT ck_roles_estado CHECK (estado IN (0, 1))
);
```

V5 crea exclusivamente `rolusu`:

```sql
CREATE TABLE rolusu (
    login VARCHAR(30) NOT NULL,
    codr INTEGER NOT NULL,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_rolusu PRIMARY KEY (login, codr),
    CONSTRAINT fk_rolusu_login
        FOREIGN KEY (login)
        REFERENCES usuarios (login)
        ON DELETE CASCADE,
    CONSTRAINT fk_rolusu_codr
        FOREIGN KEY (codr)
        REFERENCES roles (codr)
        ON DELETE RESTRICT
);
```

V1, V2 y V3 permanecieron intactas. PostgreSQL aplica el default de `estado` y de `fecha_asignacion`; las entidades `Rol` y `RolUsu` usan `@DynamicInsert` para respetarlos.

## Arquitectura y contrato

El módulo `com.orman.backend.role` contiene `controller`, `dto.request`, `dto.response`, `entity`, `mapper`, `repository`, `service` y `service.impl`.

`RolUsu` es una entidad explícita con `@EmbeddedId`, `@MapsId` y relaciones `LAZY` hacia `Usuario` y `Rol`, sin cascadas JPA ni colecciones bidireccionales. La clave compuesta evita asignaciones duplicadas y `fecha_asignacion` permanece disponible para trazabilidad básica.

| Método | Ruta | Resultado |
|---|---|---|
| POST | `/api/v1/roles` | Crea Rol, `201 Created` y `Location`. |
| GET | `/api/v1/roles/{codr}` | Consulta un Rol. |
| GET | `/api/v1/roles` | Lista paginada por nombre ascendente. |
| PUT | `/api/v1/roles/{codr}` | Actualiza solo nombre. |
| PATCH | `/api/v1/roles/{codr}/activar` | Activa idempotentemente. |
| PATCH | `/api/v1/roles/{codr}/desactivar` | Desactiva idempotentemente. |
| POST | `/api/v1/usuarios/{login}/roles/{codr}` | Asigna Rol, `201 Created` y `Location`. |
| DELETE | `/api/v1/usuarios/{login}/roles/{codr}` | Retira asignación, `204`. |
| GET | `/api/v1/usuarios/{login}/roles` | Lista Roles de Usuario. |
| GET | `/api/v1/roles/{codr}/usuarios` | Lista Usuarios del Rol. |

No existe endpoint DELETE físico para Roles.

## Reglas y errores

- `nombre` es obligatorio, se recorta y se normaliza a mayúsculas con `Locale.ROOT`.
- El nombre duplicado devuelve `409 CONFLICT`.
- El estado del Rol es `0` o `1`; activar y desactivar son idempotentes.
- Un Usuario inactivo conserva sus asignaciones y puede recibir asignaciones administrativas.
- Un Rol inactivo conserva sus asignaciones, pero no acepta nuevas; devuelve `422 BUSINESS_RULE_VIOLATION`.
- La asignación duplicada devuelve `409 CONFLICT`.
- Usuario, Rol o asignación inexistentes devuelven `404 RESOURCE_NOT_FOUND`.
- `rolusu` se elimina físicamente solo al retirar la asignación.
- Eliminar físicamente un Usuario elimina sus filas `rolusu`; eliminar un Rol asignado queda restringido.

Se reutiliza `ProblemDetail`; no se exponen SQL, constraints, contraseñas, hashes ni datos completos de Persona. `RolUsuResponse` expone únicamente `login`, `codr`, `nombreRol` y `fechaAsignacion`.

## Pruebas y validación

Se añadieron pruebas de mapper, servicios, MVC y una integración real con PostgreSQL. Cubren migraciones, esquema, defaults, constraints, PK/FK/UK/CHECK, `CASCADE`, `RESTRICT`, creación, normalización, paginación, actualización, estados, asignación, retiro, inexistentes, duplicados, Rol inactivo, `ProblemDetail`, `Location` y ausencia de secretos.

Comando ejecutado:

```powershell
.\mvnw.cmd clean test
```

Resultado: **BUILD SUCCESS**; 103 pruebas, 0 fallos, 0 errores y 0 omitidas. PostgreSQL 17.6 conectó; Flyway validó V1–V5 y dejó el esquema en versión 5; Hibernate validó el esquema con `ddl-auto=validate`.

Los flujos HTTP básicos están cubiertos por las pruebas MVC y los flujos reales de creación, asignación, consulta, duplicado, desactivación, reactivación y retiro por la integración transaccional contra PostgreSQL. Se usaron transacciones con rollback para no dejar usuarios o roles de prueba persistentes.

## Archivos y cierre

Se crearon V4, V5, el módulo `role`, seis clases de prueba, esta documentación y la guía Postman de Roles. Se actualizaron las pruebas existentes que declaraban exactamente las tablas y versiones Flyway, además del plan y la documentación técnica relacionada.

No se implementaron login, autenticación, JWT, refresh token, sesiones, logout, filtros, `SecurityFilterChain`, authorities, autorización, permisos, OTP, menús, procesos ni auditoría completa. La Fase 09 no se inició.

Riesgo conocido no bloqueante: Mockito informa una advertencia del JDK sobre carga dinámica de agente durante las pruebas; no hubo fallos y no se modificó la configuración de pruebas, por quedar fuera del alcance de la fase.

El usuario debe revisar los cambios y ejecutar Git manualmente.

## Actualización posterior — Gestión remota de Roles (2026-09-05)

Se amplió exclusivamente el catálogo de Roles para preparar la pantalla de gestión, sin modificar migraciones, autenticación/JWT, la relación Usuario–Rol, la relación Rol–Menú, permisos, DELETE ni los contratos CRUD existentes.

- `GET /api/v1/roles` acepta `q` y `estado` opcionales además de `page`, `size` y `sort`.
- `q` aplica búsqueda parcial por `nombre`, case-insensitive y con `trim`; valores vacíos no filtran.
- `estado` acepta `0` o `1`; un valor inválido devuelve `400 VALIDATION_ERROR` mediante el `ProblemDetail` existente.
- Los filtros se combinan con `AND` y se resuelven en PostgreSQL mediante una consulta JPQL paginada, sin cargar ni filtrar el catálogo en memoria.
- `GET /api/v1/roles/resumen` devuelve `RolResumenResponse(totalRoles, activos, inactivos)` para todo el catálogo, calculado mediante `count` y `countByEstado` en BD.

No se creó índice adicional: `roles.nombre` ya está cubierto por `uk_roles_nombre`; el catálogo esperado es pequeño y `estado` por sí solo no justifica optimización prematura.

Se agregaron pruebas MVC, unitarias, integración PostgreSQL y autorización del resumen. La validación final ejecutó `./mvnw.cmd clean test` con **BUILD SUCCESS**: 237 pruebas, 0 fallos, 0 errores y 0 omitidas; PostgreSQL validó Flyway V1–V9 y Hibernate permaneció en `ddl-auto=validate`. `./mvnw.cmd package -DskipTests` también generó el JAR correctamente.
