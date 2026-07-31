# Etapa 2 — Personas, usuarios y roles

## Módulo Persona

La API de Persona se implementa como parte de un monolito modular. El controlador atiende HTTP y delega en el servicio; el servicio coordina reglas y transacciones; el repositorio persiste; el mapper separa contratos API y entidad JPA.

El CRUD usa DTO de entrada y salida, paginación y `ProblemDetail` para errores. Las validaciones de los DTO protegen el contrato antes de la capa de servicio; las restricciones de PostgreSQL preservan la integridad del esquema.

`estado` es un `SMALLINT`: `1` representa activa y `0` inactiva. Activar o desactivar conserva la Persona y es idempotente. La eliminación es física, responde `204 No Content` y no equivale a desactivar.

Las capas se prueban de forma complementaria: mapper y servicio de manera unitaria, controlador con MVC y flujo real contra PostgreSQL. Flyway conserva V1 y V2 como fuente de verdad del esquema; Hibernate solo lo valida.
