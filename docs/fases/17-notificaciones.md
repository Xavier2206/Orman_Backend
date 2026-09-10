# ETAPA 4.4 — Notificaciones internas

## Estado

`COMPLETADA` el 2026-09-10. La etapa implementa exclusivamente notificaciones
internas persistentes para el propietario autenticado. No incorpora correo,
WhatsApp, SMS, Firebase Push, aplicación móvil, usuarios inquilinos ni
integraciones externas.

## Implementación

- El módulo reside en `com.orman.backend.notification`, con `controller`,
  `dto.response`, `entity`, `mapper`, `repository`, `service` y `service.impl`.
  No existe `dto.request`: ninguna ruta permite crear notificaciones arbitrarias.
- Flyway V13 crea `notificaciones`, vinculada por `login_destinatario` a
  `usuarios.login`. La tabla conserva tipo, título, mensaje, referencia
  polimórfica (`referencia_tipo`, `referencia_id`), fechas de creación y lectura.
- `NotificacionEntity` no tiene relaciones JPA hacia Cuota, Pago ni Contrato.
  `ReferenciaTipo` se limita a `CUOTA` y `PAGO`; las referencias se validan en
  el servicio generador.
- La lectura se deriva de `fecha_lectura`: nula significa no leída. La acción
  `PATCH /leer` es idempotente y nunca permite volver a no leída.
- La restricción única `(login_destinatario, tipo, referencia_tipo,
  referencia_id)` evita notificaciones lógicas duplicadas.

## Eventos y recordatorios

- `PagoConfirmadoEvent`, `PagoRechazadoEvent` y `ComprobanteRecibidoEvent`
  son eventos internos de Spring publicados por `payment` después de la
  transición correspondiente. El handler del módulo `notification` crea las
  notificaciones en la misma transacción, después de generar el recibo en la
  confirmación.
- `CuotaNotificacionScheduler` se ejecuta diariamente a las 08:00 con zona
  `America/La_Paz`. Considera solamente cuotas `PENDIENTE` y `PARCIAL`:
  genera aviso próximo un día antes o el día de vencimiento, y aviso vencido
  después de la fecha de vencimiento.
- `POST /api/v1/cuotas/{codcuo}/notificar` crea un recordatorio manual para
  cuotas pendientes, parciales o vencidas. La capa transaccional verifica
  `Usuario -> Persona -> Propiedad -> Unidad -> Contrato -> Cuota` antes de
  persistirlo.
- Los mensajes de comprobante son neutrales: no atribuyen el envío a un
  inquilino porque aún no existe ese usuario ni aplicativo móvil.

## API implementada

| Recurso | Rutas |
|---|---|
| Notificaciones | `GET /api/v1/notificaciones`; `GET /api/v1/notificaciones/{codnot}`; `GET /api/v1/notificaciones/resumen`; `PATCH /api/v1/notificaciones/{codnot}/leer` |
| Recordatorio manual | `POST /api/v1/cuotas/{codcuo}/notificar` |

El listado reutiliza `PageResponse<T>`, admite filtros opcionales `tipo` y
`leida`, y limita el tamaño de página a 100. Todas las rutas requieren
`ROLE_PROPIETARIO`; no se modificaron JWT, `SecurityConfig`, Roles, Menús ni
Procesos.

## Tipos

`CUOTA_PROXIMA_VENCER`, `CUOTA_VENCIDA`, `COMPROBANTE_RECIBIDO`,
`PAGO_CONFIRMADO` y `PAGO_RECHAZADO`.

## Archivos

### Creados

- `src/main/resources/db/migration/V13__create_notificaciones_table.sql`.
- Módulo `src/main/java/com/orman/backend/notification`.
- Eventos internos en `src/main/java/com/orman/backend/payment/event`.
- Pruebas de mapper, MVC e integración en `src/test/java/com/orman/backend/notification`.
- `docs/postman/notification.md`.

### Modificados

- `payment` para publicar eventos internos después de confirmar, rechazar o
  registrar comprobantes.
- `contract/CuotaRepository` para seleccionar cuotas candidatas del scheduler.
- Clase principal y configuración YAML para activar y configurar el scheduler.
- Pruebas preexistentes que enumeran Flyway, para reconocer V13.
- Plan, changelog, arquitectura y documentación de base de datos.

## Validación

- Pruebas específicas de mapper, MVC e integración PostgreSQL: **8 pruebas,
  0 fallos, 0 errores y 0 omitidas**.
- `./mvnw.cmd clean test`: **BUILD SUCCESS**; **290 pruebas**, 0 fallos,
  0 errores y 0 omitidas. PostgreSQL real validó Flyway V1–V13 y Hibernate
  con `ddl-auto=validate`.

## Riesgos y pendientes

- La referencia polimórfica no puede imponer una FK hacia Pago o Cuota; la
  validez se controla por el servicio y los recursos existentes son
  restrictivos.
- El único destinatario actual es el propietario. Notificaciones dirigidas a
  inquilinos, canales externos, marcado como no leída, archivado y retención
  quedan fuera de esta etapa.
