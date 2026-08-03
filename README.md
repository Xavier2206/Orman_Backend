# ORMAN-BACKEND

Backend de ORMAN construido con Java 21 y Spring Boot 4.1.0. La Fase 10.1 completó sesiones por dispositivo, JWT y refresh rotatorio sobre PostgreSQL.

## Estado actual

**Última subfase completada: Fase 10.1 — Sesiones por dispositivo, JWT y refresh token**

**Fase 10 global: `EN DESARROLLO`; Fase 10.2 pendiente y no iniciada**

El backend incorpora Persona, Usuario, Roles, login WEB/MOBILE y refresh. Flyway V6 administra `sesiones_usuario`; el access JWT dura 15 minutos y el refresh opaco rota, dura 30 días y se persiste solo como hash. Aún no hay filtro JWT, `SecurityFilterChain`, protección de endpoints, logout ni autorización.

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

El servidor se configura para usar el puerto `9090`. La API de Persona está disponible en `/api/v1/personas`.

## Requisitos

- JDK 21
- Una terminal compatible con el Maven Wrapper

Para iniciar la aplicación se requiere PostgreSQL, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET` de al menos 32 bytes.

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
$env:JWT_SECRET = "<secreto-aleatorio-de-al-menos-32-bytes>"
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

Con las variables configuradas, puedes iniciar la aplicación con `./mvnw spring-boot:run` en Linux/macOS o `.\mvnw.cmd spring-boot:run` en Windows. Flyway administrará el historial de esquema y aplicará las migraciones de `personas` y `usuarios` cuando corresponda.

## Documentación

- [Reglas permanentes de trabajo](AGENTS.md)
- [Registro de cambios](CHANGELOG.md)
- [Índice de documentación](docs/README.md)
- [Plan general](docs/PLAN_GENERAL.md)
- [Etapa 1 — Fundación técnica](docs/etapas/etapa-01-fundacion-tecnica.md)
- [Etapa 2 — Personas, usuarios y roles](docs/etapas/etapa-02-personas-usuarios-roles.md)
- [Fase 00 — Planificación general](docs/fases/00-planificacion-general.md)
- [Fase 01 — Revisión y normalización Spring Boot](docs/fases/01-revision-normalizacion-spring-boot.md)
- [Fase 02 — Configuración PostgreSQL y Flyway](docs/fases/02-configuracion-postgresql-flyway.md)
- [Fase 03 — Infraestructura común y manejo global de errores](docs/fases/03-infraestructura-comun-manejo-errores.md)
- [Fase 04 — Modelo y migración de Persona](docs/fases/04-modelo-migracion-persona.md)
- [Fase 05 — API CRUD de Persona](docs/fases/05-crud-persona.md)
- [Fase 06 — Modelo y migración de Usuario](docs/fases/06-modelo-migracion-usuario.md)
- [Fase 07 — Administración de Usuarios y Contraseñas](docs/fases/07-administracion-usuarios-contrasenas.md)
- [Fase 08 — Roles y relación Usuario–Rol](docs/fases/08-roles-relacion-usuario-rol.md)
- [Fase 09 — Autenticación y validación de credenciales](docs/fases/09-autenticacion-validacion-credenciales.md)
- [Fase 10.1 — Sesiones por dispositivo, JWT y refresh token](docs/fases/10-1-sesiones-jwt-refresh.md)
- [Guía Postman de la API Persona](docs/postman/persona.md)
- [Guía Postman de la API Usuario](docs/postman/usuario.md)
- [Guía Postman de la API Roles](docs/postman/rol.md)
- [Guía Postman de Autenticación](docs/postman/auth.md)

La documentación generada por Spring Initializr se conserva en `HELP.md`.
