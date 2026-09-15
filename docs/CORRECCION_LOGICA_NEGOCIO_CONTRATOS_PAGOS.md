# Corrección de lógica de negocio de Contratos y Pagos

Fecha: 2026-09-15  
Alcance: corrección controlada de `contract`, `payment`, `notification` y
persistencia/documentación relacionada.

## 1. Estado anterior

El registro contractual creaba un estado previo, exigía una confirmación
posterior, solo impedía un segundo contrato vigente y trataba la renovación
como una operación especial. La finalización no comprobaba fecha ni deuda; la
rescisión no regularizaba cuotas futuras.

Todo pago entraba pendiente de revisión y el origen describía canal técnico.
No se distinguía quién presentaba el pago ni quién lo resolvía. Las
notificaciones de pagos se procesaban sin frontera after-commit y podían
propagar un fallo hacia la transacción financiera.

## 2. Decisiones aplicadas

Se implementó exclusivamente el modelo doméstico aprobado: Unidad con estado
operativo binario, contratos efectivos sin etapa previa, BOB como moneda única,
cuotas mensuales sin prorrateo, dos flujos de pago según actor y notificaciones
secundarias a la operación financiera.

## 3. Nuevos estados de contrato

- `PROGRAMADO`: registrado con inicio futuro.
- `VIGENTE`: su inicio llegó y la relación continúa.
- `FINALIZADO`: alcanzó su fecha final y no mantiene obligaciones pendientes.
- `RESCINDIDO`: terminación anticipada acordada.

No existe un estado contractual previo a la aceptación.

## 4. Regla de solapamiento

Los periodos se interpretan como `[fechaInicio, fechaFin)`. La creación bloquea
la Unidad y consulta contratos `PROGRAMADO`/`VIGENTE`; dos intervalos que
comparten días se rechazan y dos contratos contiguos se permiten.

## 5. Ciclo contractual

El alta decide entre `PROGRAMADO` y `VIGENTE` usando la fecha de negocio de
`America/La_Paz`. Un scheduler diario, transaccional e idempotente activa los
programados cuyo inicio ya llegó. Los únicos cierres son finalización normal y
rescisión.

## 6. Finalización

Solo aplica a `VIGENTE` cuando `fechaActual >= fechaFin`, no existe pago
`PENDIENTE_REVISION` en el contrato y no queda cuota `PENDIENTE` o `PARCIAL`.

## 7. Rescisión

El periodo efectivo debe ser el primer día de un mes incluido en el intervalo
contractual y no puede ser futuro. Todas las cuotas desde el inicio hasta ese
periodo inclusive deben estar `PAGADA`, y ningún pago del contrato puede estar
pendiente de revisión.

## 8. Cuotas anuladas

Al rescindir, todas las cuotas posteriores pasan a `ANULADA`; no se eliminan.
Una cuota anulada no acepta pagos, no se considera deuda y no es seleccionada
por el scheduler ni por el recordatorio manual.

Las respuestas de cuota agregan `montoConfirmado`, `saldo` y
`montoPendienteRevision`, calculados desde pagos sin persistir acumulados.

## 9. Flujo de pago de propietaria

La propietaria puede registrar `EFECTIVO`, `QR` o `TRANSFERENCIA` que ya
verificó. El pago se confirma en el alta, bloquea la cuota, recalcula el saldo,
actualiza su estado y genera su recibo en la misma transacción. QR y
transferencia requieren cuenta activa; su comprobante es opcional.

## 10. Flujo de pago del inquilino

El inquilino puede presentar exclusivamente `QR` o `TRANSFERENCIA` para una
cuota de su propio contrato. Debe incluir cuenta y comprobante. El pago entra
`PENDIENTE_REVISION`; solo la propietaria puede confirmarlo, rechazarlo o
anularlo.

## 11. Métodos de pago

Se mantienen `EFECTIVO`, `QR` y `TRANSFERENCIA`. Efectivo queda limitado al
registro de la propietaria. ORMAN registra operaciones externas y no procesa
dinero.

## 12. Pagos parciales

La suma financiera considera solo pagos `CONFIRMADO`. Los pendientes no
reservan saldo. Cada confirmación recalcula bajo bloqueo pesimista y deja la
cuota `PARCIAL` o `PAGADA`; nunca permite saldo negativo.

## 13. Recibos

Cada pago confirmado genera exactamente un recibo, incluidos los parciales.
No se genera para pagos pendientes, rechazados o anulados. La respuesta deja
claro pago, cuota, periodo, monto, moneda, método y fechas. Es una constancia
interna y no un documento fiscal.

## 14. Notificaciones

Los eventos de pago se consumen mediante `@TransactionalEventListener` en
fase `AFTER_COMMIT`. El handler contiene y registra fallos de notificación; por
ello no puede revertir el pago, la cuota o el recibo ya confirmados. Los
recordatorios consultan únicamente cuotas `PENDIENTE` y `PARCIAL`.

## 15. Migraciones creadas

- `V16__correct_contract_lifecycle.sql`: transforma contratos existentes,
  agrega BOB, renombra la fecha de registro, actualiza constraints/índice,
  detecta solapamientos preexistentes y completa cuotas mensuales faltantes de
  contratos activos.
- `V17__add_payment_origin_and_actors.sql`: transforma el origen histórico a
  propietaria, incorpora registrador/revisor, FKs, checks e índices. La
  migración aborta si un pago histórico no puede asociarse al Usuario de la
  propietaria.

V1–V15 no fueron modificadas. Ambas migraciones se ejecutaron correctamente
sobre la base PostgreSQL de desarrollo.

## 16. Endpoints modificados

- `POST /api/v1/unidades/{coduni}/contratos` registra contrato efectivo y cuotas.
- `GET /api/v1/contratos` admite los cuatro estados vigentes.
- Se retiraron edición, confirmación y renovación especial de contratos.
- `PATCH /api/v1/contratos/{codcon}/finalizar` y `/rescindir` aplican las nuevas reglas.
- `POST /api/v1/cuotas/{codcuo}/pagos` deduce el actor autenticado; admite
  propietaria e inquilino.
- Consultas y acciones de revisión de pagos permanecen exclusivas de propietaria.
- Las respuestas de contrato, cuota, pago y recibo exponen los nuevos datos de
  moneda, saldo y trazabilidad.

No se modificaron los contratos HTTP de Personas, Propiedades, Fotografías,
Roles, Menús o asignaciones.

## 17. Pruebas agregadas o modificadas

Se cubrieron alta vigente/futura, scheduler, contigüidad/solapamiento,
revalidaciones de Unidad/Propiedad/inquilino/importes, finalización, rescisión,
anulación de cuotas, bloqueo de pagos a cuotas anuladas, pagos directos de
propietaria, pagos presentados por inquilino, comprobante obligatorio,
confirmación/rechazo/anulación, sobrepago, parciales, recibos, resúmenes de
cuota, actores y aislamiento de fallos de notificación.

También se actualizaron pruebas MVC, mappers, persistencia PostgreSQL y las
verificaciones globales de Flyway.

## 18. Decisiones técnicas realizadas

- Se reutilizó el `Clock` y la zona `America/La_Paz` para decisiones de fecha.
- El bloqueo de Unidad serializa altas contractuales concurrentes; el bloqueo
  de Cuota serializa confirmaciones financieras.
- La continuación de un alquiler usa el endpoint normal de creación.
  `codcon_origen` se conserva físicamente solo para no destruir historia de
  desarrollo, pero fue eliminado del modelo JPA y del contrato HTTP.
- La rescisión no admite periodos futuros, evitando cerrar obligaciones antes
  del periodo mensual efectivo.
- No se incorporaron workflow, prorrateo, multimoneda, reservas de saldo,
  pasarela de pagos ni infraestructura distribuida.

## 19. Resultado de compilación y pruebas

Comando final: `./mvnw.cmd clean test`.

- Resultado: `BUILD SUCCESS`.
- Pruebas: 367.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.
- Base: PostgreSQL 17.6 real.
- Flyway: V1–V17 validadas.
- Hibernate: `ddl-auto=validate` satisfactorio.

## 20. Puntos pendientes de decisión

No queda una decisión de dominio bloqueante dentro del alcance autorizado. El
frontend de Contratos/Pagos deberá consumir los nuevos estados, rutas y campos
antes de implementar esas pantallas. La revisión de cambios y las operaciones
Git permanecen a cargo del usuario.

## Archivos

### Creados

- Migraciones V16 y V17.
- `ContratoActivacionScheduler`.
- `OrigenRegistroPago`.
- Prueba del handler de notificaciones after-commit.
- Este informe.

### Eliminados

- `ContratoRenovacionRequest`.
- `OrigenPago` basado en canal técnico.

### Modificados

- Código, DTO, mappers, repositorios, servicios, controladores y pruebas de
  `contract` y `payment`.
- Handler y pruebas de `notification`.
- Consultas/resumen y protección de desactivación relacionadas en `property`.
- Configuración de scheduler, documentación Postman, documentos de fases,
  plan, changelog, arquitectura y modelo de datos.
