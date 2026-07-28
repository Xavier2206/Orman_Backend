# Arquitectura inicial

## Visión

ORMAN-BACKEND comenzará como un monolito modular: una sola aplicación Spring Boot y un único artefacto desplegable, dividido internamente por funcionalidades del dominio. Esta decisión mantiene simple la operación inicial y exige límites claros entre módulos.

## Convenciones base

- Paquete raíz: `com.orman.backend`.
- Organización: paquetes por funcionalidad o dominio.
- Plataforma: Java 21, Spring Boot y Maven.
- Configuración principal: `application.yml`.
- Persistencia futura: PostgreSQL, Spring Data JPA e Hibernate.
- Evolución del esquema: Flyway.
- Puerto HTTP: `9090`.

Una estructura ilustrativa futura podría contener paquetes como `persona` o `usuario`, cada uno con sus componentes internos necesarios. Esta fase no crea esos paquetes porque aún no existe implementación que los justifique.

## Límites y dependencias

- Cada funcionalidad debe concentrar sus reglas y exponer interacciones deliberadas.
- Las capas internas no deben mezclarse ni saltarse sin justificación.
- Los módulos no deben formar dependencias circulares.
- Las entidades de persistencia no serán contratos directos de las APIs.
- Los componentes comunes solo se crearán cuando resuelvan una necesidad compartida real.
- La infraestructura transversal de errores reside en `common.error` y `common.exception`; no contiene reglas ni tipos de un dominio concreto.

## Datos y esquema

PostgreSQL será la fuente persistente. Flyway creará y modificará el esquema mediante migraciones versionadas; Hibernate validará la correspondencia cuando esa configuración sea incorporada. No se usará generación automática `create` o `update`.

El núcleo inicialmente analizado contiene `personas`, `usuarios`, `roles` y `rol_usuario`, pero ninguna tabla se implementa durante la Fase 00.

## Seguridad

La seguridad se incorporará en sus fases específicas. Las contraseñas se almacenarán mediante hash BCrypt, nunca en texto plano, y ningún secreto, hash, token u OTP deberá exponerse o registrarse. JWT, OTP y Spring Security no forman parte de la arquitectura ejecutable actual.

## Evolución

La modularidad facilitará crecer dentro del mismo despliegue. Una separación en microservicios solo podría considerarse ante necesidades técnicas y operativas demostrables; no es parte del plan actual.

## Decisiones relacionadas

- [ADR-001 — Monolito modular](../decisiones/ADR-001-monolito-modular.md)
- [ADR-002 — Flyway controla el esquema](../decisiones/ADR-002-flyway-controla-esquema.md)
- [ADR-004 — Configuración YAML](../decisiones/ADR-004-configuracion-yaml.md)
- [ADR-005 — Uso controlado de Lombok](../decisiones/ADR-005-uso-controlado-lombok.md)
