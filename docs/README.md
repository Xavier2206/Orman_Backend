# Documentación de ORMAN-BACKEND

Este directorio concentra la planificación, la teoría por etapa, el historial por fase, las decisiones arquitectónicas y el análisis inicial del modelo de datos.

## Navegación

- [Plan general del desarrollo](PLAN_GENERAL.md)
- [Etapa 1 — Fundación técnica](etapas/etapa-01-fundacion-tecnica.md)
- [Etapa 2 — Personas, usuarios y roles](etapas/etapa-02-personas-usuarios-roles.md)
- [Índice de fases](fases/README.md)
- [Fase 00 — Planificación general](fases/00-planificacion-general.md)
- [Fase 01 — Revisión y normalización Spring Boot](fases/01-revision-normalizacion-spring-boot.md)
- [Fase 02 — Configuración PostgreSQL y Flyway](fases/02-configuracion-postgresql-flyway.md)
- [Fase 03 — Infraestructura común y manejo global de errores](fases/03-infraestructura-comun-manejo-errores.md)
- [Fase 04 — Modelo y migración de Persona](fases/04-modelo-migracion-persona.md)
- [Guía Postman de la API Persona](postman/persona.md)
- [Fase 05 — API CRUD de Persona](fases/05-crud-persona.md)
- [Fase 06 — Modelo y migración de Usuario](fases/06-modelo-migracion-usuario.md)
- [Fase 07 — Administración de Usuarios y Contraseñas](fases/07-administracion-usuarios-contrasenas.md)
- [Guía Postman de la API Usuario](postman/usuario.md)
- [Fase 08 — Roles y relación Usuario–Rol](fases/08-roles-relacion-usuario-rol.md)
- [Guía Postman de Roles](postman/rol.md)
- [Arquitectura inicial](arquitectura/arquitectura-inicial.md)
- [Documentación de base de datos](database/README.md)

## Fase 09

- [Fase 09 — Autenticación y validación de credenciales](fases/09-autenticacion-validacion-credenciales.md)
- [Guía Postman de Autenticación](postman/auth.md)

## Decisiones arquitectónicas

- [ADR-001 — Monolito modular](decisiones/ADR-001-monolito-modular.md)
- [ADR-002 — Flyway controla el esquema](decisiones/ADR-002-flyway-controla-esquema.md)
- [ADR-003 — Git manual](decisiones/ADR-003-git-manual.md)
- [ADR-004 — Configuración YAML](decisiones/ADR-004-configuracion-yaml.md)
- [ADR-005 — Uso controlado de Lombok](decisiones/ADR-005-uso-controlado-lombok.md)

La teoría se documenta por etapa. Cada fase registra únicamente su alcance, ejecución, validaciones, decisiones y resultado.
