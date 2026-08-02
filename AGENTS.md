# Reglas permanentes de trabajo — ORMAN-BACKEND

## 1. Propósito y alcance

Este archivo define las reglas permanentes e invariantes para trabajar en
ORMAN-BACKEND. No sustituye `docs/PLAN_GENERAL.md`, el documento de la fase
actual ni las decisiones específicas aprobadas para una ejecución.

Estas reglas aplican a diagnósticos, análisis, planificación, implementación
y validación, salvo que una instrucción explícita del usuario limite la
ejecución de forma más restrictiva.

## 2. Jerarquía de instrucciones

- Las instrucciones explícitas del usuario determinan el alcance de la
  ejecución.
- `AGENTS.md` define las reglas permanentes del repositorio.
- `docs/PLAN_GENERAL.md` define la secuencia, dependencias y estados de las
  fases.
- El documento individual de la fase define su alcance, decisiones y criterios
  específicos.
- Una fase no autoriza por sí sola el inicio de otra fase.

## 3. Stack tecnológico

- Usar Java 21.
- Usar Spring Boot 4.1.0.
- Usar Maven Wrapper.
- Generar empaquetado JAR.
- Usar PostgreSQL como base de datos.
- Usar Spring Data JPA, Hibernate, Flyway y Bean Validation.
- Usar Lombok de forma controlada.
- Usar `application.yml` como configuración principal.
- Configurar el servidor en el puerto `9090`.
- Usar `com.orman.backend` como paquete base.

## 4. Arquitectura modular

- Mantener un monolito modular.
- Organizar el código por dominio o funcionalidad.
- No crear microservicios.
- Evitar dependencias circulares entre módulos.
- Los controladores gestionan HTTP y delegan las reglas al servicio.
- Los servicios contienen las reglas de negocio y las transacciones.
- Los repositorios gestionan la persistencia.
- Usar DTO para contratos de entrada y salida.
- Usar mappers explícitos para separar DTO y entidades.
- No exponer entidades JPA directamente desde las APIs.
- No crear abstracciones, capas o componentes sin una necesidad real.

## 5. Convenciones de código

- Preferir inyección por constructor.
- Definir los servicios mediante interfaz e implementación.
- Usar repositorios Spring Data JPA.
- Colocar las transacciones en la capa de servicio.
- Mantener nombres claros y responsabilidades acotadas.
- No colocar lógica de negocio en los controladores.
- No usar `System.out.println`; usar logging apropiado.
- Implementar `equals` y `hashCode` de forma segura en entidades JPA.
- No incluir relaciones JPA, secretos ni datos sensibles en `toString`,
  `equals` o `hashCode`.
- No usar `@Data` indiscriminadamente en entidades JPA.

## 6. PostgreSQL, Hibernate y Flyway

- PostgreSQL es la base de datos real del proyecto.
- Flyway es la única autoridad para crear y evolucionar el esquema.
- Todo cambio de esquema requiere una nueva migración.
- No modificar migraciones aplicadas.
- No usar `IF NOT EXISTS` para ocultar inconsistencias o errores de
  migración.
- No eliminar ni vaciar `flyway_schema_history`.
- No ejecutar `flyway clean`.
- No modificar manualmente el esquema fuera de Flyway.
- Definir nombres explícitos para constraints PK, FK, UK y CHECK.
- Mantener tipos compatibles entre claves primarias y foráneas.
- Usar nombres SQL en minúsculas y `snake_case`.
- Mantener Hibernate con `ddl-auto=validate`.
- No usar `ddl-auto=create`, `create-drop` ni `update`.
- No crear tablas fuera de la fase autorizada.
- No incluir secretos reales en migraciones, configuración, pruebas ni
  documentación.

## 7. API REST y contratos

- Usar el prefijo `/api/v1`.
- Usar DTO separados para entrada y salida.
- Responder `201 Created` al crear recursos y proporcionar `Location` cuando
  corresponda.
- Usar `200 OK` cuando se devuelve un resultado.
- Usar `204 No Content` sin body cuando la operación no devuelve contenido.
- Hacer idempotentes las operaciones de activar y desactivar.
- Reutilizar `PageResponse<T>` para respuestas paginadas.
- No agregar mensajes de éxito redundantes cuando el código HTTP y el recurso
  ya expresan el resultado.
- No devolver entidades JPA directamente.
- Mantener contratos HTTP consistentes entre módulos.

## 8. Errores y logging

- Usar `ProblemDetail` para errores HTTP.
- Responder con `application/problem+json`.
- Mantener códigos `errorCode` estables.
- No exponer SQL, stack traces ni nombres internos de constraints.
- No exponer secretos, contraseñas, hashes ni tokens.
- Registrar `traceId` de forma segura cuando corresponda.
- No incluir información sensible en logs ni mensajes de error.

## 9. Seguridad

- Toda contraseña persistida debe usar BCrypt.
- Nunca almacenar contraseñas en texto plano.
- No exponer `password`, `passwd`, `password_hash` ni hashes BCrypt en APIs,
  DTO, logs, trazas, pruebas o documentación.
- No registrar contraseñas, hashes, tokens ni códigos OTP.
- `spring-security-crypto` puede utilizarse para BCrypt cuando la fase lo
  autorice.
- No activar Spring Security HTTP, `SecurityFilterChain`, filtros,
  autenticación, JWT, sesiones, authorities, autorización u OTP antes de sus
  fases autorizadas.
- `tipo_persona` es una clasificación de negocio y no equivale a un Rol,
  permiso, authority o mecanismo de autorización.
- No incluir secretos reales en el repositorio.

## 10. Pruebas y validación

- Toda fase que incorpore lógica debe incluir pruebas relevantes.
- Incluir pruebas unitarias, de servicio, MVC/controller, persistencia e
  integración según el alcance.
- Ejecutar las pruebas de integración contra PostgreSQL real.
- Probar constraints, defaults, nulabilidad, claves y reglas de persistencia
  cuando correspondan.
- Probar el contrato `ProblemDetail` cuando se incorporen errores HTTP.
- Probar que las respuestas no expongan secretos ni datos sensibles.
- No usar H2.
- No usar Testcontainers.
- No desactivar, omitir ni debilitar pruebas para conseguir una compilación
  exitosa.
- Antes de cerrar una implementación, ejecutar:

  ```powershell
  .\mvnw.cmd clean test
  ```

- Registrar el total real de pruebas, fallos, errores y pruebas omitidas.
- No declarar una fase completada si fallan las validaciones requeridas, salvo
  que exista un impedimento externo admitido y documentado.

## 11. Documentación

En fases de implementación autorizadas:

- actualizar el documento individual de la fase;
- actualizar `docs/PLAN_GENERAL.md` cuando cambie el estado o alcance;
- actualizar `CHANGELOG.md` al cerrar una fase;
- actualizar arquitectura y documentación de base de datos cuando
  corresponda;
- crear o actualizar la guía Postman cuando se agreguen endpoints;
- registrar decisiones, archivos creados, modificados o eliminados,
  validaciones, resultados, errores, riesgos y pendientes.

En análisis o planificación no mutante:

- no modificar documentación;
- registrar en el informe las actualizaciones pendientes.

## 12. Git y preservación de cambios

- El usuario ejecuta Git manualmente.
- No ejecutar `git add`, `git commit` ni `git push`.
- No cambiar ramas.
- No modificar el historial.
- No revertir, restaurar, limpiar ni eliminar cambios del usuario.
- No incluir archivos no relacionados con la tarea autorizada.
- Se pueden usar comandos Git exclusivamente de lectura cuando sean
  necesarios para auditar el estado.
- Al final solo sugerir los comandos Git manuales apropiados, sin ejecutarlos.

## 13. Trabajo por fases

- Trabajar únicamente en la fase expresamente autorizada.
- No adelantar fases.
- No agregar tablas, migraciones, dependencias ni funcionalidades de fases
  futuras.
- Respetar el alcance y las exclusiones aprobadas.
- No aprovechar una corrección para agregar funcionalidades.
- Distinguir claramente entre diagnóstico, análisis y diseño, implementación y
  validación.
- En una implementación completa, presentar antes el objetivo, alcance,
  archivos, riesgos y validación prevista.
- En una corrección puntual, presentar la causa, archivos afectados,
  corrección mínima y validación.
- Detenerse al concluir el trabajo autorizado.

## 14. Prohibiciones generales

- No crear archivos, carpetas, dependencias o abstracciones sin uso real y
  autorización dentro del alcance.
- No modificar migraciones aplicadas.
- No crear SQL fuera de migraciones Flyway autorizadas.
- No exponer entidades JPA, secretos, contraseñas, hashes, tokens o SQL.
- No usar generación automática de tablas por Hibernate.
- No mezclar responsabilidades entre módulos o capas sin justificación.
- No ejecutar operaciones destructivas sobre el repositorio o los cambios del
  usuario.
- No iniciar una fase futura aunque sea técnicamente conveniente.
- No declarar completado trabajo que no haya sido validado y documentado.

## 15. Checklist general de cierre

Antes de cerrar una implementación, verificar que:

- el objetivo y el alcance autorizado se cumplieron;
- no se incorporaron funcionalidades de fases futuras;
- los archivos creados, modificados y eliminados están registrados;
- las pruebas y validaciones requeridas fueron ejecutadas;
- los resultados reales están documentados;
- no existen errores bloqueantes conocidos;
- la documentación requerida está actualizada;
- las decisiones, riesgos y pendientes están registrados;
- el usuario debe revisar los cambios;
- Git queda para ejecución manual del usuario.
