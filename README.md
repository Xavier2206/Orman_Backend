# ORMAN-BACKEND

Backend de ORMAN construido con Java y Spring Boot. El proyecto se encuentra en su fase de planificación y fundación técnica; todavía no implementa endpoints ni funcionalidades de negocio.

## Estado actual

**Fase 01 — Revisión y normalización del proyecto Spring Boot: COMPLETADA**

La estructura técnica inicial está normalizada: el proyecto usa el nombre lógico ORMAN-BACKEND, Java 21, configuración YAML y un Maven Wrapper funcional. La compilación principal y de pruebas pasa; la carga del contexto permanece pendiente de la configuración de datasource de la Fase 02.

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

El servidor se configura para usar el puerto `9090`. PostgreSQL aún no está configurado y no existe una API disponible.

## Requisitos

- JDK 21
- Una terminal compatible con el Maven Wrapper

PostgreSQL será necesario a partir de la fase dedicada a su configuración, no para interpretar la documentación de esta fase.

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

El arranque puede requerir configuración adicional mientras las dependencias de persistencia estén presentes y PostgreSQL todavía no esté configurado. Esa configuración pertenece a las siguientes fases.

## Documentación

- [Reglas permanentes de trabajo](AGENTS.md)
- [Registro de cambios](CHANGELOG.md)
- [Índice de documentación](docs/README.md)
- [Plan general](docs/PLAN_GENERAL.md)
- [Etapa 1 — Fundación técnica](docs/etapas/etapa-01-fundacion-tecnica.md)
- [Fase 00 — Planificación general](docs/fases/00-planificacion-general.md)
- [Fase 01 — Revisión y normalización Spring Boot](docs/fases/01-revision-normalizacion-spring-boot.md)

La documentación generada por Spring Initializr se conserva en `HELP.md`.
