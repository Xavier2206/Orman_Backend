# ADR-003 — Git manual

- **Estado:** Aceptada
- **Fecha:** 2026-07-27

## Contexto

El usuario desea conservar el control explícito sobre ramas, selección de archivos, commits y publicación de cambios.

## Decisión

Git será ejecutado manualmente por el usuario. Codex no realizará operaciones Git de escritura, no cambiará ramas, no creará commits y no publicará ni revertirá cambios.

## Consecuencias positivas

- El usuario revisa exactamente qué entra al historial.
- Se reduce el riesgo de incluir o revertir cambios no relacionados.
- Los límites de responsabilidad quedan claros.

## Consecuencias negativas

- El cierre requiere pasos manuales adicionales.
- Codex no puede confirmar el estado final del commit o del remoto.
- La omisión de un archivo durante el agregado debe ser detectada por la revisión del usuario.
