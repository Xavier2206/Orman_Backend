# Documentación de base de datos

Este directorio registra el análisis y las decisiones de datos antes de crear migraciones.

- [Modelo inicial analizado](modelo-inicial.md)
- [Tablas postergadas](tablas-postergadas.md)

La correccion tecnica documentada en `docs/fases/ajuste-correo-obligatorio-persona.md`
aplica V8 para hacer obligatorio `personas.correo`, sin modificar V1–V7 ni
fabricar valores para registros historicos NULL.

La Fase 13 usa V9 para `otp_challenges`. La Etapa 4.1 usa V10 para
`propiedades`, `unidades` y `unidad_fotos`; V1–V9 permanecen intactas.
La Etapa 4.2 usa V11 para `contratos`, `contrato_archivos` y `cuotas`; V1–V10
permanecen intactas.
La Etapa 4.3 usa V12 para `cuentas_pago`, `pagos`, `pago_comprobantes` y
`recibos`; V1–V11 permanecen intactas.
La Etapa 4.4 usa V13 para `notificaciones`; V1–V12 permanecen intactas.

La Fase 02 preparó el datasource PostgreSQL, Flyway y el directorio `src/main/resources/db/migration/`, sin crear tablas, entidades ni migraciones. Git no versiona directorios vacíos: la primera migración SQL autorizada en una fase posterior dejará el directorio persistido en el repositorio.

Cuando exista una conexión válida, Flyway puede iniciar sin migraciones y crear únicamente `flyway_schema_history`. Las tablas del dominio no se crearán hasta sus fases autorizadas.

La Fase 04 creó `personas` mediante V1 y la migración V2 hizo obligatorio `fecha_registro`, sin editar la migración ya aplicada. La Fase 06 agregó exclusivamente `usuarios` mediante V3, con una referencia uno a uno a `personas` y `ON DELETE RESTRICT`. La Fase 08 agregó `roles` mediante V4 y `rolusu` mediante V5, con `ON DELETE CASCADE` desde Usuario y `ON DELETE RESTRICT` desde Rol. La Etapa 4.1 agregó V10 con relaciones restrictivas hacia `personas`, `propiedades` y `unidades`, checks de dominio, importes `NUMERIC` e índices para las consultas del módulo. La Etapa 4.2 agregó V11 con las relaciones contractuales restrictivas, estados controlados, fechas mensuales, importes `NUMERIC`, cuotas únicas por período y contrato vigente único por Unidad. Sus validaciones se documentan en las fases correspondientes.

La Etapa 4.3 agregó V12 con cuentas propias por Persona propietaria, pagos por cuota con clave de idempotencia, comprobantes por URL y un recibo único por pago. Todas las FKs son restrictivas; `pagos.monto` usa `NUMERIC(14,2)` y sus checks controlan método, estado, origen y la presencia de cuenta según el método.

La Etapa 4.4 agregó V13 con notificaciones internas por Usuario destinatario,
referencias escalares a Cuota o Pago, checks de tipos y lectura, una clave única
antiduplicados e índices para la consulta cronológica y de no leídas.
