# Fase 02 — Configuración de PostgreSQL y Flyway

## Identificación

- **Estado:** `BLOQUEADA`
- **Fecha de inicio:** 2026-07-27
- **Fecha de actualización:** 2026-07-27

## Objetivo

Configurar datasource PostgreSQL, variables de entorno, Spring Data JPA, Hibernate en modo de validación, Flyway y el directorio de migraciones, sin crear tablas del dominio.

## Alcance

- Configuración segura de datasource mediante variables de entorno.
- Hibernate con `ddl-auto=validate` y `open-in-view=false`.
- Flyway habilitado, validando migraciones, usando `classpath:db/migration`, sin baseline automático y con `clean` deshabilitado.
- Plantilla versionable de variables y reglas de Git para secretos locales.
- Diagnóstico de conexión disponible sin leer ni registrar credenciales.

## Fuera de alcance

No se crearon tablas, entidades, repositorios, controladores, servicios, DTO, migraciones SQL, perfiles, Docker, usuarios de base de datos, bases de datos, seguridad, JWT, OTP ni funcionalidades de negocio.

## Estado inicial

- JPA, driver PostgreSQL, Flyway y `flyway-database-postgresql` ya estaban declarados.
- No había datasource, configuración JPA/Flyway ni variables `DB_*` disponibles.
- `contextLoads` fallaba porque Spring no podía determinar un datasource.
- No existían migraciones ni clases de negocio.

## Configuración implementada

```yaml
spring:
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

No se declaró `driver-class-name`: Spring Boot detecta el driver PostgreSQL disponible.

## Variables de entorno

| Variable | Requerida | Valor de referencia |
|---|---|---|
| `DB_HOST` | No | `localhost` |
| `DB_PORT` | No | `5432` |
| `DB_NAME` | No | `orman` |
| `DB_USERNAME` | Sí | Configurado por el usuario |
| `DB_PASSWORD` | Sí | Configurado por el usuario |

[`.env.example`](../../.env.example) contiene solo ejemplos. `.env` y `.env.*` están ignorados, excepto la plantilla. Spring Boot no carga `.env` automáticamente.

## Dependencias revisadas

- `spring-boot-starter-data-jpa` 4.1.0.
- `org.postgresql:postgresql` 42.7.11 con alcance `runtime`.
- `spring-boot-starter-flyway` 4.1.0.
- `org.flywaydb:flyway-database-postgresql` 12.4.0, declarado directamente y administrado por Spring Boot.
- Flyway Core 12.4.0, aportado por el módulo PostgreSQL.

No se agregaron ni eliminaron dependencias durante esta fase.

## Directorio de migraciones

Se creó `src/main/resources/db/migration/` sin migraciones ni archivos SQL. Los directorios vacíos no se versionan en Git; la primera migración autorizada en una fase posterior lo persistirá. Flyway puede iniciar sin migraciones y crear únicamente su historial cuando haya conexión válida.

## Archivos creados

- `.env.example`
- `src/main/resources/db/migration/` (directorio vacío local)
- `docs/fases/02-configuracion-postgresql-flyway.md`

## Archivos modificados

- `src/main/resources/application.yml`
- `.gitignore`
- `README.md`
- `CHANGELOG.md`
- `docs/README.md`
- `docs/PLAN_GENERAL.md`
- `docs/fases/README.md`
- `docs/database/README.md`
- `docs/etapas/etapa-01-fundacion-tecnica.md`

## Archivos eliminados

Ninguno.

## Comandos ejecutados

- Inspección de reglas, configuración, documentación, árbol de fuentes y presencia de variables `DB_*` sin mostrar valores sensibles.
- Comprobación de disponibilidad de `psql` y `pg_isready`.
- `.\mvnw.cmd dependency:tree`: primer intento bloqueado por acceso restringido a Maven Central; segundo intento autorizado y correcto.
- `.\mvnw.cmd clean test-compile`: primer intento bloqueado por acceso restringido a Maven Central; segundo intento autorizado y correcto.
- `.\mvnw.cmd test`: primer intento bloqueado por acceso restringido a Maven Central; segundo intento autorizado y ejecutado sin credenciales ficticias.

No se ejecutaron comandos Git, `flyway clean`, SQL de creación de bases ni comandos que modificaran PostgreSQL.

## Conexión probada

Las cinco variables `DB_*` no estaban definidas y no había cliente `psql` ni `pg_isready` disponible. Aun así, la prueba alcanzó PostgreSQL en el host y puerto predeterminados; el servidor rechazó la autenticación porque no recibió `DB_USERNAME` ni `DB_PASSWORD` válidas. No se puede confirmar la existencia de la base `orman` ni consultar su esquema hasta proporcionar credenciales.

Para desbloquear, el usuario debe configurar variables temporales o de su entorno de ejecución con un usuario y contraseña válidos para la base `orman` —o ajustar `DB_NAME` a una base existente autorizada— y ejecutar:

```powershell
.\mvnw.cmd test
```

Si la base `orman` no existe, el usuario debe crearla manualmente con una cuenta autorizada, por ejemplo: `CREATE DATABASE orman;`. Codex no la creó.

## Estado de Flyway

Flyway está habilitado y recibió la configuración correcta, pero no logró iniciar por la autenticación rechazada antes de obtener una conexión. No se creó ni verificó `flyway_schema_history`.

## Estado de `contextLoads`

La prueba encuentra `OrmanBackendApplication`, crea Hikari y alcanza PostgreSQL. Finaliza con un error de autenticación porque faltan credenciales válidas. No se declarará como exitosa hasta que Flyway inicie y el contexto cargue contra PostgreSQL real.

## Pruebas y validaciones

- Árbol efectivo de dependencias: correcto para JPA, PostgreSQL, Flyway Core y soporte PostgreSQL.
- `clean test-compile`: **BUILD SUCCESS** con Java 21.
- `test`: una prueba ejecutada, cero fallos de aserción, un error de contexto, cero omitidas.
- `ddl-auto=validate`, `open-in-view=false`, puerto `9090` y `clean-disabled=true`: configurados.
- `.env.example` existe; `.env` queda ignorado; no existe `.env` real.
- No existen migraciones, tablas o clases de negocio.

## Errores y correcciones

1. Los primeros comandos Maven en el entorno restringido no pudieron acceder a Maven Central; se repitieron con acceso autorizado.
2. La prueba no pudo autenticarse en PostgreSQL por ausencia de `DB_USERNAME` y `DB_PASSWORD`; no se aplicó una corrección artificial ni se usaron valores inventados.

## Riesgos y limitaciones

- La Fase 02 no puede cerrarse sin una conexión PostgreSQL real y credenciales válidas.
- No se confirmó la creación de `flyway_schema_history` ni la ausencia de tablas de negocio dentro de la base porque no hubo autenticación.
- El directorio de migraciones vacío existe localmente, pero Git no versiona directorios vacíos.

## Pendientes

- Configurar `DB_USERNAME` y `DB_PASSWORD` válidas en el entorno de ejecución.
- Confirmar o crear manualmente la base autorizada indicada por `DB_NAME`.
- Ejecutar `contextLoads` con conexión real y verificar Flyway y `flyway_schema_history`.
- Mantener el esquema sin tablas de negocio hasta sus fases autorizadas.
- Revisión manual del usuario y Git manual.

## Criterios de aceptación

- [x] Datasource configurado mediante variables de entorno.
- [x] Sin credenciales reales en el repositorio.
- [x] Driver PostgreSQL y soporte Flyway PostgreSQL disponibles.
- [x] Hibernate usa `ddl-auto=validate`.
- [x] `open-in-view` deshabilitado.
- [x] Directorio de migraciones preparado, sin SQL ni tablas del dominio.
- [x] Documentación e índices actualizados.
- [x] Compilación principal y de pruebas correcta.
- [ ] `contextLoads` pasa contra PostgreSQL real.
- [ ] Flyway inicia y se verifica `flyway_schema_history`.

## Resultado final

La configuración de la Fase 02 está implementada, pero la fase queda **BLOQUEADA** por falta de credenciales PostgreSQL válidas. No se avanzará a la Fase 03 hasta validar la conexión real, Flyway y `contextLoads`.

## Siguiente paso para desbloquear

Configurar `DB_USERNAME` y `DB_PASSWORD` válidas para una instancia PostgreSQL accesible y ejecutar `.\mvnw.cmd test`. Si la base indicada por `DB_NAME` no existe, crearla manualmente con autorización antes de repetir la prueba.
