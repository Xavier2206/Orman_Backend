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

El núcleo contiene `personas`, `usuarios` y roles. La tabla relacional Usuario–Rol aún no tiene nombre definitivo: antes de la Fase 08 se elegirá entre `usuarios_roles`, `rol_usuario` u otro nombre aprobado. No se crean tablas fuera de la fase que las autorice.

## Seguridad

La Fase 07 introducirá BCrypt mediante `PasswordEncoder`; puede usar `spring-security-crypto` sin activar todavía Spring Security HTTP completo. La Fase 09 validará credenciales, la Fase 10 implementará JWT y una sola sesión activa mediante `sesiones_usuario`, y la Fase 11 aplicará autorización por roles. Ningún secreto, contraseña, hash, token u OTP deberá exponerse o registrarse.

`tipo_persona` es clasificación de negocio de Persona y no sustituye roles, permisos ni autorización. JWT, OTP y Spring Security HTTP no forman parte de la arquitectura ejecutable actual.

## Evolución

La modularidad facilitará crecer dentro del mismo despliegue. Una separación en microservicios solo podría considerarse ante necesidades técnicas y operativas demostrables; no es parte del plan actual.

## Decisiones relacionadas

- [ADR-001 — Monolito modular](../decisiones/ADR-001-monolito-modular.md)
- [ADR-002 — Flyway controla el esquema](../decisiones/ADR-002-flyway-controla-esquema.md)
- [ADR-004 — Configuración YAML](../decisiones/ADR-004-configuracion-yaml.md)
- [ADR-005 — Uso controlado de Lombok](../decisiones/ADR-005-uso-controlado-lombok.md)
