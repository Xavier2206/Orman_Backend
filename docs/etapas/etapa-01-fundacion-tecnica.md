# Etapa 1 — Fundación técnica

## Propósito

La fundación técnica establece una base predecible antes de implementar el negocio. Define cómo se construye, configura, prueba y evoluciona el backend, y evita que cada módulo resuelva de forma distinta los mismos problemas de infraestructura.

Esta etapa comprende las fases 00 a 03: planificación, normalización del proyecto, configuración de persistencia y creación de infraestructura común.

## Spring Boot

Spring Boot es una plataforma sobre el ecosistema Spring que simplifica la creación de aplicaciones Java. Proporciona configuración automática, un servidor embebido, convenciones y herramientas de diagnóstico. La anotación `@SpringBootApplication` identifica el punto de entrada y habilita el escaneo de componentes desde su paquete y subpaquetes.

La configuración automática no elimina la necesidad de tomar decisiones. Cuando una dependencia está presente, Spring Boot intenta configurar su capacidad; por ejemplo, JPA y Flyway pueden requerir un datasource. Por eso las dependencias y la configuración deben incorporarse de manera coordinada.

## Maven

Maven administra el ciclo de construcción y las dependencias mediante `pom.xml`. Sus fases habituales incluyen compilación, pruebas, empaquetado y verificación. El Maven Wrapper (`mvnw` y `mvnw.cmd`) fija una forma reproducible de invocar Maven sin depender de una instalación global compatible.

En este proyecto, Maven compilará con Java 21 y producirá un JAR. Agregar una dependencia tiene efectos sobre el classpath y también puede activar configuración automática; cada adición debe tener un uso y una justificación.

## Estructura básica de un proyecto Spring Boot

- `pom.xml`: metadatos, dependencias y plugins de construcción.
- `src/main/java`: código de producción, comenzando en `com.orman.backend`.
- `src/main/resources`: configuración y, en fases futuras, migraciones y otros recursos.
- `src/test/java`: pruebas automatizadas.
- `OrmanBackendApplication`: punto de entrada actual de Spring Boot.

Los nuevos módulos se ubicarán por funcionalidad dentro del paquete base. No se crearán carpetas anticipadamente si todavía no existe código que las use.

## `application.yml`

`application.yml` es el formato principal de configuración. YAML representa propiedades jerárquicas mediante indentación, por ejemplo:

```yaml
server:
  port: 9090
```

La indentación debe ser consistente y no se deben incluir secretos reales. En fases posteriores, los valores que cambien por ambiente se obtendrán de variables de entorno o perfiles, sin duplicar innecesariamente la configuración.

## PostgreSQL

PostgreSQL es el sistema gestor de base de datos relacional elegido. Aporta integridad referencial, transacciones y tipos de datos robustos. Su conexión, credenciales y esquema todavía no pertenecen a la Fase 00; se configurarán y verificarán en la Fase 02.

### Datasource y JDBC

El datasource es el componente que administra conexiones para JPA, Hibernate y Flyway. En este proyecto su URL usa el formato JDBC de PostgreSQL y obtiene host, puerto, base, usuario y contraseña de variables de entorno. Host, puerto y nombre pueden tener valores locales no sensibles; usuario y contraseña no deben tener valores predeterminados ni entrar al repositorio.

Spring Boot no carga un archivo `.env` por sí solo. El entorno de ejecución, IntelliJ IDEA, PowerShell o CMD deben proporcionar las variables. Sin credenciales válidas, la aplicación no puede inicializar Flyway ni cargar el contexto.

## JPA e Hibernate

JPA define una especificación Java para mapear objetos a datos relacionales. Hibernate es una implementación habitual de esa especificación y se encarga de materializar consultas, gestionar el contexto de persistencia y convertir entre entidades y filas.

JPA no reemplaza el diseño de base de datos. Las entidades deberán reflejar decisiones explícitas sobre claves, restricciones, relaciones y ciclos de vida. Las futuras APIs tampoco devolverán directamente entidades JPA, para evitar acoplar el contrato HTTP al modelo de persistencia.

## Flyway

Flyway versiona cambios de esquema mediante migraciones ordenadas. Cada migración aplicada queda registrada, lo que permite reproducir la estructura y conocer su historia. Una migración aplicada no se edita: un cambio posterior requiere una nueva versión.

### Migraciones frente a generación automática

Una migración es un cambio explícito, revisable y versionado. La generación automática con `ddl-auto=create` o `update` permite que Hibernate altere el esquema a partir de entidades, pero sus efectos pueden ser implícitos, difíciles de revisar y peligrosos para datos existentes.

Por esta razón, Flyway creará y modificará el esquema. Cuando el esquema exista, Hibernate podrá usar `ddl-auto=validate` para comprobar correspondencia sin alterarlo.

En la Fase 02 se configura `ddl-auto=validate` y `open-in-view=false`. La validación comprueba el modelo contra el esquema, pero no crea ni modifica tablas. Flyway busca migraciones en `classpath:db/migration`, las valida al iniciar y mantiene deshabilitado `clean` para evitar borrados accidentales.

## Lombok y sus riesgos

Lombok genera código repetitivo durante la compilación, como getters o constructores. Puede mejorar la legibilidad, pero un uso indiscriminado oculta comportamiento y resulta problemático en entidades JPA: `equals`, `hashCode` o `toString` generados pueden recorrer relaciones, disparar cargas inesperadas, producir ciclos o exponer datos sensibles.

Se preferirán anotaciones específicas. Lombok no debe reemplazar métodos que expresen reglas de negocio.

## Monolito modular

Un monolito modular se despliega como una sola aplicación, pero separa internamente las capacidades del negocio mediante límites claros. Reduce la complejidad operativa inicial y permite mantener cohesión sin convertir el código en un bloque indivisible.

Los módulos deben comunicarse a través de contratos deliberados y evitar dependencias circulares. Convertirlos en microservicios no es un objetivo inicial.

## Paquetes por funcionalidad

Organizar por funcionalidad agrupa en un mismo módulo los elementos relacionados con una capacidad, por ejemplo `persona` o `usuario`, en lugar de reunir todos los controladores, servicios o repositorios de toda la aplicación en paquetes globales.

Cada funcionalidad puede contener capas internas cuando tengan uso real. Esta organización hace visible la pertenencia al dominio y reduce cruces accidentales.

## Variables de entorno

Las variables de entorno permiten suministrar valores que cambian entre instalaciones, especialmente credenciales y direcciones externas. El repositorio debe contener nombres o valores seguros por defecto cuando corresponda, nunca secretos reales.

Las variables concretas se definirán cuando una fase implemente la capacidad que las necesita. Inventarlas antes produciría documentación que no coincide con el sistema.

## Perfiles de configuración

Los perfiles de Spring permiten activar diferencias controladas entre ambientes, como desarrollo o pruebas. No deben usarse para duplicar toda la configuración ni para ocultar divergencias innecesarias. Su diseño se abordará cuando existan requisitos reales de ambiente.

## Pruebas iniciales

El proyecto incluye una prueba `contextLoads`, cuyo propósito es verificar que el contexto de Spring pueda iniciarse. Es una señal básica de integración, no una prueba de negocio. Al incorporar persistencia, la prueba necesitará un entorno o estrategia coherente con PostgreSQL y Flyway.

Las fases con lógica deberán añadir pruebas de comportamiento relevantes. Una prueba verde no justifica desactivar infraestructura o ignorar errores de configuración.

## Manejo centralizado de errores

Una API necesita traducir errores técnicos y de negocio a respuestas consistentes. Spring permite centralizar esta traducción, por ejemplo mediante mecanismos de manejo global de excepciones. La Fase 03 definirá el contrato, las categorías de error y el registro apropiado; no se implementará de forma anticipada.

La Fase 03 usa `ProblemDetail` y RFC 9457 como contrato HTTP. Además de los campos estándar (`status`, `title`, `detail` e `instance`), el contrato expone un código interno estable, instante, identificador local de trazabilidad y, cuando corresponde, errores de campo ordenados. Las respuestas no incluyen trazas, clases internas, SQL ni credenciales. Un `@RestControllerAdvice` concentra la traducción de excepciones y evita que cada módulo implemente su propio formato.

## Relación entre las fases 00, 01, 02 y 03

1. La **Fase 00** define el mapa, las reglas permanentes y las decisiones iniciales.
2. La **Fase 01** revisará y normalizará la estructura Spring Boot y su construcción sin introducir negocio.
3. La **Fase 02** configurará PostgreSQL y Flyway y establecerá una base verificable para las migraciones.
4. La **Fase 03** incorporará infraestructura común y manejo consistente de errores.

El resultado conjunto será una plataforma mínima, comprensible y comprobada sobre la que se implementarán las funcionalidades de las etapas siguientes.
