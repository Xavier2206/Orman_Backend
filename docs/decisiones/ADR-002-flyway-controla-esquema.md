# ADR-002 — Flyway controla el esquema

- **Estado:** Aceptada
- **Fecha:** 2026-07-27

## Contexto

El esquema PostgreSQL debe evolucionar de forma reproducible y revisable. La generación automática de Hibernate puede introducir cambios implícitos difíciles de controlar entre ambientes.

## Decisión

Flyway será responsable de crear y modificar el esquema mediante migraciones versionadas. Hibernate no usará `ddl-auto=create` ni `ddl-auto=update`; cuando corresponda, usará validación del esquema.

## Consecuencias positivas

- Historial explícito y reproducible de cambios.
- Revisión previa de SQL, restricciones e índices.
- Consistencia entre ambientes.
- Hibernate puede detectar divergencias sin alterar estructuras.

## Consecuencias negativas

- Cada cambio de modelo requiere diseñar una migración.
- Las migraciones exigen disciplina de orden y compatibilidad.
- Los errores deben corregirse mediante nuevas versiones si una migración ya fue aplicada.
