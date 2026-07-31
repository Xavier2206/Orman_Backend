# Fase 05 — CRUD de Persona

## Estado final

`COMPLETADA` el 2026-07-31. El CRUD, las operaciones de estado y sus pruebas específicas cumplen el alcance autorizado.

## Arquitectura final

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

`PersonaController` depende únicamente de `PersonaService`. `PersonaServiceImpl` contiene transacciones, consulta el repositorio, usa el mapper y refresca la entidad recién creada para devolver los valores generados por PostgreSQL. El flujo es `controller → service → repository → PostgreSQL`; el mapper transforma DTO y entidad dentro del servicio. No se añadió código a `security`.

## Funcionalidad cubierta

- DTO de creación, actualización, respuesta y paginación.
- `POST`, consulta por id, listado paginado, `PUT` y eliminación física.
- `PATCH /api/v1/personas/{codper}/desactivar`: conserva el registro y fija `estado=0`.
- `PATCH /api/v1/personas/{codper}/activar`: conserva el registro y fija `estado=1`.
- Activar y desactivar son idempotentes; `DELETE` elimina físicamente y responde `204`.
- CI duplicado responde `409 CONFLICT`; inexistentes responden `404 RESOURCE_NOT_FOUND`.
- Bean Validation responde `400 VALIDATION_ERROR`; JSON inválido responde `400 INVALID_REQUEST`.
- ProblemDetail conserva `errorCode`, `timestamp`, `traceId`, `instance` y `fieldErrors` cuando corresponde.

## Pruebas implementadas

| Clase | Tipo | Cobertura principal |
|---|---|---|
| `PersonaMapperTest` | Unitaria | Create/update/response, trim, mayúsculas, opcionales a `null`, preservación de `codper` y `fechaRegistro`, estado `null` en creación. |
| `PersonaServiceImplTest` | Unitaria con mocks | CRUD, paginación, conflictos, inexistentes, eliminación física, activar/desactivar idempotentes e interacciones con mapper, repositorio y `EntityManager`. |
| `PersonaControllerWebMvcTest` | MVC | Todos los endpoints reales, Location, PageResponse, ProblemDetail, CI duplicado, inexistentes, JSON inválido y validaciones de los DTO. |
| `PersonaCrudIntegrationTest` | Integración PostgreSQL | Flujo real de creación, defaults, lectura, paginación, actualización, estados, eliminación física, CI único, tablas y Flyway. |
| `PersonaPersistenceIntegrationTest` | Integración existente | Columnas, defaults, restricciones, índices y versiones de migración. |

Las validaciones MVC cubren CI vacío y mayor a 20, nombre vacío y mayor a 60, ap/am mayores a 40, género inválido, estado inválido, correo inválido y mayor a 100, teléfono vacío y mayor a 20, tipoPersona inválido, foto mayor a 255 y JSON mal formado.

## Validación final

Comando ejecutado:

```powershell
.\mvnw.cmd clean test
```

Resultado real: `BUILD SUCCESS`; 56 pruebas, 0 fallos, 0 errores y 0 omitidas.

- `contextLoads` pasó.
- PostgreSQL 17.6 conectó mediante las variables externas `DB_*`.
- Flyway validó exactamente V1 y V2; el esquema quedó en versión 2 y no existe V3.
- Hibernate inició con `ddl-auto=validate` sin generar DDL.
- Las pruebas de integración usan transacciones y rollback; no dejan datos ficticios permanentes ni borran datos ajenos.
- Se confirmó que las únicas tablas públicas son `flyway_schema_history` y `personas`.

## Error real y corrección

La primera integración mostró que, al omitir `estado`, PostgreSQL aplicaba correctamente su default pero la respuesta de creación se mapeaba antes de refrescar la entidad y podía exponer `estado` y `fechaRegistro` como `null`. Se añadió `EntityManager.refresh` inmediatamente después de `saveAndFlush` en `PersonaServiceImpl`. No cambió ningún endpoint, DTO, migración ni tabla.

También se corrigió el generador de cuerpos inválidos de la prueba MVC; era un defecto de la prueba, no de la API.

## Riesgos, limitaciones y cierre

- No hay autenticación, JWT, autorización, carga física de fotos, restauración física ni filtros avanzados; permanecen fuera del alcance de esta fase.
- No se modificaron dependencias, migraciones, esquema, tabla `personas` ni `security`.
- No se inició la Fase 06.
- Los criterios de cierre se cumplen: CRUD, estados, eliminación física, errores, paginación, ProblemDetail y todas las pruebas requeridas pasan.

Archivos creados: cuatro clases de prueba y la teoría de la Etapa 2. Archivos modificados: servicio y documentación de cierre. Archivos eliminados: ninguno.

El usuario debe revisar los cambios y ejecutar Git manualmente.
