# ETAPA 4.1 — Propiedades, Unidades y Fotografías

## Estado

`COMPLETADA` el 2026-09-10. Esta etapa implementa exclusivamente el módulo
backend de Propiedades, Unidades y UnidadFotos. No incorpora Contratos, Cuotas,
Pagos, Recibos ni Notificaciones.

## Diseño aprobado

- El módulo reside en `com.orman.backend.property`, con `controller`,
  `dto.request`, `dto.response`, `entity`, `mapper`, `repository`, `service` y
  `service.impl`.
- `PropiedadEntity` referencia a `Persona`; `UnidadEntity` referencia a
  `PropiedadEntity`; y `UnidadFotoEntity` referencia a `UnidadEntity`. Todas
  usan `@ManyToOne(fetch = FetchType.LAZY)` desde hijo hacia padre, sin
  colecciones inversas ni `CascadeType.ALL`.
- Flyway V10 crea `propiedades`, `unidades` y `unidad_fotos`, con FKs
  `ON DELETE RESTRICT`, montos `NUMERIC(14,2)`, coordenadas `NUMERIC(9,6)`,
  checks de rango y estados, orden único y portada única parcial por Unidad.
- `url` de UnidadFoto y `portada_url` son URLs HTTP/HTTPS administradas por
  API. La portada interna de Propiedad se documenta como ampliación V14 abajo.

### Ampliación posterior: portada interna de Propiedad

V14 agrega `propiedades.portada_ref` y una portada interna gestionada por
ORMAN. La imagen se recibe por multipart, se valida por MIME y contenido real,
se normaliza a JPEG y se almacena en filesystem configurable sin exponer la
ruta física. Se dispone de `PUT/GET/DELETE
/api/v1/propiedades/{codprop}/portada`, protegido por `ROLE_PROPIETARIO` y
ownership efectivo. `portada_url` conserva su semántica de URL HTTP/HTTPS para
compatibilidad, y las fotografías de `UnidadFoto` no cambian.

## Seguridad

Los controladores requieren `ROLE_PROPIETARIO`. Los servicios resuelven la
Persona del `AuthenticatedUser` y comparan su `codper` con
`propiedades.codper_propietaria`; un propietario solo administra sus recursos.

No se modificaron JWT, sesiones, `SecurityConfig`, Roles, Menús, Procesos ni el
sistema global de autorización. No existe alcance de ADMINISTRADOR inmobiliario;
`tipo_persona` no concede permisos.

## API implementada

| Recurso | Rutas |
|---|---|
| Propiedades | `POST/GET /api/v1/propiedades`; `GET/PUT /api/v1/propiedades/{codprop}`; `PATCH .../{codprop}/activar`; `PATCH .../{codprop}/desactivar` |
| Unidades | `POST/GET /api/v1/propiedades/{codprop}/unidades`; `GET/PUT /api/v1/unidades/{coduni}` |
| UnidadFotos | `POST/GET /api/v1/unidades/{coduni}/fotos`; `PUT/DELETE /api/v1/unidades/{coduni}/fotos/{id}`; `PATCH .../{id}/portada` |

La portada interna de Propiedad utiliza `PUT/GET/DELETE
/api/v1/propiedades/{codprop}/portada` y mantiene separada la referencia
externa `portada_url` de la referencia interna `portada_ref`.

### Ampliación posterior: disponibilidad contractual de Unidad

El 2026-09-17, sin modificar el esquema ni las reglas de creación de
Contratos, `UnidadResponse` incorporó `disponibleParaContrato`. El endpoint
`GET /api/v1/propiedades/{codprop}/unidades` deriva el campo a partir de los
Contratos `PROGRAMADO` y `VIGENTE`, que son los únicos estados bloqueantes
según ETAPA 4.2; `FINALIZADO` y `RESCINDIDO` dejan la Unidad disponible.

La página de Unidades conserva su consulta y paginación existentes y resuelve
los IDs bloqueados mediante una consulta agregada por lote, filtrada por la
propietaria autenticada. No se expone la relación JPA ni se incorpora una
colección inversa en `UnidadEntity`. Las respuestas individuales de Unidad
también informan el mismo campo calculado.

Las creaciones responden `201 Created` y `Location`; la eliminación de foto
responde `204 No Content`. Los errores usan `ProblemDetail` con
`application/problem+json` y códigos estables existentes.

## Archivos

### Creados

- `src/main/resources/db/migration/V10__create_propiedades_unidades_unidad_fotos_tables.sql`.
- `src/main/resources/db/migration/V14__add_propiedades_portada_ref.sql` y la
  configuración/servicio de portada interna de Propiedad.
- Módulo `src/main/java/com/orman/backend/property` con entidades, DTOs,
  mappers, repositories, servicios y controladores.
- Pruebas del módulo en `src/test/java/com/orman/backend/property`.
- `docs/postman/property.md`.

### Modificados

- Pruebas existentes que enumeran el historial y tablas de Flyway, para
  reconocer V10 sin modificar el comportamiento de módulos anteriores.
- `docs/PLAN_GENERAL.md`, `CHANGELOG.md`, documentación de arquitectura y de
  base de datos, e índice Postman.

## Validación

- Compilación tras migración, entidades, repositories, DTOs/mappers, servicios
  y controladores: `BUILD SUCCESS`.
- Pruebas de mapper, MVC, propiedad efectiva, persistencia y constraints sobre
  PostgreSQL real.
- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 262 pruebas, 0 fallos, 0 errores
  y 0 omitidas. Flyway validó V1–V10 e Hibernate validó `ddl-auto=validate`.
- Validación posterior de la portada interna V14: `./mvnw.cmd clean test`:
  **BUILD SUCCESS**; 311 pruebas, 0 fallos, 0 errores y 0 omitidas. PostgreSQL
  real validó Flyway V1–V14 y Hibernate mantuvo `ddl-auto=validate`.

Validación posterior de disponibilidad contractual: se agregaron pruebas de
estados `PROGRAMADO`, `VIGENTE`, `FINALIZADO` y `RESCINDIDO`, unidad sin
contratos, aislamiento por propietaria, paginación y ausencia de N+1 en la
consulta de la página. `./mvnw.cmd clean test`: **BUILD SUCCESS**; 389
pruebas, 0 fallos, 0 errores y 0 omitidas. PostgreSQL real validó Flyway
V1–V18 y Hibernate mantuvo `ddl-auto=validate`.

## Riesgos y pendientes

- La disponibilidad contractual, ocupación, cuotas, pagos y recibos quedan para
  ETAPA 4.2 y posteriores.
- La carga de archivos de UnidadFoto no está autorizada; el módulo administra
  referencias URL.
- ETAPA 4.2.1 requiere autorización explícita antes de comenzar.
