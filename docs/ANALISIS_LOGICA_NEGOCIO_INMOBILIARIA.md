# ANÁLISIS DE LÓGICA DE NEGOCIO INMOBILIARIA Y FINANCIERA

Fecha de auditoría: 2026-09-14  
Repositorio auditado: ORMAN-BACKEND  
Alcance: `Propiedad → Unidad → Contrato → Cuota → Pago → Recibo → Notificación`

## 1. Resumen ejecutivo

ORMAN tiene una base funcional sólida para propiedad, unidad, fotografías, generación mensual de cuotas, pagos parciales, confirmación serializada de pagos, recibo único y notificaciones internas idempotentes. No existe pasarela de pagos: `TRANSFERENCIA`, `QR` y `EFECTIVO` representan pagos realizados fuera de ORMAN y registrados para control interno. Esta separación es coherente con el alcance documentado.

La cadena completa todavía **no está lista para cerrar el frontend de Contratos**. El principal problema no es de estilo ni de capas: el estado `VIGENTE` representa a la vez “contrato confirmado” y “ocupación actual”. Por ello, un contrato futuro confirmado ocupa la unidad desde el momento de confirmación, mientras la restricción de un único `VIGENTE` impide confirmar anticipadamente una renovación no solapada. En sentido inverso, finalizar o rescindir libera la unidad inmediatamente, aunque la fecha efectiva sea futura.

Además, `finalizar(...)` y `rescindir(...)` solo cambian el contrato. No clasifican las cuotas futuras, no resuelven pagos pendientes ni coordinan recordatorios. El propio test de integración conserva todas las cuotas `PENDIENTE` tras rescindir. Como consecuencia, una cuota futura de un contrato rescindido sigue cobrable y el scheduler puede seguir generando avisos.

La lógica financiera principal sí evita saldo negativo al confirmar: bloquea la cuota, suma únicamente pagos confirmados y rechaza el pago que excedería el monto. Dos pagos simultáneos pueden quedar pendientes, pero solo uno se confirmará si juntos exceden el saldo. Esta política puede ser válida para pagos externos que no reservan saldo, aunque debe declararse y mostrarse en la operación de revisión.

Resultado global: **REQUIERE CORRECCIONES ANTES DE CONTINUAR**. Se identifican **10 mejoras NECESARIAS**, **7 RECOMENDADAS**, **4 OPCIONALES** y **14 decisiones de negocio pendientes**. Las decisiones que bloquean específicamente Contratos son el modelo temporal, el solapamiento, la renovación, la finalización, la rescisión y el tratamiento del contrato de monto cero.

## 2. Alcance y método

Se recorrió el repositorio completo y se contrastaron directamente código, migraciones y pruebas. Se leyó `AGENTS.md`; no existe `9.agents.md`, otro `agents.md` ni una instrucción adicional aplicable en subdirectorios. También se revisaron `README.md`, `CHANGELOG.md`, `docs/PLAN_GENERAL.md`, documentos de fases 14–17, documentación Postman, ADR, migraciones V10–V15, configuración y pruebas relevantes.

`docs/INFORME_AUDITORIA_BACKEND.md` se utilizó solo como índice de posibles riesgos. Cada conclusión de este documento fue verificada nuevamente contra el estado actual del código.

Este análisis no modifica código, datos, migraciones ni tests. Cuando una política no está definida por código o documentación se marca como **DECISIÓN DE NEGOCIO PENDIENTE**.

### Verificación ejecutada

Se ejecutó `./mvnw.cmd clean test` contra PostgreSQL real 17.6: **BUILD SUCCESS**, **351 pruebas**, 0 fallos, 0 errores y 0 omitidas. Flyway validó las 15 migraciones y el esquema estaba en V15. Se observaron avisos de compilación sobre una API obsoleta en `AuthorizationIntegrationTest`, operaciones unchecked en `AuthServiceImplTest` y carga dinámica del agente Mockito; no invalidan esta auditoría de dominio. Una suite verde confirma el comportamiento programado, pero no cubre las decisiones y escenarios faltantes enumerados en la sección 41.

## 3. Modelo de dominio encontrado

| Agregado/recurso | Relación real | Responsabilidad actual |
|---|---|---|
| Propiedad | Pertenece a una `Persona` propietaria | Agrupa unidades; estado binario activo/inactivo |
| Unidad | Pertenece a una propiedad | Catálogo físico, precio base y estado operativo binario |
| UnidadFoto | Pertenece a una unidad | Galería externa o archivo interno, orden y portada |
| Contrato | Unidad + persona inquilina + posible contrato origen | Periodo mensual, renta, garantía y ciclo contractual |
| ContratoArchivo | Pertenece a contrato | Referencia URL ordenada; no es archivo interno |
| Cuota | Pertenece a un contrato | Obligación mensual y estado de cobro |
| CuentaPago | Pertenece a propietaria | Destino externo para transferencia/QR |
| Pago | Pertenece a cuota y opcionalmente a cuenta | Registro y revisión de un pago externo |
| PagoComprobante | Pertenece a pago | Evidencia mediante URL |
| Recibo | Pertenece de forma única a pago | Marca interna de emisión tras confirmación |
| Notificación | Pertenece a usuario propietario | Aviso interno con referencia lógica a cuota o pago |

No hay colecciones JPA inversas ni cascadas destructivas; las FK son restrictivas. Esto favorece la conservación histórica. No hay eliminación física de unidades, contratos, cuotas, pagos, cuentas ni recibos a través de la API.

## 4. Flujo general del negocio

### Estado actual

1. El propietario crea una unidad operativa y sus fotografías.
2. Crea un contrato `BORRADOR` para una persona inquilina activa.
3. Al confirmar, el contrato cambia inmediatamente a `VIGENTE` y se generan todas las cuotas mensuales `PENDIENTE` dentro de la misma transacción.
4. Se registran pagos externos como `PENDIENTE_REVISION`.
5. Al confirmar un pago, se bloquea la cuota, se recalcula el total confirmado, se actualiza la cuota a `PARCIAL` o `PAGADA`, se genera un recibo y se crea una notificación.
6. El contrato puede finalizarse o rescindirse, pero esas operaciones no cambian cuotas, pagos, recibos ni notificaciones.

### Problema

La conexión es completa en el camino feliz, pero no en los estados terminales. El contrato deja de considerarse ocupante sin ajustar sus obligaciones futuras.

### Regla de negocio propuesta

Usar el estado de la cuota como autoridad de cobrabilidad y un intervalo contractual efectivo como autoridad de ocupación. Finalizar/rescindir debe clasificar atómicamente las cuotas afectadas; no debe bloquear automáticamente el cobro de deuda histórica válida.

### Cambio necesario

Resolver las decisiones de las secciones 39 y 42 y después implementar una transición coordinada Contrato–Cuota–Pago–Notificación.

## 5. Unidades

### Estado actual

- `UnidadEntity` pertenece obligatoriamente a `PropiedadEntity`; la API de actualización no permite cambiar `codprop`.
- `estadoOperativo` es `0/1`. Activar y desactivar son acciones separadas, idempotentes y toman bloqueo pesimista de la unidad.
- `UnidadServiceImpl.deactivate(...)` bloquea la desactivación si existe un contrato `VIGENTE`.
- No hay endpoint de eliminación de unidad.
- `UnidadServiceImpl.update(...)` permite cambiar nombre, tipo, descripción, área, dormitorios, baños, piso, ubicación y precio base aunque exista contrato `VIGENTE`; solo impide cambiar el estado operativo mediante `PUT`.
- Crear/activar una unidad y crear/confirmar un contrato no comprueban el estado de la propiedad.
- `UnidadResponse` expone estado operativo, pero no disponibilidad ni ocupación contractual.
- La ocupación agregada de propiedades se calcula como “unidad operativa con contrato `VIGENTE`”, sin considerar fechas.

**Evidencia:** `UnidadServiceImpl.create/update/activate/deactivate`, `UnidadRepository.countByPropiedadesOwned(...)`, `UnidadResponse`, `V11__create_contratos_archivos_cuotas_tables.sql`.

### Evaluación de operaciones con contrato vigente

| Operación | Comportamiento actual | Evaluación |
|---|---|---|
| Editar datos | Permitido si conserva `estadoOperativo` | Útil para correcciones; puede alterar la lectura histórica de datos físicos |
| Cambiar precio base | Permitido | Correcto: no cambia `ContratoEntity.montoMensual` ni cuotas existentes |
| Cambiar propiedad | No implementado | Correcto para preservar pertenencia e histórico |
| Desactivar | Prohibido mientras haya `VIGENTE` | Regla correcta bajo la semántica actual, pero depende del problema temporal de `VIGENTE` |
| Eliminar | NO IMPLEMENTADO | Correcto por conservación referencial |
| Recibir otro contrato | Puede recibir borradores; solo un `VIGENTE` | Insuficiente para reservas futuras y solapamiento temporal |

### Problema

El modelo sí distingue operación (`estadoOperativo`) de contrato, pero no ofrece una disponibilidad contractual explícita. La implementación infiere ocupación del estado `VIGENTE`, que no es sensible a la fecha. Tampoco está definido si una propiedad inactiva es solo un registro oculto o si deja de ser administrable.

### Regla de negocio propuesta

- Mantener separados `operativa/no operativa` y `disponible/reservada/ocupada`.
- Calcular disponibilidad con intervalos confirmados efectivos, no con un único flag de unidad.
- Permitir corregir una unidad ocupada, pero definir qué atributos son históricos. El precio base puede cambiar sin afectar contratos; para datos físicos sensibles basta inicialmente con advertencia/auditoría, no con bloquear todo `PUT`.

### Cambio necesario

Exponer disponibilidad derivada y ajustar las comprobaciones al nuevo modelo temporal. La edición histórica es una mejora recomendada, no un bloqueo inmediato.

### Unidad durante el ciclo contractual

| Evento | Comportamiento actual | Comportamiento coherente propuesto |
|---|---|---|
| Confirmar contrato | La unidad se considera ocupada inmediatamente porque el contrato queda `VIGENTE` | Si inicia en el futuro, reservada/programada; ocupada solo desde `fechaInicio` |
| Llegar a `fechaInicio` | No ocurre transición | Pasar a vigente/ocupada o derivarlo por fecha |
| Llegar a `fechaFin` | No ocurre nada automáticamente | Dejar de estar ocupada y finalizar normalmente |
| Finalizar manualmente | Se considera disponible de inmediato | Solo para fin normal efectivo; la deuda no debe mantenerla ocupada |
| Rescindir | Se considera disponible de inmediato | Disponible en `fechaRescision`, no en la fecha de registro si esta es futura |
| Renovar | El borrador no reserva; confirmado no es posible mientras exista otro `VIGENTE` | Mantener ocupación continua y reservar el intervalo futuro no solapado |

## 6. Fotografías de unidades

### Estado actual

- Se admite URL externa o archivo interno, nunca ambos, mediante la restricción `ck_unidad_fotos_fuente` de V15.
- Los archivos internos aceptan JPEG/PNG real, se normalizan a JPEG y se almacenan con referencia privada.
- Título, ambiente y orden son editables. El orden es único por unidad.
- Existe como máximo una portada por unidad mediante índice único parcial. `setPortada(...)` bloquea la unidad, limpia la anterior y asigna la nueva.
- Las fotos nuevas no son portada automáticamente. Al borrar la portada, la unidad queda sin portada; no se elige reemplazo.
- Se puede agregar/modificar una foto de una unidad inactiva.
- Reemplazo y borrado coordinan BD/filesystem: el archivo anterior se elimina después del commit y el nuevo se limpia en rollback. Un fallo de borrado posterior puede dejar un archivo huérfano, pero no una referencia BD rota.
- No existe versión ni conservación histórica de fotografías eliminadas.

**Evidencia:** `UnidadFotoServiceImpl.createInternal/update/replaceArchivo/setPortada/delete`, `UnidadFotoRepository`, migraciones V10 y V15, `UnidadFotoServiceImplTest`, `UnidadFotoArchivoIntegrationTest`.

### Distinción de hallazgos

| Tipo | Hallazgo | Evaluación |
|---|---|---|
| Lógica de negocio | Puede no existir portada | Válido: la portada es opcional; el frontend debe soportarlo |
| Lógica de negocio | Fotos permitidas en unidad inactiva | Coherente para mantenimiento/preparación del catálogo |
| Lógica de negocio | Sin histórico de galería | No es necesario para la cadena contractual actual |
| Técnico | Puede quedar archivo huérfano si falla el borrado poscommit | No corrompe el estado de negocio; requiere limpieza operativa futura |

### Conclusión

**SIN CAMBIOS RECOMENDADOS** para la lógica de negocio principal. La selección automática de nueva portada y el histórico son opcionales.

## 7. Contratos

### Estado actual

Los estados reales son `BORRADOR`, `VIGENTE`, `FINALIZADO` y `RESCINDIDO`. El intervalo es mensual y semiabierto: `[fechaInicio, fechaFin)`, con ambas fechas en el primer día del mes.

Precondiciones observadas:

| Regla | Crear borrador | Actualizar borrador | Confirmar |
|---|---:|---:|---:|
| Unidad existente y propia | Sí | Por contrato propio | Sí, con bloqueo |
| Unidad operativa | Sí | No se revalida | Sí |
| Propiedad activa | No | No | No |
| Inquilino existente | Sí | Sí | No se revalida |
| Inquilino activo | Sí | Sí | No se revalida |
| Fechas válidas/primer día | Sí | Sí | Confía en borrador/BD |
| Monto/garantía no negativos | Bean Validation + BD | Bean Validation + BD | Confía en borrador/BD |
| Moneda | NO IMPLEMENTADO | NO IMPLEMENTADO | NO IMPLEMENTADO |
| Solapamiento temporal | No | No | No |
| Otro `VIGENTE` en unidad | No | No | Sí |

**Evidencia:** `ContratoServiceImpl.createDraft/updateDraft/confirm`, `ContratoRequest`, `ContratoRepository`, constraints de V11.

### Problema

Un inquilino puede quedar inactivo entre borrador y confirmación y el contrato se confirma igual. Una propiedad inactiva tampoco impide la operación. Más importante: “un `VIGENTE`” no equivale a “ningún intervalo ocupado incompatible”.

### Regla de negocio propuesta

La confirmación es el punto de compromiso y debe revalidar unidad, propiedad e inquilino según las semánticas que se aprueben. Los borradores no deben reservar; los contratos confirmados sí deben reservar su intervalo efectivo.

### Cambio necesario

Implementar N-01 y N-05 de la sección 35 antes de cerrar el flujo de confirmación en frontend.

## 8. Máquina de estados de Contratos

### Máquina real

```text
crear ──> BORRADOR ──confirmar──> VIGENTE ──finalizar──> FINALIZADO
                                  └─rescindir──────────> RESCINDIDO

VIGENTE / FINALIZADO / RESCINDIDO ──renovar──> nuevo BORRADOR relacionado
```

| Estado actual | Acción | Nuevo estado/resultado | Validaciones actuales | Efectos |
|---|---|---|---|---|
| — | Crear | BORRADOR | Propiedad efectiva, unidad operativa, inquilino activo, fechas | Ninguno financiero |
| BORRADOR | Editar | BORRADOR | Estado, fechas, inquilino activo | Sustituye datos del borrador |
| BORRADOR | Confirmar | VIGENTE | Unidad operativa y sin otro `VIGENTE` | Genera todas las cuotas |
| VIGENTE | Finalizar | FINALIZADO | Solo estado | Ningún otro módulo |
| VIGENTE | Rescindir | RESCINDIDO | Fecha dentro del intervalo, motivo | Ningún otro módulo |
| VIGENTE/FINALIZADO/RESCINDIDO | Renovar | Nuevo BORRADOR | Origen no borrador; fechas del nuevo contrato | Enlaza `codconOrigen`; origen no cambia |
| Estado terminal | Reabrir | NO IMPLEMENTADO | — | — |

No hay transición automática por fecha. No existe `PROGRAMADO`, `CONFIRMADO` o fecha de finalización efectiva.

## 9. Solapamiento temporal

### Estado actual

`ContratoRepository.existsByUnidadCoduniAndEstado(..., VIGENTE)` y el índice parcial `uk_contratos_coduni_vigente` garantizan un único `VIGENTE` por unidad. No existe consulta de solapamiento.

Esto produce cuatro resultados:

1. Dos borradores pueden solaparse: aceptable mientras no sean reservas.
2. Un contrato futuro confirmado se vuelve `VIGENTE` y ocupa la unidad hoy.
3. Una renovación futura no solapada no puede confirmarse mientras el contrato actual siga `VIGENTE`.
4. Si el contrato actual se finaliza anticipadamente, puede confirmarse otro cuyas fechas sí se solapen con el intervalo nominal anterior.

### Regla propuesta

La restricción coherente no es solo “un `VIGENTE`”; debe ser “ningún solapamiento entre intervalos que reservan ocupación”. Los borradores no bloquean. Para rescindidos, el extremo efectivo debería ser `fechaRescision`; para contratos normales, `fechaFin`.

Recomendación: introducir una representación inequívoca de contrato futuro confirmado (`PROGRAMADO`) o separar “confirmado” de “vigente por fecha”. La disponibilidad se deriva de contratos confirmados cuyo intervalo contiene la fecha consultada.

## 10. Renovaciones

### Estado actual

`ContratoServiceImpl.renew(...)` crea un nuevo `BORRADOR`, copia unidad e inquilino, guarda `codconOrigen` y acepta nueva renta, garantía y periodo. Puede renovarse un origen `VIGENTE`, `FINALIZADO` o `RESCINDIDO`; solo se rechaza `BORRADOR`. No exige continuidad, proximidad al fin, no solapamiento, unidad/inquilino activos ni unicidad de renovación. No altera el origen y no genera cuotas hasta confirmar el nuevo borrador.

### Problema

Actualmente es “crear otro contrato relacionado”, no una renovación completa. La relación histórica es correcta, pero no garantiza continuidad ni permite preconfirmar de forma coherente por el límite de un `VIGENTE`.

### Regla propuesta

Si ORMAN usa “renovación” en sentido estricto, el nuevo inicio debería coincidir con el fin efectivo del origen y el origen debería ser vigente/próximo a finalizar o finalizado. Una relación con hueco debería registrarse como nuevo contrato, no renovación. Esta política requiere aprobación.

### Cambio necesario

Tras D-03, validar elegibilidad/continuidad y aplicar la comprobación temporal al confirmar. Mantener la creación de cuotas en la confirmación del nuevo contrato es correcto.

## 11. Finalización

### Estado actual

`ContratoServiceImpl.finish(...)` permite `VIGENTE → FINALIZADO` en cualquier momento. No compara la fecha actual con `fechaFin`, no registra fecha real de finalización y no revisa cuotas, pagos, recibos o notificaciones. La unidad deja de contarse ocupada inmediatamente porque las consultas solo buscan `VIGENTE`.

Las cuotas `PENDIENTE/PARCIAL` permanecen cobrables y reciben recordatorios; los pagos pendientes pueden confirmarse. Esto podría ser correcto para deuda histórica, pero también afecta cuotas futuras si la finalización se hizo antes de tiempo.

### Regla propuesta

- Finalización normal: ocurre al alcanzar `fechaFin`; libera ocupación sin extinguir deuda previa.
- Rescisión: termina antes de `fechaFin`, registra fecha/motivo y aplica la política de cuotas afectadas.
- No usar “finalizar” como atajo de liberación anticipada.

### Cambio necesario

Registrar/derivar la fecha efectiva y automatizar o restringir la transición normal. No es necesario exigir saldo cero para finalizar: una relación puede terminar conservando deuda.

## 12. Rescisión

### Estado actual comprobado

`ContratoServiceImpl.rescind(...)` acepta cualquier fecha dentro de `[inicio, fin)`, incluso futura o a mitad de mes, cambia inmediatamente a `RESCINDIDO` y guarda motivo. `ContractModuleIntegrationTest.managesDraftConfirmationQuotasRenewalAndRescissionForTheOwner()` afirma expresamente que todas las cuotas siguen `PENDIENTE`.

| Elemento | Resultado real tras rescindir |
|---|---|
| Cuotas anteriores pagadas | Se conservan `PAGADA` |
| Cuotas anteriores parciales | Se conservan `PARCIAL` |
| Cuotas anteriores vencidas | Conservan `PENDIENTE/PARCIAL`; la deuda sigue |
| Cuota del periodo de rescisión | No cambia; no hay prorrateo |
| Cuotas futuras | Siguen `PENDIENTE` y cobrables |
| Pagos pendientes | Siguen pendientes; pueden confirmarse si hay saldo |
| Pagos confirmados | Se conservan |
| Recibos | Se conservan |
| Notificaciones existentes | Se conservan |
| Recordatorios futuros | Continúan para toda cuota pendiente/parcial |
| Unidad | Se considera libre inmediatamente, incluso si la fecha de rescisión es futura |

**Evidencia:** `ContratoServiceImpl.rescind`, `PagoServiceImpl.validateCuotaCanReceivePayment`, `CuotaRepository.findAllPendingOrPartialDueOnOrBefore`, `CuotaNotificacionScheduler.generateFor`, prueba de integración contractual.

### Problema

Es una inconsistencia contractual-financiera alta: ORMAN libera la unidad, permite un nuevo `VIGENTE` y al mismo tiempo conserva como cobrables todas las cuotas futuras del contrato anterior.

### Regla propuesta

Conservar siempre pagos confirmados, recibos y cuotas históricas. Marcar como `ANULADA` las cuotas posteriores al fin efectivo que no representen deuda. El periodo de rescisión y pagos pendientes dependen de D-06/D-07. La transición debe ser atómica.

### Cambio necesario

Implementar un caso de uso de rescisión coordinado, no solo un cambio de enum. No eliminar cuotas: `ANULADA` ya existe y preserva trazabilidad.

## 13. Archivos de contratos

### Estado actual

`ContratoArchivoServiceImpl.create(...)` y `listByContrato(...)` permiten agregar/listar URL y metadatos en cualquier estado. No hay actualizar ni eliminar. El orden es único por contrato. El archivo no se almacena internamente y no tiene tipo semántico, hash, versión, obligatoriedad ni fecha/actor de carga.

### Evaluación

- La ausencia de borrar/modificar protege accidentalmente el histórico desde la API.
- Permitir anexar después de finalizar/rescindir puede ser útil para actas de cierre, pero el modelo no diferencia un contrato firmado de un anexo.
- Una URL externa puede cambiar o desaparecer fuera de ORMAN; por tanto no constituye evidencia inmutable por sí sola.

### Regla propuesta

No bloquear todos los anexos terminales. Definir tipos de documento y distinguir documento contractual esencial de anexos posteriores. Si se requiere evidencia, almacenar copia interna o huella verificable. Es una mejora recomendada, no una razón por sí sola para impedir el formulario básico de contrato.

## 14. Cuotas

### Estado actual

Una cuota pertenece a un único contrato. Tiene periodo, vencimiento, monto y estado. V11 garantiza una cuota por `(codcon, periodo)`, periodo en primer día, vencimiento igual al periodo y monto no negativo. No existe endpoint para crear, editar o eliminar cuotas manualmente.

Las cuotas se generan únicamente al confirmar el contrato y se conservan después de finalizar/rescindir.

## 15. Máquina de estados de Cuotas

```text
confirmar contrato ──> PENDIENTE
PENDIENTE ──confirmar pago menor al saldo total──> PARCIAL
PENDIENTE ──confirmar pago exacto───────────────> PAGADA
PARCIAL    ──confirmar otro pago menor──────────> PARCIAL
PARCIAL    ──confirmar pago por saldo───────────> PAGADA
ANULADA: existe en enum/BD, pero no hay transición implementada
```

| Estado | Admite crear pago | Admite confirmar pendiente | Retroceso | Observación |
|---|---:|---:|---:|---|
| PENDIENTE | Sí | Sí | No | Puede tener uno o varios pagos pendientes |
| PARCIAL | Sí | Sí | No | Saldo usa solo pagos confirmados |
| PAGADA | No | No | No | Invariante protegida por servicio |
| ANULADA | No | No | No | Estado actualmente inalcanzable desde API |

No existe estado `VENCIDA`; el vencimiento se deriva comparando fecha. Esto es suficiente y evita duplicar estado temporal, siempre que las cuotas canceladas se marquen `ANULADA`.

## 16. Generación de cuotas

`CuotaServiceImpl.generatePending(...)` recorre desde `fechaInicio`, suma meses y crea mientras el periodo sea anterior a `fechaFin`. La aritmética es correcta para intervalos completos:

| Contrato | Cuotas reales/esperadas | Periodos |
|---|---:|---|
| 1 mes: 2026-09-01 a 2026-10-01 | 1 | septiembre |
| 6 meses: 2026-09-01 a 2027-03-01 | 6 | sep–feb |
| 12 meses: 2026-09-01 a 2027-09-01 | 12 | sep–ago |
| Cambio de año | Correcto | `LocalDate.plusMonths(1)` |
| Febrero/año bisiesto | Correcto | Siempre día 1 |
| Inicio o fin a mitad de mes | Rechazado | Servicio y constraints V11 |
| Renovación | Solo al confirmar el nuevo borrador | No altera cuotas del origen |

ORMAN trabaja deliberadamente por **periodos mensuales completos**; no hay prorrateo. El vencimiento siempre es el primer día del periodo. Prorrateo y día de vencimiento son decisiones pendientes, no defectos matemáticos.

Un contrato con monto `0.00` genera cuotas `PENDIENTE` de cero. Como `PagoRequest` exige al menos `0.01`, esas cuotas nunca pueden llegar a `PAGADA` y pueden generar recordatorios. Este caso sí debe resolverse.

## 17. Cálculo de saldos

La fórmula real es:

```text
saldo = cuota.monto - SUM(pagos.monto WHERE estado = CONFIRMADO)
```

En creación se comprueba el total confirmado sin bloqueo. En confirmación se bloquea la cuota y se vuelve a calcular antes de modificar estados.

Ejemplo comprobado por `PaymentModuleIntegrationTest`:

```text
Cuota: Bs 1.500,00
Pago confirmado 1: Bs 1.000,00 → saldo Bs 500,00 → PARCIAL
Pago confirmado 2: Bs   500,00 → saldo Bs   0,00 → PAGADA
```

| Caso | Comportamiento actual |
|---|---|
| Pago parcial | Permitido; cuota pasa a `PARCIAL` |
| Varios pagos | Permitidos |
| Pago exacto | Cuota pasa a `PAGADA` |
| Pago superior al saldo al confirmar | Rechazado; transacción revierte |
| Pago cero/negativo | Rechazado por DTO y BD |
| Saldo negativo | Evitado en confirmación bajo bloqueo de cuota |
| Más de dos decimales | DTO no lo rechaza; PostgreSQL `NUMERIC(14,2)` aplica escala |

`BigDecimal.compareTo` está bien utilizado. Falta `@Digits`/normalización explícita para que el contrato HTTP tenga exactamente dos decimales y no dependa del redondeo de PostgreSQL.

## 18. Cuentas de pago

### Estado actual

La cuenta pertenece a una propietaria y contiene banco, número, titular, URL QR, instrucciones, orden y estado `0/1`. Puede crearse, consultarse, modificarse completamente, activarse y desactivarse; no se elimina. Transferencia y QR requieren una cuenta propia activa tanto al registrar como al confirmar; efectivo exige cuenta nula.

### Problemas

1. Si se registra un pago real y después se desactiva la cuenta, `PagoServiceImpl.validateCuentaForConfirmation(...)` impide confirmarlo, aunque el dinero ya haya llegado externamente.
2. Banco, número, titular y QR pueden modificarse después de recibir pagos. Los pagos históricos conservan la FK, pero su interpretación futura usa datos actuales.

### Regla propuesta

La activación debe determinar si la cuenta puede seleccionarse para **nuevos registros**, no invalidar pagos ya presentados. Para histórico, la solución mínima es impedir cambios de identidad financiera una vez referenciada y crear/desactivar cuentas; un snapshot por pago solo se justifica si se necesita mostrar exactamente los datos históricos sin depender de la cuenta.

## 19. Pagos

### Estado actual

- `MANUAL` y `MOVIL` son orígenes posibles, pero el mapper actual crea siempre `MANUAL`.
- Métodos: `EFECTIVO`, `TRANSFERENCIA`, `QR`.
- No se procesa dinero. ORMAN registra una declaración/evidencia de un pago externo y el propietario la revisa.
- Todos los endpoints son para `ROLE_PROPIETARIO`; no hay flujo de inquilino.
- No se guarda quién registró ni quién confirmó/rechazó/anuló el pago.

Esta semántica externa es correcta. `MOVIL` está modelado para una fase futura, no implementado.

## 20. Máquina de estados de Pagos

```text
crear ──> PENDIENTE_REVISION ──confirmar──> CONFIRMADO
                         ├──────rechazar──> RECHAZADO
                         └────────anular──> ANULADO
```

Todos los estados de salida son terminales. `RECHAZAR` expresa que la evidencia/registro no fue aceptado; `ANULAR` expresa cancelación administrativa de un registro aún pendiente. **No existe anulación/reversión de un pago confirmado.** Por tanto, el código no necesita recalcular cuota ni invalidar recibo al anular: esa acción solo es válida antes de confirmar.

Este diseño es financieramente prudente. Si el proyecto necesita corregir un pago confirmado, debe definirse una operación explícita de reversión con trazabilidad; no conviene reutilizar `ANULADO` silenciosamente.

## 21. Pagos parciales

El flujo está correctamente implementado:

- Una cuota admite múltiples pagos.
- Solo los confirmados reducen saldo.
- Un pago pendiente o rechazado no cambia la cuota.
- Después de un parcial puede registrarse y confirmarse otro pago.
- Cada pago confirmado genera su propio recibo; por ello una cuota pagada con dos abonos tiene dos recibos.
- Un pago rechazado deja la cuota igual y permite registrar otro.

**SIN CAMBIOS RECOMENDADOS** para la aritmética y transición parcial. Sí se recomienda que las respuestas de cuota expongan saldo confirmado y monto pendiente de revisión para facilitar la revisión.

## 22. Concurrencia

### Estado actual

Crear pago no bloquea la cuota y los pendientes no reservan saldo. Confirmar sí bloquea la cuota con `PESSIMISTIC_WRITE`.

Caso: cuota Bs 1.000; A = Bs 700; B = Bs 700 simultáneos:

1. A y B pueden crearse como `PENDIENTE_REVISION`.
2. Al confirmar, un revisor obtiene el bloqueo; A puede quedar `CONFIRMADO`, cuota `PARCIAL`, saldo Bs 300.
3. B se procesa después y falla porque 700 supera el saldo. B continúa pendiente y requiere rechazo/anulación.
4. No se produce saldo negativo ni dos confirmaciones incompatibles.

### Alternativas

| Alternativa | Ventaja | Riesgo |
|---|---|---|
| A. Pendientes reservan saldo | Menos solicitudes imposibles | Un comprobante falso/bloqueado inmoviliza el saldo |
| B. Pendientes no reservan (actual) | Refleja que son declaraciones externas aún no verificadas | Pueden acumularse pendientes incompatibles |
| C. No reservar, pero advertir/exponer totales pendientes | Mantiene seguridad y mejora operación | Requiere respuesta/UI adicional |

Recomendación: **C**, que conserva el bloqueo actual en confirmación. No se justifica una reserva estricta para dinero que ORMAN no procesa.

Verificación concurrente real multi-hilo: **NO VERIFICADO**; no existe prueba específica. El comportamiento se deriva del orden de bloqueos y consultas del código.

## 23. Idempotencia

`idempotencyKey` es UUID único global. La preconsulta ofrece error claro y la constraint protege carreras. Repetir exactamente la misma clave crea conflicto `409`; no devuelve el recurso original. Una clave diferente permite otro pago lógico, salvo que una referencia externa no nula se repita en la misma cuenta.

| Caso | Protección actual |
|---|---|
| Mismo request/misma UUID | No duplica; devuelve conflicto |
| Carrera con misma UUID | Constraint evita duplicado |
| Misma UUID con diferente importe/usuario | Conflicto global, sin comparar payload |
| Misma cuota con UUID distinta | Puede crear otro pendiente |
| Reintento tras timeout sin conocer resultado | Cliente recibe conflicto y debe buscar por otros medios |
| Efectivo duplicado con UUID distinta | No detectable automáticamente |

La garantía contra duplicado físico por la misma clave es correcta. Se recomienda semántica idempotente completa: devolver el pago existente si propietario y payload coinciden, y conflicto si no coinciden.

## 24. Comprobantes

Se permiten múltiples comprobantes ordenados por pago. Solo se agregan o eliminan mientras el pago está `PENDIENTE_REVISION`; después de confirmar, rechazar o anular quedan inmutables desde la API. Transferencia y QR requieren al menos uno antes de confirmar; efectivo no lo requiere, pero puede tenerlo.

Esto protege correctamente la evidencia después de la decisión. El riesgo es que solo se guarda URL y metadatos: el contenido remoto puede cambiar o desaparecer. Guardar copia interna/hash es recomendado si ORMAN necesita auditoría documental fuerte; no es obligatorio para un control interno básico.

## 25. Confirmación, rechazo y anulación

### Confirmación real

```text
buscar pago propio
→ bloquear cuota
→ bloquear pago
→ exigir PENDIENTE_REVISION
→ validar cuenta activa y comprobante
→ sumar pagos CONFIRMADO
→ impedir sobrepago
→ marcar pago CONFIRMADO
→ actualizar cuota PARCIAL/PAGADA
→ crear recibo único
→ publicar evento
→ crear notificación síncrona
→ commit
```

Pago, cuota y recibo quedan atómicos. Sin embargo, `@EventListener` ejecuta el generador de notificación síncronamente dentro de la misma transacción. Si no existe usuario para la propietaria o falla la persistencia de la notificación, se revierte también una confirmación financiera válida.

### Rechazo real

Solo desde pendiente, con motivo obligatorio. No cambia saldo/cuota/comprobantes y genera notificación síncrona. Se puede registrar otro pago.

### Anulación real

Solo desde pendiente, con motivo obligatorio. No cambia cuota y no genera notificación. Un pago confirmado no puede anularse.

### Regla propuesta

La información financiera principal (`Pago + Cuota + Recibo`) no debe fallar por una notificación interna. Publicar después del commit o usar un mecanismo reintentable. La ausencia de reversión de confirmados se conserva hasta que el dueño del producto defina lo contrario.

## 26. Recibos

### Estado actual

Se crea exactamente un recibo por pago confirmado. La BD garantiza unicidad por `codpag`; no hay creación manual, edición ni eliminación. Pagos parciales producen un recibo por abono. Un pago confirmado no puede anularse, por lo que el recibo no queda desalineado mediante la API.

`ReciboEntity` y `ReciboResponse` solo contienen identificador, pago y fecha de emisión. El importe, cuota, periodo, unidad, inquilino, método, cuenta, moneda y emisor se obtendrían de registros relacionados y mutables.

### Conclusión funcional

Actualmente funciona como **constancia interna de que ORMAN confirmó un pago**, no como documento contable, fiscal ni legal autosuficiente. No debe presentarse con ese valor en el frontend.

### Recomendación

Definir el propósito. Para un recibo visible, construir una representación histórica inmutable o una proyección que incluya los datos mínimos aprobados. No hace falta numeración fiscal si ORMAN no emitirá documentos fiscales.

## 27. Notificaciones

### Estado actual

Tipos: cuota próxima, cuota vencida, comprobante recibido, pago confirmado y pago rechazado. El único destinatario es el usuario propietario; no existe usuario inquilino, push, correo, SMS ni WhatsApp. La unicidad `(destinatario, tipo, referenciaTipo, referenciaId)` evita duplicados. Se puede listar, resumir no leídas y marcar como leída de forma idempotente.

No existe notificación para pago anulado ni contrato finalizado/rescindido. Las notificaciones históricas no se eliminan.

El endpoint `/cuotas/{codcuo}/notificar` crea un aviso interno para el propietario; **no envía un recordatorio al inquilino**. El frontend debe nombrarlo de forma que no sugiera comunicación externa.

## 28. Scheduler

Se ejecuta por defecto a las 08:00 en `America/La_Paz`. Selecciona todas las cuotas `PENDIENTE/PARCIAL` con vencimiento hasta mañana, sin filtrar propietario ni estado/fecha contractual. Para vencidas crea una notificación una sola vez; para próxima crea una sola por cuota.

La zona horaria y la idempotencia son correctas. El problema de recordatorios posteriores a rescisión proviene principalmente de que la rescisión no anula cuotas futuras. No se recomienda bloquear recordatorios de toda deuda de contratos terminales: una cuota histórica válida puede seguir vencida después del fin. La cuota debe ser la autoridad.

## 29. Relación contrato–cuota–pago–recibo–notificación

| Acción | Consistencia actual | Brecha |
|---|---|---|
| Confirmar contrato | Contrato y cuotas en una transacción | Estado temporal/solapamiento |
| Registrar pago | Pago pendiente sin cambiar saldo | Pendientes incompatibles no se muestran agregados |
| Confirmar pago | Pago, cuota, recibo y notificación en una transacción | Notificación puede revertir finanzas |
| Rechazar pago | Pago y notificación en una transacción | Mismo acoplamiento accesorio |
| Anular pago pendiente | Solo pago | Coherente; no hay aviso |
| Finalizar contrato | Solo contrato | Sin fecha/evaluación de cuotas futuras |
| Rescindir contrato | Solo contrato | Cuotas futuras cobrables y recordatorios activos |

## 30. Matriz general de estados

| Entidad | Estado actual | Acción | Nuevo estado | Permitido | Efectos secundarios |
|---|---|---|---|---|---|
| Unidad | 0 | Activar | 1 | Sí, propietaria | Ninguno |
| Unidad | 1 | Desactivar | 0 | Solo sin `VIGENTE` | Ninguno |
| Unidad | 0/1 | Repetir acción | Igual | Sí, idempotente | Ninguno |
| Contrato | — | Crear | BORRADOR | Unidad operativa, inquilino activo | Ninguno |
| Contrato | BORRADOR | Confirmar | VIGENTE | Unidad operativa, sin otro `VIGENTE` | Genera cuotas |
| Contrato | VIGENTE | Finalizar | FINALIZADO | Siempre | Ninguno |
| Contrato | VIGENTE | Rescindir | RESCINDIDO | Fecha en intervalo y motivo | Ninguno |
| Contrato | No BORRADOR | Renovar | Origen igual + nuevo BORRADOR | Sí | Relación con origen |
| Cuota | — | Confirmar contrato | PENDIENTE | Automático | — |
| Cuota | PENDIENTE | Confirmar pago parcial | PARCIAL | Sin sobrepago | Recibo + notificación |
| Cuota | PENDIENTE/PARCIAL | Confirmar saldo | PAGADA | Sin sobrepago | Recibo + notificación |
| Cuota | PENDIENTE/PARCIAL | Anular por rescisión | ANULADA | NO IMPLEMENTADO | Debería detener cobro/avisos |
| Pago | — | Registrar | PENDIENTE_REVISION | Cuota cobrable; método/cuenta válidos | Ninguno |
| Pago | PENDIENTE_REVISION | Confirmar | CONFIRMADO | Cuenta/prueba/saldo | Cuota + recibo + notificación |
| Pago | PENDIENTE_REVISION | Rechazar | RECHAZADO | Motivo | Notificación |
| Pago | PENDIENTE_REVISION | Anular | ANULADO | Motivo | Ninguno |
| Cuenta pago | 0 | Activar | 1 | Sí | Habilita nuevos registros |
| Cuenta pago | 1 | Desactivar | 0 | Sí | Bloquea registro y confirmación actuales |
| Notificación | No leída | Leer | Leída | Sí | Fecha de lectura |
| Notificación | Leída | Leer | Leída | Sí, idempotente | Ninguno |

## 31. Matriz de dependencias

Leyenda: `M` modifica, `V` valida/consulta, `G` genera, `—` no participa.

| Acción | Unidad | Contrato | Cuota | Pago | Recibo | Notificación |
|---|---|---|---|---|---|---|
| Desactivar unidad | M | V (`VIGENTE`) | — | — | — | — |
| Confirmar contrato | V/bloquea | M | G | — | — | — |
| Finalizar contrato | — | M | — | — | — | — |
| Rescindir contrato | — | M | — | — | — | — |
| Renovar | V indirecta | V origen/G borrador | — | — | — | — |
| Registrar pago | V ownership | V indirecta | V saldo | G pendiente | — | — |
| Adjuntar comprobante | V indirecta | V indirecta | V indirecta | V estado | — | G síncrona |
| Confirmar pago | V indirecta | V indirecta | V/M bloqueada | M | G | G síncrona |
| Rechazar pago | V indirecta | V indirecta | — | M | — | G síncrona |
| Anular pago pendiente | V indirecta | V indirecta | — | M | — | — |
| Scheduler | V indirecta | No filtra | V | — | — | G |

La matriz muestra la brecha: finalizar/rescindir deberían afectar al menos la clasificación de cuotas y, según decisión, pagos pendientes y generación de avisos.

## 32. Invariantes del negocio

### Garantizadas actualmente

1. Toda unidad pertenece a una única propiedad.
2. Una foto pertenece a una unidad, tiene una sola fuente y existe como máximo una portada por unidad.
3. Un contrato pertenece a una unidad y a una persona inquilina.
4. Las fechas contractuales son primeros de mes y `inicio < fin`.
5. Cada cuota pertenece a un contrato y es única por contrato/periodo.
6. Cada pago pertenece a una cuota y su monto es positivo.
7. `PAGADA` y `ANULADA` no aceptan nuevos pagos por servicio.
8. La suma confirmada no supera la cuota cuando los pagos se confirman por API.
9. Una cuota queda `PAGADA` solo cuando el total confirmado es exactamente su monto.
10. Un pago solo puede salir de pendiente una vez.
11. Un recibo es único por pago y el servicio solo lo crea al confirmar.
12. Un comprobante no se altera desde la API después de resolver el pago.
13. Las notificaciones lógicas no se duplican para destinatario/tipo/referencia.
14. El propietario solo opera recursos propios.

### No garantizadas y requeridas

1. Una unidad no tiene contratos confirmados con intervalos efectivos incompatibles.
2. La ocupación actual coincide con la fecha efectiva del contrato.
3. Confirmar revalida que todas las partes siguen habilitadas.
4. Finalización normal no ocurre antes de `fechaFin`.
5. Un contrato rescindido no conserva cuotas futuras cobrables.
6. Una cuota `ANULADA` no genera recordatorios (el scheduler ya lo cumple si se llega a ese estado).
7. Una fecha futura de rescisión no libera la unidad antes de esa fecha.
8. La notificación no puede abortar una confirmación financiera válida.
9. Desactivar una cuenta no invalida un pago externo ya registrado.
10. Todo importe monetario del contrato HTTP tiene escala máxima de dos decimales.
11. Un contrato de monto cero tiene un cierre coherente de sus cuotas.
12. Se conoce quién registró y quién revisó cada movimiento financiero.

## 33. Escenarios end-to-end

| Escenario | Comportamiento actual | Comportamiento esperado | Diferencia/severidad | Recomendación |
|---|---|---|---|---|
| 1. Alquiler normal | Borrador → `VIGENTE`; 12 cuotas; pagos; recibos; `finalizar` manual en cualquier momento | Contrato futuro programado, vigente por fecha y finalización normal al fin; deuda histórica preservada | **ALTA**: ocupación/fin no sensibles a fecha | Implementar N-01/N-02 |
| 2. Pago parcial | 1.500 → 500 confirmado = `PARCIAL`; +1.000 = `PAGADA`; dos recibos | Igual | **SIN BRECHA** | Conservar diseño |
| 3. Pago rechazado | Pendiente + comprobante → `RECHAZADO`; cuota igual; permite nuevo pago | Igual; conservar evidencia | **SIN BRECHA** | Conservar diseño |
| 4. Rescisión | Solo contrato cambia; todas las cuotas y pagos siguen; unidad libre inmediatamente; avisos continúan | Conservar histórico/deuda aprobada, anular futuro, resolver periodo/pagos pendientes, liberar en fecha efectiva | **ALTA** | Aprobar D-05/D-07 e implementar N-03 |
| 5. Renovación | Crea borrador relacionado; no puede confirmarse mientras origen `VIGENTE`; se puede finalizar origen antes y confirmar | Reserva futura no solapada, continuidad aprobada, cuotas del nuevo contrato al confirmarlo | **ALTA** | Aprobar D-02/D-03 e implementar N-01/N-05 |
| 6. Dos pagos de 700 sobre 1.000 | Ambos pendientes; primero confirma, segundo falla y queda pendiente | No reservar, pero mostrar total pendiente y exigir resolución | **MEDIA**, sin sobrepago | Aplicar R-05; no agregar reserva compleja |
| 7. Cuenta desactivada | Historial conserva FK; datos pueden mutar; pago pendiente no puede confirmarse | No seleccionable para nuevos; pago ya presentado revisable; identidad histórica preservada | **ALTA** | Aprobar D-13 e implementar N-07 |
| 8. Fallo de notificación | Puede revertir pago/cuota/recibo por listener síncrono | Confirmación financiera persiste; notificación reintentable | **ALTA** | Implementar N-08 |

## 34. Inconsistencias encontradas

| ID | Hallazgo | Evidencia | Impacto |
|---|---|---|---|
| I-01 | `VIGENTE` mezcla confirmación y ocupación actual | `ContratoServiceImpl.confirm`; `UnidadRepository`; índice V11 | Reservas/renovaciones y disponibilidad incorrectas |
| I-02 | No hay solapamiento temporal | `ContratoRepository` solo consulta estado | Contratos confirmables tras cierres anticipados con fechas incompatibles |
| I-03 | Finalizar no usa fecha ni downstream | `ContratoServiceImpl.finish` | Libera unidad y deja cuotas futuras activas |
| I-04 | Rescindir solo cambia contrato | `ContratoServiceImpl.rescind`; test contractual | Cobro/avisos futuros incoherentes |
| I-05 | Confirmar no revalida inquilino/propiedad | `ContratoServiceImpl.confirm` | Contrato confirmado con parte deshabilitada según semántica pendiente |
| I-06 | Monto cero deja cuota imposible de pagar | `ContratoRequest`; `CuotaServiceImpl`; `PagoRequest` | Recordatorios/deuda sin transición a pagada |
| I-07 | Cuenta inactiva bloquea confirmar pago ya registrado | `validateCuentaForConfirmation` | No se puede reconocer dinero externo recibido |
| I-08 | Cuenta mutable altera contexto histórico | `CuentaPagoServiceImpl.update`; FK de `PagoEntity` | Lectura histórica ambigua |
| I-09 | Notificación participa en transacción financiera | `@EventListener`; servicios transaccionales | Efecto accesorio aborta operación principal |
| I-10 | Scheduler ignora contrato | `CuotaRepository.findAllPendingOrPartial...` | Visible tras rescisión porque cuotas no se anulan |
| I-11 | No se registran actores financieros | `PagoEntity`, `ReciboEntity` | Trazabilidad insuficiente de revisión |
| I-12 | Recibo no es autosuficiente | `ReciboResponse(codrec,codpag,fechaEmision)` | No sirve como documento histórico visible sin reconstrucción |

## 35. Reglas correctamente implementadas

- Propiedad efectiva aplicada en servicios y controladores de propietario.
- Separación técnica entre estado operativo de unidad y contrato; la brecha es temporal, no un único flag persistido.
- Sin cambio de propiedad ni borrado de unidad con pérdida de histórico.
- Fuente exclusiva, orden y portada única de fotografías.
- Contratos/fechas mensuales y generación determinista `[inicio, fin)`.
- Cuota única por periodo y sin edición manual.
- Pagos externos claramente registrados, no procesados por ORMAN.
- Métodos/cuenta coherentes: efectivo sin cuenta; QR/transferencia con cuenta.
- Comprobante obligatorio para confirmar QR/transferencia.
- Pagos parciales y varios pagos por cuota.
- Bloqueo de cuota y recálculo al confirmar; sin sobrepago confirmado.
- Pago confirmado inmutable y recibo único por pago.
- Rechazo/anulación solo de pendientes y con motivo.
- Evidencias inmutables por API después de la decisión.
- Notificaciones internas idempotentes y zona horaria La Paz.

## 36. Mejoras necesarias

Se contabilizan **10** acciones NECESARIAS:

| ID | Acción necesaria | Razón |
|---|---|---|
| N-01 | Definir e implementar estado temporal/reserva y solapamiento por intervalo | Evitar ocupación y renovaciones incoherentes |
| N-02 | Definir y restringir/automatizar finalización normal con fecha efectiva | Evitar liberar anticipadamente y conservar deuda correctamente |
| N-03 | Implementar rescisión atómica sobre cuotas futuras, periodo actual y pagos pendientes | Evitar cobrar lo cancelado y seguir notificándolo |
| N-04 | Revalidar al confirmar unidad, inquilino y propiedad según la semántica aprobada | La confirmación es el compromiso real |
| N-05 | Completar reglas de renovación: elegibilidad, continuidad y confirmación futura | Hoy solo crea un contrato relacionado |
| N-06 | Resolver monto cero, escala de dos decimales y moneda contractual | Evitar cuotas imposibles y redondeos implícitos |
| N-07 | Permitir revisar un pago ya registrado aunque la cuenta se desactive; preservar identidad histórica | El dinero se movió fuera de ORMAN |
| N-08 | Desacoplar fallos de notificación de pago/cuota/recibo y comprobante | Prioridad financiera sobre efecto secundario |
| N-09 | Registrar actores de alta y revisión de pagos/recibos | Trazabilidad administrativa y financiera |
| N-10 | Alinear scheduler con cuotas anuladas resultantes de terminación/rescisión | Evitar recordatorios contractualmente inválidos |

N-10 no requiere filtrar ciegamente todo contrato terminal: debe consumir el resultado correcto de N-02/N-03.

## 37. Mejoras recomendadas

Se contabilizan **7** acciones RECOMENDADAS:

| ID | Mejora recomendada | Beneficio |
|---|---|---|
| R-01 | Exponer disponibilidad/ocupación contractual derivada en DTO de unidad | Frontend no confundirá operación con disponibilidad |
| R-02 | Definir edición de atributos críticos de unidad ocupada y/o auditoría mínima | Conserva interpretación histórica sin bloquear correcciones |
| R-03 | Tipificar archivos contractuales y proteger contenido esencial con copia/hash | Evidencia documental más clara |
| R-04 | Hacer idempotencia de pago repetible devolviendo el recurso si payload coincide | Reintentos móviles seguros y simples |
| R-05 | Exponer saldo confirmado y total pendiente de revisión | Manejo claro de concurrencia sin reservar saldo |
| R-06 | Validar política de `fechaPago` y registrar tiempos en una zona/UTC coherente | Evita pagos futuros o retroactivos ambiguos |
| R-07 | Enriquecer el recibo según su propósito aprobado, sin atribuir valor fiscal | Documento interno útil e histórico |

## 38. Mejoras opcionales

Se contabilizan **4** mejoras OPCIONALES:

1. Elegir automáticamente otra foto como portada al eliminar la actual.
2. Versionar/conservar fotografías históricas de unidad.
3. Limitar a una sola renovación hija por contrato si el proceso operativo lo requiere.
4. Almacenar comprobantes de pago internamente además de la URL cuando se necesite archivo probatorio fuerte.

## 39. Secciones sin cambios recomendados

- Aritmética de generación mensual para fechas en primer día.
- Fórmula de saldo con pagos confirmados.
- Transición `PENDIENTE/PARCIAL → PAGADA` bajo bloqueo.
- Un recibo por cada pago parcial confirmado.
- Inmutabilidad de pagos confirmados y evidencias después de resolverlos.
- Distinción `RECHAZADO` vs `ANULADO` para registros pendientes.
- Portada única y fuente exclusiva de fotografías.
- Notificaciones internas para propietario como alcance actual documentado.
- No incorporar pasarela de pago: **NO NECESARIA** para el objetivo de ORMAN actual.

## 40. Decisiones de negocio que debe tomar el propietario del proyecto

Se identifican **14 decisiones**. Las recomendaciones no describen comportamiento existente.

### D-01. Significado de propiedad inactiva y unidad no operativa

- **Problema:** el contrato solo valida unidad operativa; propiedad inactiva no interviene.
- **A:** propiedad inactiva es solo ocultamiento administrativo; contratos siguen. Ventaja: simple. Riesgo: nombre “inactiva” ambiguo.
- **B:** propiedad inactiva impide nuevos contratos/unidades, pero no cancela históricos. Ventaja: control coherente. Riesgo: requiere validación cruzada.
- **Recomendación:** B; nunca cancelar contratos automáticamente.

### D-02. Estado de contrato futuro y solapamiento

- **Problema:** `VIGENTE` significa confirmado y ocupado.
- **A:** no confirmar hasta la fecha de inicio. Simple; impide formalizar reservas.
- **B:** agregar `PROGRAMADO` y pasar a `VIGENTE` al inicio. Claro; requiere transición temporal.
- **C:** mantener `VIGENTE` como confirmado y derivar ocupación por fechas. Menos estados; semántica del nombre es confusa.
- **Recomendación:** B y bloqueo de intervalos de `PROGRAMADO/VIGENTE`.

### D-03. Qué es una renovación

- **A:** inicio exactamente igual al fin efectivo del origen y mismo inquilino. Historial claro.
- **B:** permitir huecos y llamarlo renovación. Flexible, pero confunde continuidad.
- **C:** todo es contrato nuevo; enlace origen solo informativo. Simple, pierde semántica operativa.
- **Recomendación:** A; si hay hueco, usar contrato nuevo.

### D-04. Finalización normal con deuda

- **A:** finalizar en fecha aunque exista deuda, conservándola cobrable. Separa ocupación de cobranza.
- **B:** no finalizar hasta saldo cero. Mantiene estado, pero falsea ocupación y bloquea unidad.
- **Recomendación:** A.

### D-05. Deuda previa después de rescisión

- **A:** cuotas vencidas/parciales previas siguen cobrables. Conserva obligaciones devengadas.
- **B:** se perdonan/anulan al rescindir. Requiere decisión explícita por caso.
- **C:** parametrizar por rescisión. Flexible, exige motivo y auditoría.
- **Recomendación:** A por defecto; excepciones explícitas y trazables.

### D-06. Cuota del periodo de rescisión

- **A:** mes completo. Coherente con periodos mensuales actuales.
- **B:** prorrateo diario. Más equitativo, agrega cálculo/redondeo.
- **C:** monto acordado manualmente al rescindir. Flexible, requiere auditoría.
- **Recomendación:** A inicialmente; C solo si el negocio necesita acuerdos excepcionales. El código actual no prorratea.

### D-07. Pagos pendientes al rescindir

- **A:** revisar si corresponden a deuda válida; anular/rechazar los ligados a cuota futura anulada.
- **B:** anular todos automáticamente. Simple, puede ignorar dinero recibido.
- **C:** dejarlos todos pendientes sin señal. Es el comportamiento actual y genera ambigüedad.
- **Recomendación:** A.

### D-08. Moneda, monto cero y precisión

- **A:** moneda única BOB, dos decimales, renta > 0. Simple y coherente con ejemplos.
- **B:** moneda única BOB y permitir renta cero, creando cuotas ya satisfechas/anuladas.
- **C:** multimoneda. Mayor alcance y complejidad no justificada hoy.
- **Recomendación:** A; documentar BOB explícitamente.

### D-09. Día de vencimiento y prorrateo general

- **A:** vencimiento primer día y meses completos, como ahora.
- **B:** día configurable por contrato, sin prorrateo.
- **C:** fechas libres con primer/último mes prorrateados.
- **Recomendación:** A para la primera versión, salvo necesidad comercial confirmada.

### D-10. Garantía

- **Problema:** hoy es solo monto informativo; no genera deuda, pago ni devolución.
- **A:** conservarla informativa. Simple, no controla caja.
- **B:** modelarla como obligación/pago/reintegro separado. Trazabilidad completa, más alcance.
- **Recomendación:** A para Contratos; etiquetar claramente “monto acordado, no cobrado por ORMAN”.

### D-11. Pendientes reservan saldo

- **A:** sí. Evita pendientes incompatibles, pero bloquea por evidencia no verificada.
- **B:** no, como ahora. Requiere resolver excedentes en revisión.
- **C:** no reservan, pero se exponen totales/alertas.
- **Recomendación:** C.

### D-12. Fecha declarada de pago

- **A:** no futura y retroactividad limitada configurable.
- **B:** cualquier fecha con motivo/auditoría.
- **C:** sin validación, como ahora.
- **Recomendación:** A para captura ordinaria; excepción administrativa futura si se necesita.

### D-13. Histórico de cuenta de pago

- **A:** bloquear banco/número/titular cuando tenga pagos; para cambiar, crear otra cuenta.
- **B:** snapshot de cuenta en cada pago.
- **C:** referencia mutable actual.
- **Recomendación:** A por simplicidad; B solo si el recibo debe ser autosuficiente.

### D-14. Naturaleza de recibos y documentos contractuales

- **A:** constancias internas, sin valor fiscal; datos suficientes para consulta y contenido estable.
- **B:** documentos contables/fiscales. Requiere requisitos legales fuera del repositorio.
- **C:** simples marcadores técnicos, como el recibo actual.
- **Recomendación:** A. Para contratos, permitir anexos posteriores pero no sustituir/eliminar silenciosamente documentos esenciales.

## 41. Prioridad de correcciones

1. Aprobar D-01 a D-09, especialmente D-02 a D-07.
2. Implementar N-01 a N-06: estados temporales, solapamiento, renovación, fin/rescisión y dinero.
3. Añadir pruebas E2E de contrato futuro, solapamiento, renovación, finalización y rescisión.
4. Implementar N-07/N-08 para pago-cuenta-notificación.
5. Implementar N-09 y definir D-13/D-14 antes del frontend financiero/recibos.
6. Aplicar mejoras recomendadas según la pantalla que se construya.

Pruebas prioritarias faltantes:

- confirmar contrato futuro mientras existe uno actual no solapado;
- rechazar todos los solapamientos parciales y límites contiguos;
- inquilino/propiedad desactivados entre borrador y confirmación;
- finalización antes/en/después de fecha con deuda;
- rescisión con cuotas pagadas, parciales, vencidas, actuales y futuras;
- rescisión con pagos pendientes/confirmados;
- dos confirmaciones concurrentes reales sobre la misma cuota;
- cuenta desactivada entre registro y confirmación;
- fallo deliberado de notificación después de una confirmación financiera;
- montos con más de dos decimales y contrato de renta cero;
- generación de 1, 6 y 12 meses cruzando febrero bisiesto.

## 42. Evaluación módulo por módulo

| Pregunta | Resultado | Justificación |
|---|---|---|
| ¿Lógica de Unidades lista? | ⚠️ LISTO CON MEJORAS RECOMENDADAS | CRUD operativo seguro y sin borrado; falta disponibilidad temporal explícita y semántica de propiedad inactiva |
| ¿Fotografías listas? | ✅ LISTO | Reglas de fuente, orden, portada y archivo coherentes; histórico/portada automática son opcionales |
| ¿Contratos listos para frontend? | 🔴 NO RECOMENDADO CONTINUAR | Estado temporal, solapamiento, renovación y terminación cambian contratos/UI/acciones |
| ¿Archivos de contrato listos? | ⚠️ LISTO CON MEJORAS RECOMENDADAS | Alta/listado e histórico por API funcionan; falta tipificación/fortaleza documental |
| ¿Cuotas correctamente modeladas? | 🟠 REQUIERE CORRECCIONES ANTES DE CONTINUAR | Generación mensual correcta; `ANULADA` no se usa y renta cero queda sin salida |
| ¿Cuentas de pago coherentes? | 🟠 REQUIERE CORRECCIONES ANTES DE CONTINUAR | Desactivación bloquea pagos ya presentados y datos históricos son mutables |
| ¿Pagos seguros para el negocio? | 🟠 REQUIERE CORRECCIONES ANTES DE CONTINUAR | El bloqueo evita sobrepago, pero faltan trazabilidad, tratamiento de cuenta desactivada y aislamiento de notificación |
| ¿Recibos consistentes? | ⚠️ LISTO CON MEJORAS RECOMENDADAS | Relación/único/confirmado correctos; contenido solo sirve como marcador interno |
| ¿Notificaciones coherentes? | 🟠 REQUIERE CORRECCIONES ANTES DE CONTINUAR | Idempotencia/zona correctas; cuotas futuras rescindidas y rollback financiero son incoherentes |
| ¿Cadena completa preparada? | 🟠 REQUIERE CORRECCIONES ANTES DE CONTINUAR | Camino feliz completo, estados terminales incompletos |

## 43. ¿Podemos empezar el frontend de Contratos?

**NO**, si “empezar” significa cerrar contratos, estados, acciones, renovación y visualización de disponibilidad con contratos reales.

Antes deben resolverse exclusivamente estas reglas de backend contractual:

1. Qué representa `VIGENTE` y cómo se registra un contrato futuro confirmado.
2. Qué intervalos bloquean la unidad y cómo se detecta solapamiento.
3. Cuándo y desde qué estados puede renovarse, y si exige continuidad.
4. Cuándo se permite finalizar normalmente y cuándo corresponde rescindir.
5. Qué ocurre al rescindir con cuota actual, cuotas futuras, deuda previa y pagos pendientes.
6. Cuándo la unidad se considera disponible tras finalizar/rescindir.
7. Si propiedad e inquilino deben seguir activos al confirmar.
8. Si la renta puede ser cero y cuál es la moneda/precisión contractual.

Se puede diseñar un prototipo visual o consumir listados/borradores, pero no conviene fijar el contrato de UI de estados y acciones porque esas decisiones cambiarán botones, validaciones, filtros, mensajes y disponibilidad.

## 44. Conclusión final

ORMAN ya expresa correctamente el camino feliz desde contrato mensual hasta pago parcial/total y constancia interna. El riesgo real aparece en el tiempo y en las excepciones del negocio: reserva futura, renovación, finalización y rescisión. La corrección recomendada no requiere microservicios, motores de workflow ni una pasarela de pagos. Requiere definir pocos invariantes fuertes y hacer que las transiciones existentes actualicen de forma atómica las entidades que ya existen, usando especialmente `CuotaEstado.ANULADA` y el bloqueo de cuota disponible.

Una vez aprobadas las decisiones D-01 a D-09 e implementadas las correcciones contractuales N-01 a N-06, el frontend de Contratos podrá construirse sobre un contrato estable. Los ajustes de cuentas, recibos y notificaciones pueden secuenciarse antes de sus respectivas pantallas, salvo el manejo de cuotas/notificaciones por rescisión, que sí forma parte del flujo contractual.
