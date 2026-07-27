# ADR-005 — Uso controlado de Lombok

- **Estado:** Aceptada
- **Fecha:** 2026-07-27

## Contexto

Lombok reduce código repetitivo, pero algunas anotaciones generan métodos con efectos indeseados en entidades JPA, relaciones y datos sensibles.

## Decisión

Lombok está permitido de forma controlada. Se preferirán anotaciones específicas y no se usará `@Data` indiscriminadamente en entidades JPA. `toString`, `equals` y `hashCode` no incluirán relaciones JPA ni información sensible.

## Consecuencias positivas

- Menos código mecánico sin abandonar decisiones explícitas.
- Menor riesgo de ciclos, cargas inesperadas o exposición de datos.
- Entidades y objetos mantienen una intención más clara.

## Consecuencias negativas

- Requiere revisar cada anotación y su código generado.
- Puede existir algo más de código explícito.
- El IDE y la compilación necesitan soporte correcto para procesamiento de anotaciones.
