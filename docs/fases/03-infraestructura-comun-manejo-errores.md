# Fase 03 — Infraestructura común y manejo global de errores

## Identificación

- **Estado:** `COMPLETADA`
- **Fecha de inicio y cierre:** 2026-07-27

## Objetivo

Incorporar infraestructura transversal mínima para respuestas de error HTTP uniformes, seguras, trazables y reutilizables por los módulos posteriores.

## Alcance y estado inicial

Al iniciar, las fases 00, 01 y 02 estaban completadas. PostgreSQL, Flyway y `contextLoads` funcionaban; el esquema público solo contenía `flyway_schema_history`. No existían migraciones, tablas de negocio, entidades, repositorios, servicios, controladores ni funcionalidades de negocio.

Esta fase implementó exclusivamente contrato de error, excepciones reutilizables, consejo global, validación, JSON inválido, logging seguro, trazabilidad local y pruebas. No implementó Persona, Usuario, Rol, seguridad, JWT, OTP, CRUD, migraciones ni elementos de la Fase 04.

## Arquitectura implementada

| Paquete | Clases | Responsabilidad |
|---|---|---|
| `common.error` | `ErrorCode`, `GlobalExceptionHandler` | Contrato HTTP, códigos y traducción global a `ProblemDetail`. |
| `common.exception` | `ApplicationException`, `ResourceNotFoundException`, `BusinessRuleException`, `ConflictException` | Fallos esperables reutilizables, sin acoplamiento a HTTP. |

`GlobalExceptionHandler` tiene precedencia alta sobre el manejador automático de Spring para garantizar que validación y JSON inválido usan el mismo contrato.

## Contrato de error

Se usa `application/problem+json` y `ProblemDetail` conforme a RFC 9457.

| Propiedad | Origen | Uso |
|---|---|---|
| `status`, `title`, `detail`, `instance` | RFC 9457 | Estado, descripción segura y ruta de la solicitud. |
| `errorCode` | Extensión | Código interno estable. |
| `timestamp` | Extensión | Instante ISO-8601 de construcción de la respuesta. |
| `traceId` | Extensión | UUID local por respuesta de error. |
| `fieldErrors` | Extensión opcional | Lista ordenada de `{ field, message }` para validación. |

El contrato no devuelve stack traces, SQL, clases internas, credenciales, tokens ni detalles técnicos. Los mensajes de las excepciones esperables deben ser seguros para clientes.

## Códigos, excepciones y mapeo HTTP

| Caso | Código | Estado |
|---|---|---|
| `ResourceNotFoundException` | `RESOURCE_NOT_FOUND` | 404 |
| `ConflictException` | `CONFLICT` | 409 |
| `BusinessRuleException` | `BUSINESS_RULE_VIOLATION` | 422 |
| Bean Validation o `ConstraintViolationException` | `VALIDATION_ERROR` | 400 |
| JSON ilegible | `INVALID_REQUEST` | 400 |
| Excepción inesperada | `INTERNAL_ERROR` | 500 |

Se eligió 422 para reglas de negocio porque la solicitud puede tener sintaxis correcta, pero su contenido no satisface una regla del dominio.

## Configuración, logging y trazabilidad

Se habilitó `spring.mvc.problemdetails.enabled=true`, propiedad oficial de Spring Boot MVC. No se modificaron datasource, Flyway, Hibernate, puerto ni credenciales; tampoco se agregaron dependencias.

Los errores esperables se traducen sin logging ruidoso. Para errores inesperados se registra a nivel `ERROR` el `traceId`, la ruta y el tipo de excepción, sin registrar el mensaje ni la traza que pudiera contener información sensible. El `traceId` es local; no incorpora infraestructura distribuida.

## Pruebas y resultados

Se creó una prueba MVC aislada y un controlador exclusivo de `src/test/java`; no existe endpoint artificial en `src/main`.

- 404 por recurso inexistente.
- 409 por conflicto.
- 422 por regla de negocio.
- 400 con errores de campo ordenados para Bean Validation.
- 400 por JSON inválido.
- 500 seguro sin stack trace ni detalle interno.
- Presencia de código interno, ruta, instante y `traceId`.
- Tipo de contenido `application/problem+json`.
- `contextLoads` contra PostgreSQL y Flyway.

Comando ejecutado: `.\mvnw.cmd clean test`, importando las variables `DB_*` de Windows al proceso sin mostrarlas. Resultado final: **BUILD SUCCESS**, 7 pruebas, 0 fallos, 0 errores y 0 omitidas. PostgreSQL aceptó la conexión, HikariPool inició, Flyway validó 0 migraciones y Hibernate inició con validación de esquema.

El primer intento reveló que el consejo integrado de Spring atendía validación y JSON antes que el consejo global. Se corrigió con `@Order(Ordered.HIGHEST_PRECEDENCE)` y se repitió la validación completa con éxito.

## Archivos

### Creados

- `src/main/java/com/orman/backend/common/error/ErrorCode.java`
- `src/main/java/com/orman/backend/common/error/GlobalExceptionHandler.java`
- `src/main/java/com/orman/backend/common/exception/ApplicationException.java`
- `src/main/java/com/orman/backend/common/exception/ResourceNotFoundException.java`
- `src/main/java/com/orman/backend/common/exception/BusinessRuleException.java`
- `src/main/java/com/orman/backend/common/exception/ConflictException.java`
- `src/test/java/com/orman/backend/common/error/GlobalExceptionHandlerWebMvcTest.java`
- `docs/fases/03-infraestructura-comun-manejo-errores.md`

### Modificados

- `src/main/resources/application.yml`
- `README.md`
- `CHANGELOG.md`
- `docs/PLAN_GENERAL.md`
- `docs/README.md`
- `docs/fases/README.md`
- `docs/etapas/etapa-01-fundacion-tecnica.md`
- `docs/arquitectura/arquitectura-inicial.md`

### Eliminados

- Ninguno.

## Decisiones, riesgos y limitaciones

- `ProblemDetail` evita un DTO paralelo y permite extensiones JSON de forma estándar.
- El UUID por error es suficiente para correlación local; observabilidad distribuida queda fuera de alcance.
- Los módulos futuros deben proporcionar mensajes seguros al lanzar excepciones esperables.
- La infraestructura no sustituye las validaciones ni reglas concretas de cada módulo.

## Criterios de cierre y siguiente fase

- [x] Contrato uniforme y manejo global implementados.
- [x] Validación y JSON inválido responden de forma coherente.
- [x] Los errores inesperados no exponen información interna.
- [x] Pruebas MVC y `contextLoads` pasan.
- [x] PostgreSQL y Flyway permanecen operativos.
- [x] No se crearon migraciones, tablas ni negocio.
- [x] Documentación actualizada sin secretos.

La Fase 03 queda **COMPLETADA**. La siguiente fase autorizable es la **Fase 04 — Modelo y migración de Persona**; no se inició en esta ejecución.
