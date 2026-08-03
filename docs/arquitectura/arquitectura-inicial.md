# Arquitectura inicial

## Visión

ORMAN-BACKEND comenzará como un monolito modular: una sola aplicación Spring Boot y un único artefacto desplegable, dividido internamente por funcionalidades del dominio. Esta decisión mantiene simple la operación inicial y exige límites claros entre módulos.

## Convenciones base

- Paquete raíz: `com.orman.backend`.
- Organización: paquetes por funcionalidad o dominio.
- Plataforma: Java 21, Spring Boot y Maven.
- Configuración principal: `application.yml`.
- Persistencia: PostgreSQL, Spring Data JPA e Hibernate.
- Evolución del esquema: Flyway.
- Puerto HTTP: `9090`.

## Límites y dependencias

- Cada funcionalidad debe concentrar sus reglas y exponer interacciones deliberadas.
- Las capas internas no deben mezclarse ni saltarse sin justificación.
- Los módulos no deben formar dependencias circulares.
- Las entidades de persistencia no serán contratos directos de las APIs.
- Los componentes comunes solo se crearán cuando resuelvan una necesidad compartida real.
- La infraestructura transversal de errores reside en `common.error` y `common.exception`.

## Datos y esquema

PostgreSQL será la fuente persistente. Flyway creará y modificará el esquema mediante migraciones versionadas; Hibernate validará la correspondencia. No se usará generación automática `create` o `update`.

El núcleo contiene `personas`, `usuarios`, `roles`, `rolusu` y `sesiones_usuario`. `rolusu` materializa Usuario–Rol; `sesiones_usuario` pertenece al módulo `auth`, referencia Usuario de forma unidireccional y limita a una sesión activa por `(login, device_id)`.

## Seguridad

La Fase 07 introdujo BCrypt y la Fase 09 validó credenciales. La subfase 10.1 incorpora JWT HS256 y refresh opaco con sesiones por dispositivo. Nimbus JOSE + JWT es la única biblioteca JWT; la clave se obtiene del entorno y debe tener al menos 32 bytes. Ningún secreto, contraseña, hash, token u OTP se expone o registra.

`tipo_persona` es clasificación de negocio y no sustituye roles, permisos ni autorización. Los JWT no contienen Roles. Spring Security HTTP, filtros, protección de rutas, logout y autorización siguen ausentes y pertenecen a 10.2/11.

El módulo `auth` valida Usuario, Persona y BCrypt, actualiza `ultimo_acceso` UTC, crea sesiones y emite tokens mediante login. `POST /api/v1/auth/refresh` rota el refresh bajo bloqueo pesimista. WEB usa cookie HttpOnly y MOBILE JSON. La configuración HTTP completa permanece pendiente.

## Evolución

La modularidad facilitará crecer dentro del mismo despliegue. Una separación en microservicios solo podría considerarse ante necesidades técnicas y operativas demostrables; no es parte del plan actual.

## Decisiones relacionadas

- [ADR-001 — Monolito modular](../decisiones/ADR-001-monolito-modular.md)
- [ADR-002 — Flyway controla el esquema](../decisiones/ADR-002-flyway-controla-esquema.md)
- [ADR-004 — Configuración YAML](../decisiones/ADR-004-configuracion-yaml.md)
- [ADR-005 — Uso controlado de Lombok](../decisiones/ADR-005-uso-controlado-lombok.md)
