# Documentación de base de datos

Este directorio registra el análisis y las decisiones de datos antes de crear migraciones.

- [Modelo inicial analizado](modelo-inicial.md)
- [Tablas postergadas](tablas-postergadas.md)

La Fase 02 preparó el datasource PostgreSQL, Flyway y el directorio `src/main/resources/db/migration/`, sin crear tablas, entidades ni migraciones. Git no versiona directorios vacíos: la primera migración SQL autorizada en una fase posterior dejará el directorio persistido en el repositorio.

Cuando exista una conexión válida, Flyway puede iniciar sin migraciones y crear únicamente `flyway_schema_history`. Las tablas del dominio no se crearán hasta sus fases autorizadas.

La Fase 04 creó `personas` mediante V1 y la migración V2 hizo obligatorio `fecha_registro`, sin editar la migración ya aplicada. La Fase 06 agregó exclusivamente `usuarios` mediante V3, con una referencia uno a uno a `personas` y `ON DELETE RESTRICT`. Sus validaciones se documentan en las fases 04 y 06.
