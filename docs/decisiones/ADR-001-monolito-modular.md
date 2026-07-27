# ADR-001 — Monolito modular

- **Estado:** Aceptada
- **Fecha:** 2026-07-27

## Contexto

ORMAN-BACKEND inicia con varios dominios relacionados y un equipo que necesita evolucionar el producto por fases. Introducir distribución operativa desde el comienzo aumentaría la complejidad sin una necesidad demostrada.

## Decisión

El backend será un monolito modular, desplegado como una sola aplicación y organizado internamente por funcionalidad bajo `com.orman.backend`. No se crearán microservicios.

## Consecuencias positivas

- Despliegue, pruebas y diagnóstico iniciales más simples.
- Transacciones locales y menor complejidad de comunicación.
- Límites de dominio visibles mediante módulos funcionales.
- Posibilidad de evolucionar sin infraestructura distribuida prematura.

## Consecuencias negativas

- Los límites deben vigilarse para evitar acoplamiento interno.
- Un único despliegue coordina la publicación de todos los módulos.
- Un módulo con consumo excesivo puede afectar al resto del proceso.
