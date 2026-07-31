# Fase 05 — CRUD de Persona

## Estado

`PENDIENTE`. El CRUD y las operaciones de estado están implementados provisionalmente; las pruebas automatizadas específicas siguen pendientes y son necesarias antes de cerrar la fase.

## Segmentación del módulo person

```text
person
├── controller/PersonaController.java
├── dto/{CreatePersonaRequest, UpdatePersonaRequest, PersonaResponse, PageResponse}.java
├── entity/Persona.java
├── mapper/PersonaMapper.java
├── repository/PersonaRepository.java
├── security/ (vacío)
└── service/PersonaService.java
    └── impl/PersonaServiceImpl.java
```

El controlador resuelve HTTP y delega solo a `PersonaService`. Los DTO son contratos API. La entidad contiene el mapeo JPA. El mapper convierte y normaliza sin consultar la base. El repositorio solo persiste. La interfaz de servicio expone el CRUD y las operaciones de activación y desactivación; la implementación coordina mapper, repositorio, duplicidad de CI, excepciones y transacciones.

Flujo: `PersonaController → PersonaService → PersonaServiceImpl → PersonaRepository → PostgreSQL`; el mapper opera entre DTO y entidad desde la implementación. No hay dependencias inversas ni código en `security`.

## Operaciones de estado

- `PATCH /api/v1/personas/{codper}/desactivar` conserva la Persona y establece `estado = 0`.
- `PATCH /api/v1/personas/{codper}/activar` conserva la Persona y establece `estado = 1`.
- Ambas operaciones son idempotentes, devuelven `PersonaResponse` y responden `404 RESOURCE_NOT_FOUND` cuando la Persona no existe.
- `DELETE /api/v1/personas/{codper}` conserva su comportamiento de eliminación física: devuelve `204 No Content`, no modifica `estado` y elimina el registro.

## Registro provisional

- No se agregaron pruebas automatizadas en esta ejecución.
- No se agregaron migraciones ni se modificó la estructura de `personas`.
- No se agregó código en `security` ni se inició la Fase 06.
- La guía de Postman en `docs/postman/persona.md` y los índices documentales reflejan los endpoints reales.
- Las pruebas de mapper, servicio, MVC e integración siguen pendientes antes de cerrar la Fase 05.

## Validación provisional

- `./mvnw.cmd clean test` finalizó con `BUILD SUCCESS`: 17 pruebas, 0 fallos y 0 errores.
- `contextLoads` pasó; PostgreSQL conectó, Flyway validó V1 y V2 sin aplicar migraciones, e Hibernate validó el esquema configurado.
- No existe V3 y el esquema no se modificó.
- La verificación HTTP manual no pudo iniciarse en el entorno de ejecución antes de exponer el puerto 9090; no se dejaron registros ficticios. El comportamiento de las nuevas operaciones fue revisado estáticamente en servicio y controlador. Las pruebas automatizadas específicas permanecen pendientes.
