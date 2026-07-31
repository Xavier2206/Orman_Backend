# Etapa 2 — Personas, usuarios y roles

## Módulo Persona

La API de Persona se implementa como parte de un monolito modular. El controlador atiende HTTP y delega en el servicio; el servicio coordina reglas y transacciones; el repositorio persiste; el mapper separa contratos API y entidad JPA.

El CRUD usa DTO de entrada y salida, paginación y `ProblemDetail` para errores. Las validaciones de los DTO protegen el contrato antes de la capa de servicio; las restricciones de PostgreSQL preservan la integridad del esquema.

`estado` es un `SMALLINT`: `1` representa activa y `0` inactiva. Activar o desactivar conserva la Persona y es idempotente. La eliminación es física, responde `204 No Content` y no equivale a desactivar.

Las capas se prueban de forma complementaria: mapper y servicio de manera unitaria, controlador con MVC y flujo real contra PostgreSQL. Flyway conserva V1 y V2 como fuente de verdad del esquema; Hibernate solo lo valida.

## Módulo Usuario

La Fase 06 incorpora `usuarios` como identidad persistente asociada a una Persona. La relación es uno a uno: una Persona puede no tener Usuario y un Usuario requiere exactamente una Persona. `usuarios.codper` es único y referencia `personas.codper` con `ON DELETE RESTRICT`, por lo que una Persona con Usuario no puede eliminarse físicamente.

`login` es la clave primaria natural asignada, `estado` es un `SMALLINT` con `1` activo y `0` inactivo, y los defaults de `estado` y `fecha_creacion` son generados por PostgreSQL. La entidad usa inserción dinámica para respetarlos. Los estados de Persona y Usuario son independientes; la futura autenticación deberá exigir ambos activos, sin que esta fase implemente esa regla.

La columna `passwd` se reserva para hashes seguros futuros. No hay aún BCrypt, Spring Security, autenticación, roles, JWT, sesiones ni API administrativa. La futura regla de una sesión activa se implementará mediante `sesiones_usuario` en una fase autorizada posterior.
