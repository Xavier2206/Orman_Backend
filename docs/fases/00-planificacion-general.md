# Fase 00 — Planificación general y estructura documental

## Identificación

- **Estado:** `COMPLETADA`
- **Fecha de inicio:** 2026-07-27
- **Fecha de finalización:** 2026-07-27

## Objetivo

Inspeccionar el proyecto Spring Boot recién creado, establecer reglas permanentes de trabajo y construir la documentación inicial que guiará las siguientes etapas y fases, sin implementar funcionalidades de negocio.

## Alcance

- Inspección mínima de estructura, clase principal, paquete base, Maven, configuración, prueba inicial y archivos Git visibles.
- Reglas permanentes en `AGENTS.md`.
- Plan general de etapas y fases.
- Teoría práctica de la Etapa 1.
- Registro de ejecución de la Fase 00.
- Documentación inicial de arquitectura y base de datos.
- Cinco decisiones arquitectónicas.
- README principal e índices documentales.
- Changelog inicial.
- Migración de la configuración mínima desde properties a YAML y puerto `9090`.
- Validaciones documentales y ejecución de la prueba inicial mediante Maven Wrapper.

## Fuera de alcance

No se implementan entidades JPA, controladores, servicios, repositorios, DTO, migraciones, autenticación, autorización, JWT, OTP, CRUD, endpoints, conexión funcional con PostgreSQL ni tablas. Tampoco se normaliza todavía el `pom.xml` o el nombre interno del artefacto.

## Estado inicial del proyecto

- Proyecto Spring Boot mínimo con `pom.xml` y Maven Wrapper.
- Clase principal: `BackendApplication`.
- Paquete base: `com.orman.backend`.
- Java: 21.
- Configuración existente: `application.properties` con `spring.application.name=backend`.
- Prueba inicial: `BackendApplicationTests.contextLoads()`.
- Dependencias ya presentes: Spring Data JPA, Flyway, Validation, Web MVC, PostgreSQL, Lombok y dependencias de prueba asociadas.
- Archivos Git visibles: `.gitignore` y `.gitattributes`; repositorio `.git` presente.
- README principal inexistente; `HELP.md` generado por Spring Initializr presente.
- Sin clases ni recursos de negocio.

## Decisiones confirmadas

- Arquitectura de monolito modular y paquetes por funcionalidad.
- Paquete base `com.orman.backend`.
- Java 21, Maven, JAR y puerto `9090`.
- `application.yml` como formato principal.
- PostgreSQL como base de datos y Flyway como responsable del esquema.
- Uso controlado de Lombok.
- BCrypt para contraseñas cuando corresponda; nunca texto plano.
- Git administrado manualmente por el usuario.
- Documentación teórica por etapa y registro de ejecución por fase.

## Estructura documental creada

```text
AGENTS.md
README.md
CHANGELOG.md
docs/
├── README.md
├── PLAN_GENERAL.md
├── etapas/
│   └── etapa-01-fundacion-tecnica.md
├── fases/
│   ├── README.md
│   └── 00-planificacion-general.md
├── arquitectura/
│   └── arquitectura-inicial.md
├── database/
│   ├── README.md
│   ├── modelo-inicial.md
│   └── tablas-postergadas.md
└── decisiones/
    ├── ADR-001-monolito-modular.md
    ├── ADR-002-flyway-controla-esquema.md
    ├── ADR-003-git-manual.md
    ├── ADR-004-configuracion-yaml.md
    └── ADR-005-uso-controlado-lombok.md
```

## Archivos creados

- `AGENTS.md`
- `README.md`
- `CHANGELOG.md`
- `docs/README.md`
- `docs/PLAN_GENERAL.md`
- `docs/etapas/etapa-01-fundacion-tecnica.md`
- `docs/fases/README.md`
- `docs/fases/00-planificacion-general.md`
- `docs/arquitectura/arquitectura-inicial.md`
- `docs/database/README.md`
- `docs/database/modelo-inicial.md`
- `docs/database/tablas-postergadas.md`
- `docs/decisiones/ADR-001-monolito-modular.md`
- `docs/decisiones/ADR-002-flyway-controla-esquema.md`
- `docs/decisiones/ADR-003-git-manual.md`
- `docs/decisiones/ADR-004-configuracion-yaml.md`
- `docs/decisiones/ADR-005-uso-controlado-lombok.md`
- `src/main/resources/application.yml`

## Archivos modificados

No se modificaron archivos preexistentes. Los documentos y el YAML son nuevos.

## Archivos eliminados

- `src/main/resources/application.properties`: eliminado después de trasladar `spring.application.name=backend` a YAML.

## Implementación realizada

Se construyó exclusivamente la estructura documental solicitada. Se definieron reglas permanentes, el mapa de trabajo, la teoría de la Etapa 1, decisiones arquitectónicas y el análisis inicial de datos. La configuración mínima se migró a YAML y se añadió el puerto `9090`. No se implementó lógica de aplicación.

## Comandos ejecutados

- `rg --files -g '!target/**'` para inventariar archivos.
- `Get-ChildItem` para inspeccionar la raíz, fuentes y archivos Git visibles.
- `Get-Content` para leer `pom.xml`, clase principal, prueba, configuración y archivos Git de texto.
- `Select-String` para revisar los encabezados útiles de `HELP.md`.
- `New-Item -ItemType Directory` para crear los directorios documentales solicitados.
- Herramienta de parches para crear, actualizar y eliminar los archivos documentados.
- Script PowerShell con `Get-ChildItem`, expresiones regulares, `Test-Path`, `rg` y `Select-Xml` para validar documentos, enlaces, Java, recursos y dependencias.
- `.\mvnw.cmd test`: un intento terminó por el límite operativo de 1 segundo y el segundo reveló un error del script Wrapper antes de iniciar Maven.
- `java -version`, inspección de `.mvn/wrapper/maven-wrapper.properties` y `Get-Command mvn`/`mvn -version` para diagnosticar el entorno.
- `mvn test`: primer intento sin acceso a Maven Central; segundo intento autorizado descargó dependencias, compiló y ejecutó la prueba.
- Inspección de `target/surefire-reports` para confirmar el resumen real.

No se ejecutó ningún comando Git.

## Validaciones realizadas

- 18 archivos Markdown encontrados, incluido `HELP.md`; 0 vacíos.
- 0 enlaces Markdown relativos rotos.
- Solo existen `BackendApplication.java` y `BackendApplicationTests.java`; no se crearon clases de negocio.
- `pom.xml` no fue editado y conserva las 12 dependencias inicialmente inspeccionadas; no se agregaron ni eliminaron dependencias.
- `application.yml` es el único archivo en `src/main/resources` y contiene `server.port: 9090`.
- Java 21 disponible.
- Maven 3.9.16 compiló 1 clase principal y 1 clase de prueba.
- Resultado de pruebas: 1 ejecutada, 0 fallos de aserción, 1 error de carga de contexto, 0 omitidas.
- El error de prueba se debe a que no existe configuración de datasource ni base embebida.

## Errores encontrados

1. El primer `.\mvnw.cmd test` agotó el límite operativo de 1 segundo sin resultado.
2. El Maven Wrapper 3.3.4 con `distributionType=only-script` falla en PowerShell al intentar indexar `Target[0]` sobre una matriz nula y muestra `Cannot start maven from wrapper`.
3. El primer `mvn test` no pudo resolver el parent POM porque el entorno restringía el acceso a Maven Central.
4. Tras autorizar la descarga, `contextLoads` no pudo iniciar el contexto: JPA y Flyway solicitaron un datasource sin URL configurada y Spring no encontró una base embebida.

## Correcciones aplicadas

- Se repitió el Wrapper con tiempo suficiente para distinguir un timeout de su error real.
- Se verificó la versión de Maven instalada y se usó Maven 3.9.16 como alternativa de diagnóstico, sin modificar el Wrapper.
- Se repitió Maven con acceso autorizado a Maven Central y se completaron la resolución de dependencias, compilación y ejecución de la prueba.
- No se alteró la prueba ni se añadió configuración de persistencia para ocultar el error. Su corrección queda fuera de alcance.

## Riesgos

- La prueba de contexto puede requerir un datasource porque JPA, Flyway y PostgreSQL ya están declarados sin configuración funcional.
- El Wrapper actual no inicia Maven en el entorno PowerShell inspeccionado.
- El nombre Maven actual es `backend`; su revisión corresponde a la Fase 01.

## Limitaciones

- Esta fase no valida una conexión PostgreSQL.
- No se crean migraciones ni modelos ejecutables.
- La arquitectura describe límites iniciales, no módulos ya implementados.
- La prueba de contexto no queda verde. La causa es una configuración de persistencia deliberadamente reservada para la Fase 02, por lo que se registra como impedimento externo al alcance documental de la Fase 00.

## Pendientes

- Revisar y normalizar el Maven Wrapper en la Fase 01.
- Revisar el nombre y metadatos Maven actuales en la Fase 01.
- Configurar PostgreSQL, datasource y Flyway en la Fase 02.
- Volver a ejecutar `.\mvnw.cmd test` y confirmar `contextLoads` en verde cuando exista el entorno correspondiente.
- Revisión manual del usuario y operaciones Git manuales.

## Criterios de aceptación

- [x] `AGENTS.md`, plan general, documento de etapa y documento de fase creados.
- [x] `CHANGELOG.md`, ADR, arquitectura y documentación de base de datos creados.
- [x] README principal con enlaces correctos.
- [x] Puerto `9090` configurado en `application.yml`.
- [x] Sin funcionalidades fuera de alcance ni dependencias nuevas.
- [x] Enlaces, documentos y alcance verificados.
- [x] Compilación verificada y error de la prueba documentado sin ocultarlo.
- [x] Impedimentos externos al alcance de la fase documentados.
- [x] Ningún comando Git ejecutado.

## Resultado final

Fase 00 completada en su objetivo documental y de planificación. La estructura, las reglas, las decisiones y la configuración mínima quedaron establecidas. La compilación pasa, pero la prueba de contexto conserva un error conocido por ausencia de datasource; solucionarlo ahora violaría el alcance que reserva PostgreSQL y Flyway para la Fase 02. El impedimento y el defecto del Wrapper quedan explícitamente registrados para las siguientes fases.

## Siguiente fase recomendada

**FASE 01 — REVISIÓN Y NORMALIZACIÓN DEL PROYECTO SPRING BOOT**

No debe comenzar sin autorización expresa del usuario.
