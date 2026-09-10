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
- `url` y `portada_url` son URLs HTTP/HTTPS administradas por API. No se agregó
  carga, conversión ni almacenamiento de archivos.

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

Las creaciones responden `201 Created` y `Location`; la eliminación de foto
responde `204 No Content`. Los errores usan `ProblemDetail` con
`application/problem+json` y códigos estables existentes.

## Archivos

### Creados

- `src/main/resources/db/migration/V10__create_propiedades_unidades_unidad_fotos_tables.sql`.
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

## Riesgos y pendientes

- La disponibilidad contractual, ocupación, cuotas, pagos y recibos quedan para
  ETAPA 4.2 y posteriores.
- La carga de archivos de UnidadFoto no está autorizada; el módulo administra
  referencias URL.
- ETAPA 4.2.1 requiere autorización explícita antes de comenzar.
