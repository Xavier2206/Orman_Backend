# Fase 06 — Modelo y migración de Usuario

## Estado final

`COMPLETADA` el 2026-07-31. La fase incorpora exclusivamente el modelo de persistencia de Usuario, su migración Flyway, repositorio y pruebas reales contra PostgreSQL.

## Objetivo y alcance

Se creó `usuarios` mediante V3, la entidad JPA `Usuario`, `UsuarioRepository` y una relación uno a uno unidireccional hacia `Persona`. No se creó CRUD REST, controller, DTO, mapper, service, seguridad, BCrypt, autenticación, JWT, roles, sesiones ni tablas adicionales.

## Tabla `usuarios`

| Columna | PostgreSQL | Nulable | Default | Java |
|---|---|---:|---|---|
| `login` | `VARCHAR(30)` | No | — | `String` |
| `passwd` | `VARCHAR(255)` | No | — | `String` |
| `estado` | `SMALLINT` | No | `1` | `Short` |
| `codper` | `INTEGER` | No | — | relación `Persona` |
| `fecha_creacion` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | `LocalDateTime` |
| `ultimo_acceso` | `TIMESTAMP` | Sí | — | `LocalDateTime` |

Las restricciones son `pk_usuarios` sobre `login`, `uk_usuarios_codper`, `ck_usuarios_estado` (`0` o `1`) y `fk_usuarios_personas`. Esta última referencia `personas(codper)` con `ON DELETE RESTRICT`. No hay columnas, índices ni tablas adicionales.

Una Persona puede tener cero o un Usuario; un Usuario pertenece obligatoriamente a una Persona. Una Persona con Usuario no puede eliminarse físicamente. El estado de Persona y el de Usuario son independientes: desactivar o reactivar Persona no modifica Usuario. La futura autenticación deberá exigir ambos estados en `1`; esa regla no se implementó aquí.

`ultimo_acceso` queda nullable y puramente persistido: no hay listeners, actualizaciones automáticas, control de sesión ni lógica de autenticación.

## Migración

`V3__create_usuarios_table.sql` contiene únicamente:

```sql
CREATE TABLE usuarios (
    login VARCHAR(30) NOT NULL,
    passwd VARCHAR(255) NOT NULL,
    estado SMALLINT NOT NULL DEFAULT 1,
    codper INTEGER NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso TIMESTAMP,

    CONSTRAINT pk_usuarios PRIMARY KEY (login),
    CONSTRAINT uk_usuarios_codper UNIQUE (codper),
    CONSTRAINT ck_usuarios_estado
        CHECK (estado IN (0, 1)),
    CONSTRAINT fk_usuarios_personas
        FOREIGN KEY (codper)
        REFERENCES personas (codper)
        ON DELETE RESTRICT
);
```

V1 y V2 no se modificaron. Flyway administra V1, V2 y V3; Hibernate continúa con `ddl-auto=validate`.

## Entidad, repositorio y seguridad

`Usuario` usa `@Entity`, `@Table(name = "usuarios")`, `@Id` en `login`, `@DynamicInsert`, `@Getter`, `@NoArgsConstructor` y solo setters por campo. La relación es `@OneToOne(fetch = LAZY, optional = false)` con `@JoinColumn(name = "codper", nullable = false, unique = true)`, sin cascadas. PostgreSQL genera `estado` y `fecha_creacion` cuando se omiten.

La igualdad compara únicamente el `login` asignado y no nulo. `hashCode` usa la clase efectiva resuelta por Hibernate, para no confundir proxies. No incluye `passwd`, estado, Persona, fechas ni datos personales. No existe `toString` generado.

`UsuarioRepository` extiende `JpaRepository<Usuario, String>` sin métodos especulativos.

`passwd` representa exclusivamente el futuro hash de contraseña. En esta fase no hay BCrypt, `PasswordEncoder`, generación, comparación ni exposición de contraseñas; las pruebas emplean una cadena ficticia que identifica expresamente que no es una credencial real.

## Pruebas y validación

`UsuarioPersistenceIntegrationTest` usa PostgreSQL real, transacciones y rollback. Comprueba V3, tablas, columnas, tipos, longitudes, nulabilidad, defaults, constraints, índices implícitos de PK/UNIQUE, persistencia/lectura por `login`, relación, unicidad de `codper`, `CHECK` de estado, `NOT NULL`, FK, `ON DELETE RESTRICT` y ausencia de cascada de borrado desde Usuario a Persona.

Las dos pruebas preexistentes que declaraban las tablas y migraciones exactas se ajustaron únicamente para reconocer `usuarios` y V3; su cobertura de Persona se mantiene.

Comando ejecutado con las variables externas `DB_*` sin revelar valores:

```powershell
.\mvnw.cmd clean test
```

Resultado final: **BUILD SUCCESS**; 65 pruebas, 0 fallos, 0 errores y 0 omitidas. `contextLoads` pasó; PostgreSQL 17.6 conectó; Flyway validó V1–V3 y aplicó V3; Hibernate validó el esquema sin DDL.

## Archivos y decisiones

### Creados

- `src/main/resources/db/migration/V3__create_usuarios_table.sql`
- `src/main/java/com/orman/backend/user/entity/Usuario.java`
- `src/main/java/com/orman/backend/user/repository/UsuarioRepository.java`
- `src/test/java/com/orman/backend/user/entity/UsuarioPersistenceIntegrationTest.java`
- Este documento.

### Modificados

- Pruebas de integración existentes que fijaban las tablas y versiones permitidas.
- Plan, índices documentales, teoría de Etapa 2, documentación de datos, README y CHANGELOG.

### Eliminados y dependencias

No se eliminaron archivos ni se modificaron dependencias.

## Riesgos, limitaciones y pendientes

El endpoint DELETE de Persona existente puede devolver un error de integridad no traducido si intenta eliminar una Persona con Usuario; esta fase verifica la restricción de base de datos, pero no modifica la API de Persona. La traducción HTTP corresponde a una fase futura autorizada.

La futura política es una sola sesión activa por Usuario: un nuevo inicio revocará la anterior mediante una futura tabla `sesiones_usuario`. No forma parte de esta fase y no se creó esa tabla ni tokens.

Los siguientes trabajos posibles son la Fase 07, gestión administrativa de usuarios, y posteriormente los alcances de seguridad autorizados. No se inició ninguno. El usuario debe revisar los cambios y ejecutar Git manualmente.
