# Fase 02 — Configuración de PostgreSQL y Flyway

## Identificación

- **Estado:** `COMPLETADA`
- **Fecha de inicio:** 2026-07-27
- **Fecha de cierre:** 2026-07-27

## Objetivo y alcance

Se configuró el datasource PostgreSQL mediante variables de entorno, Hibernate en modo de validación y Flyway sin migraciones ni tablas de dominio. No se implementaron funcionalidades de negocio, entidades, repositorios, API, seguridad, migraciones SQL ni elementos de la Fase 03.

## Configuración efectiva

```yaml
spring:
  application:
    name: ORMAN-BACKEND
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:orman}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    validate-on-migrate: true
    baseline-on-migrate: false
    clean-disabled: true
```

No hay comillas incorrectas, doble signo de dólar, barras invertidas, caracteres invisibles, filtrado Maven de recursos ni configuración alternativa que sobrescriba el datasource. Después de `clean`, `target/classes/application.yml` se regeneró y coincide con el recurso fuente. No existe `application.properties`, perfiles ni `bootstrap.*` residuales.

## Diagnóstico y corrección

La causa real no fue la sintaxis de `application.yml`: el proceso restringido usado en el diagnóstico inicial no veía las variables `DB_*` del entorno de usuario y Spring recibió los placeholders obligatorios sin resolver, por lo que PostgreSQL registró el usuario literal `${DB_USERNAME}`.

En el proceso autorizado que ejecutó Maven, las cinco variables de usuario estaban presentes. Se importaron al proceso sin mostrarlas ni registrarlas y la prueba se ejecutó correctamente. Se mantuvo el YAML sin credenciales. Además, `.env.example` se corrigió para usar un marcador de contraseña en lugar de una credencial concreta.

| Variable | Usuario de Windows | Proceso Maven tras importar |
|---|---|---|
| `DB_HOST` | `PRESENTE` | `PRESENTE` |
| `DB_PORT` | `PRESENTE` | `PRESENTE` |
| `DB_NAME` | `PRESENTE` | `PRESENTE` |
| `DB_USERNAME` | `PRESENTE` | `PRESENTE` |
| `DB_PASSWORD` | `PRESENTE` | `PRESENTE` |

No se mostraron los valores de usuario ni contraseña. Las credenciales no se almacenaron en código, configuración, pruebas, documentación ni plantilla versionable.

## Validaciones ejecutadas

- Inspección de `AGENTS.md`, plan, documento de fase, `application.yml`, copia de `target`, `pom.xml`, `.gitignore`, `.env.example`, `contextLoads` y configuraciones candidatas.
- Verificación de que no existen archivos SQL de migración, ni `application.properties`, ni propiedades Maven que filtren los placeholders.
- `.\mvnw.cmd clean`: **BUILD SUCCESS**.
- `.\mvnw.cmd test`: **BUILD SUCCESS**; 1 prueba ejecutada, 0 fallos, 0 errores y 0 omitidas.
- HikariPool inició y PostgreSQL 17.6 aceptó la conexión a la base configurada.
- Flyway validó correctamente 0 migraciones; la advertencia de no encontrar migraciones es esperada en esta fase.
- Hibernate inició con `ddl-auto=validate`; `open-in-view=false` y `clean-disabled=true` continúan configurados.
- Consulta JDBC de solo lectura: `public` contiene únicamente `flyway_schema_history`; no hay tablas de negocio creadas por ORMAN.

No se ejecutó `flyway clean`, no se alteró ni eliminó la base de datos y no se ejecutaron comandos Git.

## Estado de Flyway

`flyway_schema_history` está **PRESENTE**. El esquema público estaba vacío antes de la inicialización de Flyway, no se aplicaron migraciones y no hay tablas de dominio.

## Archivos creados en este cierre

- Ninguno.

## Archivos modificados en este cierre

- `.env.example`
- `README.md`
- `CHANGELOG.md`
- `docs/PLAN_GENERAL.md`
- `docs/fases/README.md`
- `docs/fases/02-configuracion-postgresql-flyway.md`

## Archivos eliminados en este cierre

- Ninguno.

## Pendientes

- Revisión manual del usuario y ejecución manual de Git.
- La Fase 03 sigue pendiente y no se inició en esta ejecución.

## Resultado final

La Fase 02 queda **COMPLETADA**. PostgreSQL, Hikari, Flyway, Hibernate y `contextLoads` fueron validados correctamente con variables externas, sin secretos en el repositorio y sin crear migraciones ni tablas de negocio.
