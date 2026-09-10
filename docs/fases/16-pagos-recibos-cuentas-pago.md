# ETAPA 4.3 — Pagos, comprobantes, recibos y cuentas de pago

## Estado

`COMPLETADA` el 2026-09-10. La etapa implementa exclusivamente Pagos,
PagoComprobantes, Recibos y CuentasPago. No incorpora notificaciones,
aplicación móvil, usuarios inquilinos, pasarela/API bancaria ni conciliación
automática.

## Diseño e implementación

- El módulo reside en `com.orman.backend.payment`, con `controller`,
  `dto.request`, `dto.response`, `entity`, `mapper`, `repository`, `service` y
  `service.impl`.
- `PagoEntity` referencia una `CuotaEntity` y, opcionalmente, una
  `CuentaPagoEntity`. `PagoComprobanteEntity` y `ReciboEntity` referencian al
  pago, y `CuentaPagoEntity` referencia a la Persona propietaria. Todas las
  relaciones de hijos son `@ManyToOne(fetch = FetchType.LAZY)`, sin colecciones
  inversas ni cascadas; la unicidad de `recibos.codpag` garantiza un recibo por
  pago.
- Flyway V12 crea `cuentas_pago`, `pagos`, `pago_comprobantes` y `recibos`, con
  FKs restrictivas, importes `NUMERIC(14,2)`, estados controlados, claves de
  idempotencia, un recibo único por pago y orden único de comprobantes.

## Reglas de negocio

- Todo pago nuevo inicia `PENDIENTE_REVISION`; el servidor asigna origen
  `MANUAL` y fecha de registro. El cliente no controla el estado ni la
  propietaria.
- Los métodos admitidos son `EFECTIVO`, `TRANSFERENCIA` y `QR`. Efectivo no
  usa cuenta y su comprobante es opcional. Transferencia y QR requieren una
  cuenta propia activa y, antes de confirmar, al menos un comprobante.
- La confirmación bloquea la Cuota, suma exclusivamente pagos `CONFIRMADO`,
  evita el sobrepago, confirma el pago, recalcula `PENDIENTE`/`PARCIAL`/
  `PAGADA`, emite un recibo único y confirma todo en una transacción.
- No se persisten saldo ni acumulado. Un pago puede rechazarse o anularse solo
  mientras esté pendiente; ambos cambios guardan fecha de revisión y motivo.
- Las cuentas se administran solo por su propietaria y se desactivan de forma
  idempotente; no existe eliminación de cuentas con trazabilidad histórica.

## Seguridad

Todos los controladores requieren `ROLE_PROPIETARIO`. La autorización de
dominio sigue la cadena:

`Usuario autenticado -> Persona asociada -> Propiedad propia -> Unidad -> Contrato -> Cuota -> Pago`.

Para cuentas, se comprueba `Usuario autenticado -> Persona propietaria ->
CuentaPago`. No se modificaron JWT, `SecurityConfig`, Roles, Menús, Procesos ni
se agregaron roles o permisos.

## API implementada

| Recurso | Rutas |
|---|---|
| Pagos | `POST/GET /api/v1/cuotas/{codcuo}/pagos`; `GET /api/v1/pagos`; `GET /api/v1/pagos/{codpag}`; `PATCH .../confirmar`, `.../rechazar`, `.../anular` |
| Comprobantes | `POST/GET /api/v1/pagos/{codpag}/comprobantes`; `DELETE /api/v1/pagos/{codpag}/comprobantes/{id}` |
| Recibos | `GET /api/v1/pagos/{codpag}/recibo`; `GET /api/v1/recibos/{codrec}` |
| CuentasPago | `POST/GET /api/v1/cuentas-pago`; `GET/PUT /api/v1/cuentas-pago/{codcta}`; `PATCH .../activar`, `.../desactivar` |

Las creaciones responden `201 Created` con `Location`; las eliminaciones de
comprobantes devuelven `204 No Content`. Los errores usan `ProblemDetail` y los
códigos existentes.

## Validación y pruebas

- Pruebas de mapper, MVC/`ProblemDetail`, persistencia PostgreSQL, servicio,
  autorización por propiedad, pagos parciales, recibo automático, cuentas y
  comprobantes.
- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 282 pruebas, 0 fallos, 0 errores
  y 0 omitidas. Flyway validó V1–V12 e Hibernate validó `ddl-auto=validate`.

## Riesgos y pendientes

- No se implementan notificaciones ni mecanismos de pago externos.
- El origen `MOVIL` queda reservado como dato de dominio para una fase futura;
  esta API crea exclusivamente pagos `MANUAL`.
- No hay conciliación automática ni QR dinámico. Cualquier transición futura
  de pagos debe conservar la suma de confirmados y el bloqueo de Cuota.
