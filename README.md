# ORMAN-BACKEND

Backend de ORMAN construido con Java 21 y Spring Boot 4.1.0. La Fase 12.2 incorpora la administración REST de Menús, Procesos y sus relaciones sin alterar la autorización existente.

## Estado actual

**Última subfase completada: Fase 12.2 — Administración REST de Menús, Procesos y relaciones**

**Fases 10, 11 y 12: `COMPLETADAS`; Fase 12.3 no iniciada**

El backend incorpora Persona, Usuario, Roles, login WEB/MOBILE, refresh rotatorio, seguridad stateless y sesiones propias. En cada petición protegida carga Roles activos como authorities `ROLE_<NOMBRE>` sin incluirlos en el JWT. La matriz protege Personas, Usuarios, Roles y asignaciones; reserva PROPIETARIO y exige al menos uno activo. Flyway está en V7 con `menus`, `procesos`, `rolme` y `mepro`, sin endpoints ni autorización por Proceso.

Antes de desplegar 11.2, el entorno debe tener un Rol activo exacto `PROPIETARIO` asignado a un Usuario y Persona activos. La base local auditada no cumple todavía esa precondición; consulte la [preparación manual](docs/fases/11-2-matriz-autorizacion-propietario.md#precondición-operativa). La aplicación no crea propietarios automáticamente.

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

`ORMAN_FRONTEND_URL` define el origen permitido para Angular. CORS admite credenciales y nunca usa `*`. Angular envía el access token mediante `Authorization: Bearer`, mantiene el refresh en cookie HttpOnly y copia el valor crudo de la cookie `XSRF-TOKEN` al header `X-XSRF-TOKEN` al renovar. Flutter envía Bearer y conserva su refresh MOBILE en almacenamiento seguro.

## Configuración local de PostgreSQL

La aplicación obtiene la conexión de estas variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD`. Los valores no sensibles de host, puerto y base tienen valores predeterminados en `application.yml`; usuario y contraseña son obligatorios.

Usa [.env.example](.env.example) como referencia. `application.yml` importa opcionalmente `.env` como archivo de propiedades (`optional:file:./.env[.properties]`); el archivo real está ignorado por Git y nunca se empaqueta. Si no existe, siguen funcionando las variables del sistema, que tienen prioridad sobre `.env`.

## Configuración local de JWT

`JWT_SECRET` es obligatorio y debe tener al menos 32 bytes. Genera un valor local una sola vez con PowerShell; no ejecutes ni compartas su resultado:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Alternativa A — `.env` local: crea `.env` en la raíz a partir de `.env.example`, coloca el resultado en `JWT_SECRET=<valor>` y arranca `OrmanBackendApplication`. Spring Boot 4.1.0 lo carga mediante el import opcional. No versionas ese archivo.

Alternativa B — IntelliJ: abre **Run → Edit Configurations → Environment variables** y agrega `JWT_SECRET=<valor-local-seguro>`. Si creaste una variable global de Windows después de abrir IntelliJ, reinicia IntelliJ para que pueda heredarla.

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

Para iniciar desde Maven en la sesión actual:

```powershell
$env:JWT_SECRET = "<valor-local-seguro>"
.\mvnw.cmd spring-boot:run
```

La variable de PowerShell dura solo durante esa sesión. Una variable de entorno configurada de este modo o en IntelliJ sobrescribe cualquier valor de `.env`.

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
- [Fase 10.2 — Seguridad HTTP y administración de sesiones](docs/fases/10-2-seguridad-sesiones.md)
- [Fase 11.1 — Carga de Roles activos y base de autorización](docs/fases/11-1-base-autorizacion-roles.md)
- [Fase 11.2 — Matriz de autorización y protección del propietario](docs/fases/11-2-matriz-autorizacion-propietario.md)
- [Fase 12.1 — Modelo persistente de Menús y Procesos](docs/fases/12-1-modelo-menus-procesos.md)
- [Fase 12.2 — Administración REST de Menús, Procesos y relaciones](docs/fases/12-2-administracion-rest-menus-procesos.md)
- [Guía Postman de asignaciones Usuario–Rol](docs/postman/rolusu.md)
- [Guía Postman de la API Persona](docs/postman/persona.md)
- [Guía Postman de la API Usuario](docs/postman/usuario.md)
- [Guía Postman de la API Roles](docs/postman/rol.md)
- [Guía Postman de Autenticación](docs/postman/auth.md)

La documentación generada por Spring Initializr se conserva en `HELP.md`.
