# Documentación de base de datos

Este directorio registra el análisis y las decisiones de datos antes de crear migraciones.

- [Modelo inicial analizado](modelo-inicial.md)
- [Tablas postergadas](tablas-postergadas.md)

La correccion tecnica documentada en `docs/fases/ajuste-correo-obligatorio-persona.md`
aplica V8 para hacer obligatorio `personas.correo`, sin modificar V1–V7 ni
fabricar valores para registros historicos NULL.

La Fase 13 usa V9 para `otp_challenges`. La Etapa 4.1 usa V10 para
`propiedades`, `unidades` y `unidad_fotos`; V1–V9 permanecen intactas.

La Fase 02 preparó el datasource PostgreSQL, Flyway y el directorio `src/main/resources/db/migration/`, sin crear tablas, entidades ni migraciones. Git no versiona directorios vacíos: la primera migración SQL autorizada en una fase posterior dejará el directorio persistido en el repositorio.

Cuando exista una conexión válida, Flyway puede iniciar sin migraciones y crear únicamente `flyway_schema_history`. Las tablas del dominio no se crearán hasta sus fases autorizadas.

La Fase 04 creó `personas` mediante V1 y la migración V2 hizo obligatorio `fecha_registro`, sin editar la migración ya aplicada. La Fase 06 agregó exclusivamente `usuarios` mediante V3, con una referencia uno a uno a `personas` y `ON DELETE RESTRICT`. La Fase 08 agregó `roles` mediante V4 y `rolusu` mediante V5, con `ON DELETE CASCADE` desde Usuario y `ON DELETE RESTRICT` desde Rol. La Etapa 4.1 agregó V10 con relaciones restrictivas hacia `personas`, `propiedades` y `unidades`, checks de dominio, importes `NUMERIC` e índices para las consultas del módulo. Sus validaciones se documentan en las fases correspondientes.
