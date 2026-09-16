# ETAPA 4.2 — Contratos y Cuotas

## Estado

`COMPLETADA`, corregida el 2026-09-15 conforme al modelo doméstico de ORMAN.

## Diseño vigente

- La Unidad conserva únicamente estado operativo binario; disponibilidad,
  ocupación actual y programación futura se derivan de contratos.
- Los estados contractuales son `PROGRAMADO`, `VIGENTE`, `FINALIZADO` y
  `RESCINDIDO`.
- Registrar un contrato es una decisión efectiva: se crea `PROGRAMADO` si su
  inicio aún no llegó y `VIGENTE` en caso contrario, junto con todas sus cuotas.
- Un scheduler transaccional e idempotente ejecuta `PROGRAMADO -> VIGENTE`.
- Los intervalos se comparan como `[fechaInicio, fechaFin)`. La creación se
  serializa bloqueando la Unidad y rechaza solapamientos entre `PROGRAMADO` y
  `VIGENTE`; los intervalos contiguos están permitidos.
- No existe proceso especial de renovación. Cada continuación es un contrato
  nuevo. `codcon_origen` se conserva solo como dato histórico legado y ya no
  participa en entidad, DTO, servicios ni endpoints.
- La moneda es exclusivamente `BOB`; el monto mensual es mayor a cero y tiene
  máximo dos decimales.

## Cuotas y terminación

Se genera una cuota mensual por cada periodo del intervalo, sin prorrateo. Los
estados son `PENDIENTE`, `PARCIAL`, `PAGADA` y `ANULADA`.

La finalización normal exige contrato `VIGENTE`, fecha actual igual o posterior
a `fechaFin`, ausencia de pagos en revisión y ausencia de cuotas pendientes o
parciales.

La rescisión exige un periodo mensual dentro del contrato, no futuro, todas
las cuotas pagadas hasta ese periodo inclusive y ningún pago pendiente de
revisión. Todas las cuotas posteriores pasan a `ANULADA` sin eliminarse.

## API vigente

| Recurso | Rutas |
|---|---|
| Contratos | `POST/GET /api/v1/unidades/{coduni}/contratos`; `GET /api/v1/contratos`; `GET /api/v1/contratos/resumen`; `GET /api/v1/contratos/{codcon}`; `PATCH .../finalizar`; `PATCH .../rescindir` |
| Archivos | `POST/GET /api/v1/contratos/{codcon}/archivos` |
| Cuotas | `GET /api/v1/contratos/{codcon}/cuotas` |

Las creaciones devuelven `201 Created` y `Location`; los errores usan
`ProblemDetail`. La autorización de propietaria y la arquitectura modular se
mantienen.

## Persistencia y validación

V16 transforma datos de desarrollo, incorpora `moneda`, sustituye
`fecha_confirmacion` por `fecha_registro`, actualiza checks e índices y rellena
cuotas faltantes sin alterar V1–V15. La suite global del cierre ejecutó 367
pruebas, sin fallos, errores ni omisiones, contra PostgreSQL real.
