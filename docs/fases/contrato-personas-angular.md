# Contrato de Personas para integración Angular

## Estado

`COMPLETADA` el 2026-08-17. Ampliación acotada del contrato de Personas sin iniciar una fase futura ni cambiar el esquema.

## Resultado

- El listado existente filtra en PostgreSQL por `q`, `tipoPersona` y `estado`, conserva `PageResponse` y admite `sort=ap,asc`.
- Listado y detalle incluyen Usuario 1:1 mínimo (`login`, `estado`) y capacidades calculadas con el actor, authorities reales y reglas existentes.
- Usuarios y comprobaciones de propietario se resuelven por lote, evitando N+1 por Persona.
- `GET /api/v1/personas/resumen` devuelve conteos globales agregados de Personas,
  sin filtros ni paginación; `conUsuario` cuenta Usuarios vinculados activos o
  inactivos.
- Las fotos se validan mediante `ImageIO` como JPEG/PNG, máximo 2 MiB, y se almacenan localmente fuera del código. `Persona.foto` conserva solo la referencia actual.

No se agregaron migraciones, tablas, PDF, cloud storage, Base64/BLOB, historial, cambios de BCrypt ni roles en JWT. DELETE Persona sigue siendo físico; PATCH activar/desactivar sigue siendo lógico.
