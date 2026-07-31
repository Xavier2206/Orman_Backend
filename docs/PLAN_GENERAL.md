# Plan general de ORMAN-BACKEND

## Objetivo general

Construir de forma incremental un backend mantenible para ORMAN, comenzando por la gestión de personas, usuarios y roles, e incorporando posteriormente autenticación, autorización, calidad y preparación para producción. La solución parte como un monolito modular con Java 21, Spring Boot, Maven y PostgreSQL.

## Forma de trabajo

El desarrollo se divide en etapas temáticas y fases acotadas. Solo una fase autorizada puede estar activa; cada fase debe documentar su alcance, cambios, validaciones y pendientes. La teoría transversal se mantiene en el documento de su etapa. Al cerrar una fase se actualizan este plan, su documento individual y `CHANGELOG.md`.

Las fases posteriores dependen de las bases establecidas por las anteriores. No se debe adelantar persistencia, seguridad, APIs o infraestructura antes de la fase que las autoriza.

## Estados permitidos

| Estado | Significado |
|---|---|
| `PENDIENTE` | Aún no iniciada. |
| `EN ANÁLISIS` | Se están confirmando alcance, requisitos o decisiones. |
| `EN DESARROLLO` | La fase está autorizada y en ejecución. |
| `BLOQUEADA` | Existe un impedimento que evita continuar o cerrar. |
| `COMPLETADA` | Cumplió su objetivo, validaciones y documentación. |
| `CANCELADA` | Se decidió no continuar con la fase. |

## Estado actual

- Etapa activa: **ETAPA 2 — Personas, usuarios y roles**
- Fase activa: **Fase 06 — Modelo y migración de Usuario**
- Estado de la Fase 06: **COMPLETADA**
- Resultado: tabla `usuarios`, relación uno a uno con `personas`, entidad, repositorio y 65 pruebas validadas contra PostgreSQL.
- Fecha de actualización: **2026-07-31**


## Etapas y fases previstas

### ETAPA 1 — Fundación técnica

Teoría: [Etapa 1 — Fundación técnica](etapas/etapa-01-fundacion-tecnica.md)

| Fase | Estado | Dependencia | Documento |
|---|---|---|---|
| 00 — Planificación general y estructura documental | `COMPLETADA` | Ninguna | [Documento de Fase 00](fases/00-planificacion-general.md) |
| 01 — Revisión y normalización del proyecto Spring Boot | `COMPLETADA` | Fase 00 | [Documento de Fase 01](fases/01-revision-normalizacion-spring-boot.md) |
| 02 — Configuración de PostgreSQL y Flyway | `COMPLETADA` | Fase 01 | [Documento de Fase 02](fases/02-configuracion-postgresql-flyway.md) |
| 03 — Infraestructura común y manejo de errores | `COMPLETADA` | Fases 01 y 02 | [Documento de Fase 03](fases/03-infraestructura-comun-manejo-errores.md) |

### ETAPA 2 — Personas, usuarios y roles

Teoría: [Etapa 2 — Personas, usuarios y roles](etapas/etapa-02-personas-usuarios-roles.md).

| Fase | Estado | Dependencia | Documento |
|---|---|---|---|
| 04 — Modelo y migración de personas | `COMPLETADA` | Fases 02 y 03 | [Documento de Fase 04](fases/04-modelo-migracion-persona.md) |
| 05 — API CRUD de personas | `COMPLETADA` | Fase 04 | [Documento de Fase 05](fases/05-crud-persona.md) |
| 06 — Modelo y migración de usuario | `COMPLETADA` | Fases 04 y 05 | [Documento de Fase 06](fases/06-modelo-migracion-usuario.md) |
| 07 — Gestión administrativa de usuarios | `PENDIENTE` | Fase 06 | Documento pendiente de creación |
| 08 — Roles y relación usuario-rol | `PENDIENTE` | Fases 06 y 07 | Documento pendiente de creación |

### ETAPA 3 — Autenticación y autorización

Teoría: Documento pendiente de creación.

| Fase | Estado | Dependencia | Documento |
|---|---|---|---|
| 09 — Spring Security y autenticación | `PENDIENTE` | Etapa 2 | Documento pendiente de creación |
| 10 — Autenticación mediante JWT | `PENDIENTE` | Fase 09 | Documento pendiente de creación |
| 11 — Autorización por roles | `PENDIENTE` | Fases 08 y 10 | Documento pendiente de creación |
| 12 — Menús y procesos dinámicos | `PENDIENTE` | Fase 11 | Documento pendiente de creación |
| 13 — OTP y `login_challenges` | `PENDIENTE` | Fases 09 y 10 | Documento pendiente de creación |

### ETAPA 4 — Calidad y producción

Teoría: Documento pendiente de creación.

| Fase | Estado | Dependencia | Documento |
|---|---|---|---|
| 14 — Documentación OpenAPI | `PENDIENTE` | APIs principales implementadas | Documento pendiente de creación |
| 15 — Pruebas de integración | `PENDIENTE` | Infraestructura y APIs estables | Documento pendiente de creación |
| 16 — Auditoría | `PENDIENTE` | Modelo y seguridad estables | Documento pendiente de creación |
| 17 — Preparación para producción | `PENDIENTE` | Fases 14, 15 y 16 | Documento pendiente de creación |

### ETAPA FUTURA — Módulos adicionales

**Estado:** `PENDIENTE DE ANÁLISIS`

Las tablas, relaciones, reglas de negocio y fases de los módulos adicionales se definirán cuando el usuario proporcione la información correspondiente.

Esta etapa no tendrá fases numeradas por el momento.

## Dependencias generales

La fundación técnica habilita el modelo de personas, usuarios y roles. Ese núcleo permite incorporar autenticación y autorización. La etapa de calidad y producción documenta, integra y verifica las capacidades confirmadas anteriormente. Los módulos adicionales solo entrarán al plan cuando el usuario proporcione su información. Una dependencia expresa orden técnico, pero cada fase requiere además autorización explícita del usuario.
