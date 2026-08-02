# Etapa 2 — Personas, usuarios y roles

## Módulo Persona

La API de Persona se implementa como parte de un monolito modular. El controlador atiende HTTP y delega en el servicio; el servicio coordina reglas y transacciones; el repositorio persiste; el mapper separa contratos API y entidad JPA.

El CRUD usa DTO de entrada y salida, paginación y `ProblemDetail` para errores. Las validaciones de los DTO protegen el contrato antes de la capa de servicio; las restricciones de PostgreSQL preservan la integridad del esquema.

`estado` es un `SMALLINT`: `1` representa activa y `0` inactiva. Activar o desactivar conserva la Persona y es idempotente. La eliminación es física, responde `204 No Content` y no equivale a desactivar.

Las capas se prueban de forma complementaria: mapper y servicio de manera unitaria, controlador con MVC y flujo real contra PostgreSQL. Flyway conserva V1 y V2 como fuente de verdad del esquema; Hibernate solo lo valida.

## Módulo Usuario

La Fase 06 incorpora `usuarios` como identidad persistente asociada a una Persona. La relación es uno a uno: una Persona puede no tener Usuario y un Usuario requiere exactamente una Persona. `usuarios.codper` es único y referencia `personas.codper` con `ON DELETE RESTRICT`, por lo que una Persona con Usuario no puede eliminarse físicamente.

`login` es la clave primaria natural asignada, `estado` es un `SMALLINT` con `1` activo y `0` inactivo, y los defaults de `estado` y `fecha_creacion` son generados por PostgreSQL. La entidad usa inserción dinámica para respetarlos. Los estados de Persona y Usuario son independientes; la autenticación futura exigirá ambos activos.

La Fase 07 incorporó la administración REST de Usuario y el almacenamiento de contraseñas mediante `PasswordEncoder` con BCrypt, usando `spring-security-crypto` sin activar Spring Security HTTP completo. Ninguna contraseña ni hash se devuelve o registra. La actualización administrativa se limita a `estado`; login y Persona asociada son inmutables.

La administración de Usuario, los roles, la autenticación, JWT, las sesiones y la autorización son responsabilidades separadas. La Fase 08 administrará roles; la Fase 09 validará credenciales; la Fase 10 implementará JWT y una sola sesión activa mediante `sesiones_usuario`; la Fase 11 protegerá endpoints por roles. OTP, menús y procesos corresponden a las Fases 13 y 12, respectivamente. La desactivación de Usuario conserva el registro; no se implementó eliminación física.

`tipo_persona` continúa siendo una clasificación de negocio de Persona y no sustituye roles, permisos ni autorización.
