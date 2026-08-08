# Fase 04 — Modelo y migración de Persona

## Identificación

- **Estado:** `COMPLETADA`
- **Inicio, bloqueo y cierre:** 2026-07-29

## Antecedente y fuente de verdad

Nota posterior: V8 corrigio la nulabilidad historica de `correo`; desde esta
correccion `personas.correo` es `VARCHAR(100) NOT NULL` para toda Persona. La
descripcion nullable de la definicion original queda supersedida por
`docs/fases/ajuste-correo-obligatorio-persona.md`.

La fase se bloqueó inicialmente porque no existía una definición completa de `personas`. Posteriormente se autorizó el modelo definitivo y, después de detectar que V1 ya aplicada dejaba `fecha_registro` nullable, se autorizó expresamente V2 para corregir únicamente esa nulabilidad. La definición SQL y semántica proporcionadas por el usuario son la fuente de verdad de la fase.

`tipo_persona` es una clasificación de negocio de Persona (`A` administrador, `I` inquilino). No es un sistema de roles, permisos ni autorización y no genera autoridades de Spring Security.

## Alcance realizado

Se implementaron exclusivamente las migraciones de `personas`, la entidad `Persona`, su repositorio mínimo, pruebas contra PostgreSQL real y documentación. No se crearon componentes en `controller`, `dto`, `mapper`, `security` o `service`; tampoco CRUD HTTP, servicios, Usuario, Rol, autenticación ni elementos de la Fase 05.

## Estructura modular

Se utilizó el módulo existente `com.orman.backend.person`:

- `com.orman.backend.person.entity.Persona`
- `com.orman.backend.person.repository.PersonaRepository`

## Esquema final

Tabla física: `personas`.

| Columna | PostgreSQL | Nulable | Default | Java |
|---|---|---:|---|---|
| `codper` | `INTEGER GENERATED ALWAYS AS IDENTITY` | No | identidad | `Integer` |
| `ci` | `VARCHAR(20)` | No | — | `String` |
| `nombre` | `VARCHAR(60)` | No | — | `String` |
| `ap` | `VARCHAR(40)` | Sí | — | `String` |
| `am` | `VARCHAR(40)` | Sí | — | `String` |
| `genero` | `CHAR(1)` | No | — | `Character` |
| `estado` | `SMALLINT` | No | `1` | `Short` |
| `correo` | `VARCHAR(100)` | Sí | — | `String` |
| `telefono` | `VARCHAR(20)` | No | — | `String` |
| `tipo_persona` | `CHAR(1)` | No | — | `Character` |
| `foto` | `VARCHAR(255)` | Sí | — | `String` |
| `fecha_registro` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | `LocalDateTime` |

Restricciones: `pk_personas`, `uk_personas_ci`, `ck_personas_genero`, `ck_personas_estado` y `ck_personas_tipo_persona`. Los únicos índices son `pk_personas` y `uk_personas_ci`, generados para la clave primaria y la unicidad.

## Migraciones Flyway

- `V1__create_personas_table.sql`: creó `personas`, pero su `fecha_registro` inicial era nullable.
- `V2__make_personas_fecha_registro_not_null.sql`: después de confirmar mediante consulta de solo lectura que había 0 valores nulos, ejecutó únicamente `ALTER COLUMN fecha_registro SET NOT NULL`.

No se editó, eliminó ni reemplazó V1; no se ejecutó `flyway clean` ni se alteró manualmente `flyway_schema_history`.

## Entidad, identidad y Lombok

`Persona` usa `@Entity`, `@Table(name = "personas")`, `@Getter`, setters controlados por campo y `@NoArgsConstructor`. No usa `@Data`, `@ToString`, `@EqualsAndHashCode`, `@Builder` ni relaciones JPA.

`codper` se mapea exactamente con `Integer`, `GenerationType.IDENTITY`, `nullable = false` y `updatable = false`. La base de datos genera el identificador.

`equals` compara únicamente `codper` cuando ambos objetos tienen identidad persistida; `hashCode` se basa en la clase. No incluyen datos personales y no se declara `toString`.

`@DynamicInsert` permite omitir `estado` y `fecha_registro` nulos para que PostgreSQL aplique `1` y `CURRENT_TIMESTAMP`. No se usan `@CreationTimestamp` ni `@PrePersist`.

`PersonaRepository` extiende `JpaRepository<Persona, Integer>` y no añade métodos derivados, consultas ni implementación manual.

## Pruebas y resultados

`PersonaPersistenceIntegrationTest` usa PostgreSQL real, `@Transactional` y rollback. Usa datos ficticios con prefijo `TEST-CI-`, nombres ficticios y teléfonos de prueba; no deja registros permanentes.

Verifica V1/V2, historial Flyway, tablas, columnas, tipos, longitudes, nulabilidad, defaults, constraints, índices, persistencia, generación de `codper`, lectura por id, defaults, nulos opcionales y todas las restricciones aprobadas.

Comando ejecutado con `DB_*` importadas desde Windows sin revelar valores:

```powershell
.\mvnw.cmd clean test
```

Resultado: **BUILD SUCCESS**, 17 pruebas, 0 fallos y 0 errores. `contextLoads` pasó. Flyway aplicó V2 y validó ambas migraciones; Hibernate inició con `ddl-auto=validate` sin crear ni modificar objetos.

Consultas finales de solo lectura confirmaron:

- Tablas: `flyway_schema_history`, `personas`.
- Versiones exitosas: `1`, `2`.
- `fecha_registro`: `NOT NULL`.
- Índices: `pk_personas`, `uk_personas_ci`.

## Archivos

### Creados

- `src/main/resources/db/migration/V2__make_personas_fecha_registro_not_null.sql`
- `src/main/java/com/orman/backend/person/entity/Persona.java`
- `src/main/java/com/orman/backend/person/repository/PersonaRepository.java`
- `src/test/java/com/orman/backend/person/entity/PersonaPersistenceIntegrationTest.java`

### Modificados

- `docs/fases/04-modelo-migracion-persona.md`
- `docs/PLAN_GENERAL.md`
- `docs/fases/README.md`
- `docs/README.md`
- `docs/database/README.md`
- `docs/database/modelo-inicial.md`
- `README.md`
- `CHANGELOG.md`

### Eliminados

- `src/test/java/com/orman/backend/persona/PersonaPersistenceIntegrationTest.java`, prueba residual de un paquete no autorizado que no tenía entidad correspondiente.

### Dependencias

No se modificaron dependencias.

## Riesgos, limitaciones y cierre

Las pruebas requieren PostgreSQL local y variables externas `DB_*`; no se añadieron H2, Docker ni Testcontainers. No hay errores bloqueantes conocidos. La Fase 04 queda **COMPLETADA**; la siguiente fase posible es la Fase 05 — CRUD de Persona, que no se inició. El usuario debe revisar los cambios y ejecutar Git manualmente.
