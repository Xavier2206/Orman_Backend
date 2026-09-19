# ETAPA 4.2 — Contratos y Cuotas

## Estado

`COMPLETADA`, corregida el 2026-09-15 conforme al modelo doméstico de ORMAN.

## Diseño vigente

- La Unidad conserva únicamente estado operativo binario; disponibilidad,
  ocupación actual y programación futura se derivan de contratos.
- Los estados contractuales son `PROGRAMADO`, `VIGENTE`, `FINALIZADO` y
  `RESCINDIDO`.
- La disponibilidad informativa de una Unidad reutiliza esos mismos estados:
  `PROGRAMADO` y `VIGENTE` bloquean la creación, mientras que `FINALIZADO` y
  `RESCINDIDO` no la bloquean. Esta ampliación solo agrega el campo calculado
  `disponibleParaContrato` a la respuesta de Unidad; no modifica la creación
  de Contratos ni sus validaciones.
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
| Archivos | `POST/GET /api/v1/contratos/{codcon}/archivos`; `GET .../{codarc}/download`; `DELETE .../{codarc}` |
| Cuotas | `GET /api/v1/contratos/{codcon}/cuotas` |

Las creaciones devuelven `201 Created` y `Location`; los errores usan
`ProblemDetail`. La autorización de propietaria y la arquitectura modular se
mantienen.

## Ampliación posterior: documentos PDF

El 2026-09-16 se amplió el recurso existente `ContratoArchivo` para recibir,
listar, descargar y eliminar documentos PDF privados. La V18 incorpora
metadatos y referencias relativas de almacenamiento sin modificar migraciones
anteriores ni guardar bytes en PostgreSQL; los registros legados que solo
contienen URL permanecen identificables como no almacenados internamente.

Los binarios se guardan en el almacenamiento local privado compartido por la
aplicación, bajo `storage/contratos/{codcon}/`, fuera de recursos públicos.
La carga valida extensión, MIME declarado, firma `%PDF-`, contenido parseable,
una página como mínimo y límite recibido configurable de 20 MiB. El objetivo
configurable de optimización es 10 MiB, no un límite de rechazo. PDFBox realiza
una reserialización de mejor esfuerzo y recompresión sin pérdida de imágenes
no enmascaradas; el resultado se utiliza solo si reduce el tamaño y conserva
el texto extraído. Los documentos con diccionarios de firma se conservan sin
reescritura para no invalidar sus firmas. La compresión no garantiza llegar al
objetivo de 10 MiB para cualquier PDF.

La descarga y la eliminación verifican la titularidad del contrato y que el
archivo solicitado pertenezca a ese contrato. `GET /api/v1/contratos` no carga
ni serializa archivos. Los endpoints específicos y variables de configuración
quedan descritos en `docs/postman/contract.md`.

## Persistencia y validación

V16 transforma datos de desarrollo, incorpora `moneda`, sustituye
`fecha_confirmacion` por `fecha_registro`, actualiza checks e índices y rellena
cuotas faltantes sin alterar V1–V15. V18 amplía únicamente la persistencia de
archivos de contrato y conserva intactas las migraciones anteriores. Las
pruebas MVC, de servicio, mapper, persistencia y aislamiento de archivos de
Contrato pasaron. Una ejecución global previa reportó errores de
descubrimiento de `@SpringBootConfiguration` que no se reprodujeron al repetir
`clean test` sin cambios en la configuración de pruebas. La ejecución completa
del 2026-09-16 terminó con 386 pruebas, 0 fallos, 0 errores y 0 omitidas; Spring
encontró `OrmanBackendApplication` en los paquetes afectados.
