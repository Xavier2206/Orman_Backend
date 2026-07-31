# ORMAN-BACKEND

Backend de ORMAN construido con Java y Spring Boot. El proyecto se encuentra en su fase de planificación y fundación técnica; todavía no implementa endpoints ni funcionalidades de negocio.

## Estado actual

**Fase 04 — Modelo y migración de Persona: COMPLETADA**

El backend incorpora un contrato uniforme RFC 9457 para errores HTTP y la persistencia inicial de `personas`, gestionada por Flyway y validada con PostgreSQL. Todavía no existen endpoints ni funcionalidades de negocio.

## Stack confirmado

- Java 21
- Spring Boot
- Maven y Maven Wrapper
- PostgreSQL
- Spring Data JPA e Hibernate
- Flyway
- Bean Validation
- Lombok
- Empaquetado JAR
- IntelliJ IDEA y Codex CLI como herramientas de trabajo

El servidor se configura para usar el puerto `9090`. No existe una API disponible.

## Requisitos

- JDK 21
- Una terminal compatible con el Maven Wrapper

Para iniciar la aplicación o ejecutar las pruebas de contexto se requiere acceso a PostgreSQL y las variables `DB_USERNAME` y `DB_PASSWORD`.

## Configuración local de PostgreSQL

La aplicación obtiene la conexión de estas variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD`. Los valores no sensibles de host, puerto y base tienen valores predeterminados en `application.yml`; usuario y contraseña son obligatorios.

Usa [.env.example](.env.example) como referencia. Puede versionarse, pero `.env` real está ignorado y Spring Boot no lo carga automáticamente.

En PowerShell, configura las variables solo para la sesión actual antes de ejecutar Maven:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "orman"
$env:DB_USERNAME = "<usuario>"
$env:DB_PASSWORD = "<contraseña>"
.\mvnw.cmd test
```

En IntelliJ IDEA, abre la configuración de ejecución o prueba, agrega las cinco variables en **Environment variables** y ejecuta la configuración. No guardes contraseñas en Git, documentación ni archivos versionados.

## Comandos Maven

En Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

En Linux o macOS:

```bash
./mvnw test
./mvnw clean package
./mvnw spring-boot:run
```

Con las variables configuradas, puedes iniciar la aplicación con `./mvnw spring-boot:run` en Linux/macOS o `.\mvnw.cmd spring-boot:run` en Windows. Flyway administrará el historial de esquema y aplicará las migraciones de `personas` cuando corresponda.

## Documentación

- [Fase 05 — API CRUD de Persona](docs/fases/05-crud-persona.md)
- [Guía Postman de la API Persona](docs/postman/persona.md)

- [Reglas permanentes de trabajo](AGENTS.md)
- [Registro de cambios](CHANGELOG.md)
- [Índice de documentación](docs/README.md)
- [Plan general](docs/PLAN_GENERAL.md)
- [Etapa 1 — Fundación técnica](docs/etapas/etapa-01-fundacion-tecnica.md)
- [Fase 00 — Planificación general](docs/fases/00-planificacion-general.md)
- [Fase 01 — Revisión y normalización Spring Boot](docs/fases/01-revision-normalizacion-spring-boot.md)
- [Fase 02 — Configuración PostgreSQL y Flyway](docs/fases/02-configuracion-postgresql-flyway.md)
- [Fase 03 — Infraestructura común y manejo global de errores](docs/fases/03-infraestructura-comun-manejo-errores.md)
- [Fase 04 — Modelo y migración de Persona](docs/fases/04-modelo-migracion-persona.md)

La documentación generada por Spring Initializr se conserva en `HELP.md`.
