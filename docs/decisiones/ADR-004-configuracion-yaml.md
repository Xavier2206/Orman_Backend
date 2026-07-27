# ADR-004 — Configuración YAML

- **Estado:** Aceptada
- **Fecha:** 2026-07-27

## Contexto

Spring Boot admite distintos formatos de propiedades. La configuración crecerá en grupos jerárquicos y necesita un formato principal consistente.

## Decisión

Se utilizará `application.yml` como formato principal de configuración. Los secretos no se escribirán en el archivo y las diferencias de ambiente se incorporarán cuando sus fases definan variables o perfiles reales.

## Consecuencias positivas

- Representación clara de propiedades jerárquicas.
- Un único formato principal reduce duplicidades.
- Facilita agrupar configuración relacionada.

## Consecuencias negativas

- La indentación incorrecta puede cambiar o invalidar la configuración.
- Debe evitarse mezclar sin necesidad archivos YAML y properties.
- Los valores escalares ambiguos requieren atención al editar.
