# Informe de auditoría técnica del backend

**Proyecto:** ORMAN-BACKEND  
**Fecha de auditoría:** 2026-09-14  
**Alcance:** estado real del repositorio local, sin modificar código ni documentación existente.  
**Archivo generado:** `docs/INFORME_AUDITORIA_BACKEND.md`

## 1. Resumen ejecutivo

El backend es un monolito modular Java/Spring Boot con una base funcional amplia: autenticación con JWT y sesiones persistidas, OTP por correo para acceso WEB administrativo, autorización por roles, personas, usuarios, roles, menús, procesos, propiedades, unidades, contratos, cuotas, pagos, comprobantes, recibos y notificaciones internas.

La compilación y la suite actual son satisfactorias: `./mvnw.cmd clean test` terminó con **BUILD SUCCESS**, **351 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas**, utilizando PostgreSQL local real y validando Flyway V1–V15 con Hibernate en `ddl-auto=validate`.

La conclusión de la auditoría no es, sin embargo, “listo para producción”. Los riesgos más importantes están en el ciclo contractual-financiero, el control de abuso de autenticación, la dependencia transaccional de notificaciones, la configuración insegura por defecto de la cookie de refresh y la ausencia de preparación operacional para producción. También existen defectos de validación y consistencia de errores que pueden producir respuestas 500 ante entradas o estados esperables.

**Resultado por severidad:**

| Severidad | Cantidad | Evaluación |
|---|---:|---|
| CRÍTICO | 0 | No se encontró evidencia de pérdida inmediata de datos, secreto expuesto o bloqueo general del sistema. |
| ALTO | 4 | Deben resolverse antes de exponer el backend a usuarios reales o avanzar con funcionalidades dependientes. |
| MEDIO | 10 | No bloquean el flujo básico, pero afectan seguridad, consistencia, operación o evolución. |
| BAJO | 7 | Deuda de calidad, documentación, mantenibilidad o alcance pendiente sin impacto crítico inmediato. |

## 2. Estado general del backend

| Área | Estado observado |
|---|---|
| Construcción | Implementada. Java 21, Maven Wrapper, empaquetado JAR. |
| Aplicación | Implementada. `OrmanBackendApplication` habilita componentes, propiedades y scheduling. |
| API REST | Implementada, 119 mappings bajo `/api/v1`. |
| Persistencia | Implementada con Spring Data JPA, Hibernate y PostgreSQL. |
| Esquema | Flyway V1–V15 aplicado y validado en la ejecución auditada. |
| Seguridad | Implementada con Spring Security, JWT HS256, sesiones, BCrypt, CORS, CSRF para refresh WEB y OTP. |
| Modelo inmobiliario | Implementado hasta propiedades, unidades, contratos, cuotas, pagos y notificaciones. |
| DTO/mapeo | Implementado; no se observó retorno directo de entidades JPA desde controllers. |
| Pruebas | Amplias para los módulos existentes; no hay herramienta de cobertura. |
| Documentación API | Guías Postman por módulo; OpenAPI/Swagger **NO IMPLEMENTADO**. |
| Dashboard, mantenimiento y reportes | **NO IMPLEMENTADOS**: los directorios existen pero están vacíos. |
| Preparación productiva | Parcial. No hay Docker, CI/CD, perfil de producción, observabilidad, backup ni guía operacional verificable. |

## 3. Instrucciones revisadas y método

Se recorrió la estructura del repositorio y se revisaron fuentes, recursos, migraciones, pruebas, configuración, README, plan, changelog, ADRs y guías Postman.

Se intentó localizar `9.agents.md`, `9.AGENTS.md`, `agents.md` y archivos equivalentes dentro del repositorio. El resultado real fue **NO ENCONTRADO**; solo existe y fue leído completamente `AGENTS.md` en la raíz. Sus reglas fueron aplicadas durante la auditoría. En particular, no se modificó código, no se ejecutaron comandos Git de escritura y solo se creó el informe solicitado.

Validaciones ejecutadas:

```text
.\mvnw.cmd clean test
BUILD SUCCESS
Tests run: 351, Failures: 0, Errors: 0, Skipped: 0
PostgreSQL real local; Flyway V1–V15; Hibernate ddl-auto=validate
```

Las pruebas no verifican por sí solas SMTP real, despliegue multiinstancia, TLS del reverse proxy, persistencia del filesystem en producción, backups, recuperación ante desastre ni carga real. Esos aspectos quedan como **NO VERIFICADO**.

## 4. Arquitectura encontrada

La estructura principal es un monolito modular organizado por dominio bajo `src/main/java/com/orman/backend`:

```text
auth            autorización HTTP, JWT, OTP, sesiones y contexto
authorization   reglas de alcance propio/administrador/propietario
common          DTO común y errores globales
config          BCrypt, reloj y configuración transversal
contract        contratos, archivos y cuotas
menu            menús y relación menú-proceso
notification    notificaciones y scheduler de cuotas
payment         cuentas, pagos, comprobantes y recibos
person          personas y fotografías
process         procesos
property        propiedades, unidades y fotografías
role            roles y relaciones usuario/menú
user            usuarios
dashboard       vacío
maintenance     vacío
report          vacío
shared          vacío
```

La separación `controller → service → repository` es reconocible y, en general, correcta. Los controllers delegan las reglas a servicios; los servicios tienen interfaces e implementaciones en los módulos principales; las entidades no se exponen directamente; los DTOs se convierten mediante mappers explícitos.

La arquitectura no muestra dependencias circulares evidentes a nivel de paquetes. Hay acoplamientos legítimos entre dominios, por ejemplo propiedad → contrato → pago → notificación, y varios servicios de ownership que repiten navegación hacia la persona propietaria. El acoplamiento más delicado es que los eventos de pago se manejan de forma síncrona dentro del mismo contexto transaccional.

Conteo observado en producción:

| Elemento | Cantidad |
|---|---:|
| Archivos Java de producción | 280 |
| Controllers REST | 22 |
| Clases con `@Entity` | 31 |
| Clases con `@Service` | 40 |
| Archivos con `@Transactional` | 33 |
| Usos de `@PreAuthorize` | 21 |
| Mappings REST | 119 |

## 5. Tecnologías y dependencias

Evidencia principal: `pom.xml:5-120`.

| Tecnología/dependencia | Uso real |
|---|---|
| Java 21 | Versión declarada en `pom.xml:18`. |
| Spring Boot 4.1.0 | Parent en `pom.xml:5-9`. |
| Spring Web MVC | Controllers REST y multipart. |
| Spring Data JPA / Hibernate | Persistencia y consultas JPQL/native. |
| PostgreSQL | Driver runtime y base real de integración. |
| Flyway | Migraciones versionadas V1–V15. |
| Bean Validation | DTOs y propiedades de configuración. |
| Spring Security | Filter chain, método seguro, CORS/CSRF y BCrypt. |
| Nimbus JOSE JWT 10.8 | Firma/validación JWT HS256. |
| Spring Mail | Envío OTP por SMTP. |
| Lombok | `@RequiredArgsConstructor`, getters/setters acotados; no se encontró `@Data` en entidades. |
| DevTools | Runtime opcional. |
| Testing starters | JPA, Flyway, Validation, Web MVC y Spring Security. |

No hay dependencia OpenAPI/Swagger, Actuator, JaCoCo, escáner de dependencias, SBOM, plugin de formato, Docker o integración CI visible en el repositorio.

## 6. Mapa completo de módulos

| Módulo | Objetivo y piezas reales | Estado |
|---|---|---|
| `auth` | `AuthController`, `AuthServiceImpl`, `JwtAuthenticationFilter`, `NimbusJwtService`, `SessionServiceImpl`, OTP y `AuthContext`. | ⚠️ Implementado con observaciones de abuso, OTP y configuración productiva. |
| `authorization` | `AuthorizationServiceImpl` y `OwnerProtectionServiceImpl`; decisiones de alcance y protección del último propietario. | ✅ Implementado y cubierto por integración, con revisión de coherencia necesaria al crecer el dominio. |
| `person` | `Persona`, CRUD, resumen, filtros, foto local y `PersonaServiceImpl`. | ⚠️ Implementado; validación defectuosa y eliminación con error 500 posible. |
| `user` | `Usuario`, CRUD, estado, cambio de contraseña y revocación de sesiones. | ✅ Implementado y aparentemente funcional. |
| `role` | `Rol`, asignaciones usuario-rol y rol-menú, protección de `PROPIETARIO`. | ✅ Implementado; sin eliminación y con código muy comprimido en parte del módulo. |
| `menu` | Menús y asignación menú-proceso. | ✅ Implementado; ordenamiento y manejo de integridad mejorables. |
| `process` | CRUD, activación y desactivación de procesos. | ⚠️ Implementado; no tiene filtros y el sort solicitado puede ser ignorado. |
| `property` | Propiedades, unidades, portada y fotos externas/internas. | ⚠️ Implementado; filesystem local y consistencia transaccional de archivos. |
| `contract` | Contratos BORRADOR/VIGENTE/FINALIZADO/RESCINDIDO, renovación, archivos y cuotas. | ⚠️ Implementado; rescisión no propaga estado a cuotas/pagos/notificaciones. |
| `payment` | Cuentas de pago, pagos pendientes, revisión, comprobantes y recibos. | ⚠️ Implementado; dependencia con contrato y concurrencia pendientes. |
| `notification` | Bandeja propia, lectura, eventos de pago y recordatorios scheduler. | ⚠️ Implementado; zona configurable ignorada y scheduler no filtra contrato rescindido. |
| `dashboard` | Directorio vacío. | ❌ NO IMPLEMENTADO. |
| `maintenance` | Directorio vacío. | ❌ NO IMPLEMENTADO. |
| `report` | Directorio vacío. | ❌ NO IMPLEMENTADO. |
| `shared` | Directorio vacío; lo transversal está en `common`/`config`. | ❌ No hay módulo compartido real. |

### Flujos funcionales reconstruidos

1. El login valida usuario/persona activos, BCrypt y autoridades. Para WEB administrativo crea desafío OTP, almacena digest HMAC y envía el código por SMTP. Tras verificarlo crea una sesión persistida, refresh token rotatorio y access token JWT.
2. El filtro JWT valida firma/claims, carga la sesión por `sid`, verifica revocación, expiración y estado de usuario/persona, y vuelve a cargar authorities desde la base.
3. Propiedades pertenecen a una persona propietaria. Unidades pertenecen a propiedades. El servicio de ownership limita los recursos al propietario autenticado.
4. Un contrato se crea en BORRADOR, se confirma si la unidad está operativa y no tiene otro vigente, y entonces genera cuotas mensuales PENDIENTE.
5. Un pago se crea PENDIENTE_REVISION. El propietario puede confirmar, rechazar o anular. La confirmación bloquea la cuota, suma pagos confirmados, cambia la cuota y genera recibo.
6. Los eventos de comprobante, confirmación y rechazo generan notificaciones internas para el usuario propietario; el scheduler genera recordatorios de cuotas próximas/vencidas.

## 7. Inventario completo de endpoints REST

Todos los endpoints usan el prefijo `/api/v1`. En la tabla: `PUB` es público; `AUTH` requiere autenticación; `OA` es `PROPIETARIO` o `ADMINISTRADOR`; `OWN` es la regla `authorizationService`; `P` es `PROPIETARIO`.

### Autenticación y sesiones — `AuthController` (`authService`, `sessionService`)

| Método y ruta | Entrada/salida y seguridad | Código/observaciones |
|---|---|---|
| `POST /auth/login` | `LoginRequest` válido → `LoginResponse`; PUB | 200; puede responder desafío OTP. |
| `POST /auth/refresh` | Cookie WEB o `RefreshRequest` MOBILE → `LoginResponse`; PUB | 200; rota refresh token y cookie cuando corresponde. |
| `POST /auth/otp/verify` | `OtpVerifyRequest` → `LoginResponse`; PUB | 200; completa login WEB. |
| `POST /auth/otp/resend` | `OtpResendRequest`; PUB | 204; envía nuevo OTP. |
| `POST /auth/logout` | principal autenticado | 204; revoca sesión actual y borra cookie. |
| `POST /auth/logout-all` | principal autenticado | 204; revoca sesiones del login. |
| `GET /auth/sessions` | sin body → lista `SessionResponse`; AUTH | 200; lista sesiones no revocadas/no expiradas. |
| `GET /auth/context` | sin body → contexto de usuario/persona/roles/menús/procesos; AUTH | 200; usa `AuthContextService`. |
| `DELETE /auth/sessions/{sid}` | UUID `sid`; AUTH | 204; solo sesión propia. |

### Personas — `PersonaController` (`PersonaService`, `PersonaPhotoService`)

| Método y ruta | Entrada/salida y seguridad | Código/observaciones |
|---|---|---|
| `POST /personas` | `CreatePersonaRequest` + `@Valid` → `PersonaResponse`; OA | 201 + `Location`. |
| `GET /personas/resumen` | sin body → resumen; OA | 200. |
| `GET /personas/{codper}` | Integer → `PersonaResponse`; OWN | 200. |
| `GET /personas` | `q`, `tipoPersona`, `estado`, `page/size/sort` → `PageResponse`; OA | 200; tamaño limitado a 100. |
| `PUT /personas/{codper}` | `UpdatePersonaRequest` + `@Valid`; OWN | 200. |
| `PATCH /personas/{codper}/desactivar` | Integer; OWN | 200; idempotente según servicio. |
| `PATCH /personas/{codper}/activar` | Integer; OWN | 200; idempotente según servicio. |
| `DELETE /personas/{codper}` | Integer; OWN | 204; FK puede producir 500 no controlado. |
| `PUT /personas/{codper}/foto` | multipart `foto`; OWN | 204; procesa y guarda JPG local. |
| `GET /personas/{codper}/foto` | Integer → `Resource`; OWN | 200; recurso binario. |
| `DELETE /personas/{codper}/foto` | Integer; OWN | 204. |

### Usuarios — `UsuarioController` (`UsuarioService`)

| Método y ruta | Entrada/salida y seguridad | Código/observaciones |
|---|---|---|
| `POST /usuarios` | `CreateUsuarioRequest` + `@Valid` → usuario; OA | 201 + `Location`; contraseña se cifra con BCrypt. |
| `GET /usuarios/{login}` | login → usuario; `canManageUser` | 200. |
| `GET /usuarios` | `q`, paginación → página; OA | 200; propietario ve alcance completo; administrador excluye propietarios activos. |
| `PUT /usuarios/{login}` | `UpdateUsuarioRequest` + `@Valid`; `canManageUser` | 200; estado. |
| `PATCH /usuarios/{login}/desactivar` | login; `canManageUser` | 200; revoca sesiones al inactivar. |
| `PATCH /usuarios/{login}/activar` | login; `canManageUser` | 200. |
| `PUT /usuarios/{login}/password` | `ChangePasswordRequest` + `@Valid`; self/owner | 204; BCrypt y revocación total de sesiones. |

### Roles y relaciones — `RolController`, `RolUsuController`, `RolMeController`

| Método y ruta | Controller/servicio | Seguridad y resultado |
|---|---|---|
| `POST /roles` | `RolController` / `RolService` | P; 201 + `Location`; `CreateRolRequest`. |
| `GET /roles/{codr}` | `RolController` / `RolService` | P; 200. |
| `GET /roles/resumen` | `RolController` / `RolService` | P; 200. |
| `GET /roles` | `RolController` / `RolService` | P; 200; `q`, `estado`, página. |
| `PUT /roles/{codr}` | `RolController` / `RolService` | P; 200; protege rol reservado. |
| `PATCH /roles/{codr}/activar` | `RolController` / `RolService` | P; 200; idempotente. |
| `PATCH /roles/{codr}/desactivar` | `RolController` / `RolService` | P; 200; protege último propietario. |
| `POST /usuarios/{login}/roles/{codr}` | `RolUsuController` / `RolUsuService` | P; 201; asignación. |
| `DELETE /usuarios/{login}/roles/{codr}` | `RolUsuController` / `RolUsuService` | P; 204; protección del último propietario. |
| `GET /usuarios/{login}/roles` | `RolUsuController` / `RolUsuService` | P; 200; lista. |
| `GET /roles/{codr}/usuarios` | `RolUsuController` / `RolUsuService` | P; 200; lista. |
| `POST /roles/{codr}/menus/{codm}` | `RolMeController` / `RolMeService` | P; 201; asignación. |
| `DELETE /roles/{codr}/menus/{codm}` | `RolMeController` / `RolMeService` | P; 204. |
| `GET /roles/{codr}/menus` | `RolMeController` / `RolMeService` | P; 200. |
| `GET /menus/{codm}/roles` | `RolMeController` / `RolMeService` | P; 200. |

### Menús y procesos — `MenuController`, `MeProController`, `ProcesoController`

| Método y ruta | Controller/servicio | Seguridad y resultado |
|---|---|---|
| `POST /menus` | `MenuController` / `MenuService` | P; 201; `CreateMenuRequest`. |
| `GET /menus/{codm}` | `MenuController` / `MenuService` | P; 200. |
| `GET /menus/resumen` | `MenuController` / `MenuService` | P; 200. |
| `GET /menus` | `MenuController` / `MenuService` | P; 200; `q`, `estado`, página. |
| `PUT /menus/{codm}` | `MenuController` / `MenuService` | P; 200. |
| `PATCH /menus/{codm}/activar` | `MenuController` / `MenuService` | P; 200. |
| `PATCH /menus/{codm}/desactivar` | `MenuController` / `MenuService` | P; 200. |
| `POST /menus/{codm}/procesos/{codp}` | `MeProController` / `MeProService` | P; 201. |
| `DELETE /menus/{codm}/procesos/{codp}` | `MeProController` / `MeProService` | P; 204. |
| `GET /menus/{codm}/procesos` | `MeProController` / `MeProService` | P; 200. |
| `GET /procesos/{codp}/menus` | `MeProController` / `MeProService` | P; 200. |
| `POST /procesos` | `ProcesoController` / `ProcesoService` | P; 201; `CreateProcesoRequest`. |
| `GET /procesos/{codp}` | `ProcesoController` / `ProcesoService` | P; 200. |
| `GET /procesos` | `ProcesoController` / `ProcesoService` | P; 200; página, sin filtros. |
| `PUT /procesos/{codp}` | `ProcesoController` / `ProcesoService` | P; 200. |
| `PATCH /procesos/{codp}/activar` | `ProcesoController` / `ProcesoService` | P; 200. |
| `PATCH /procesos/{codp}/desactivar` | `ProcesoController` / `ProcesoService` | P; 200. |

### Propiedades, unidades y fotografías

| Método y ruta | Controller/servicio | Seguridad y resultado |
|---|---|---|
| `POST /propiedades` | `PropiedadController` / `PropiedadService` | P; 201; `PropiedadRequest`. |
| `GET /propiedades/resumen` | `PropiedadController` / `PropiedadService` | P; 200; agregados. |
| `GET /propiedades` | `PropiedadController` / `PropiedadService` | P; 200; `q`, `tipo`, `estado`, página limitada a 100. |
| `GET /propiedades/{codprop}` | `PropiedadController` / `PropiedadService` | P; 200. |
| `PUT /propiedades/{codprop}` | `PropiedadController` / `PropiedadService` | P; 200. |
| `PUT /propiedades/{codprop}/portada` | multipart; `PropiedadPortadaService` | P; 204. |
| `GET /propiedades/{codprop}/portada` | recurso binario; `PropiedadPortadaService` | P; 200. |
| `DELETE /propiedades/{codprop}/portada` | `PropiedadPortadaService` | P; 204. |
| `PATCH /propiedades/{codprop}/activar` | `PropiedadService` | P; 200. |
| `PATCH /propiedades/{codprop}/desactivar` | `PropiedadService` | P; 200. |
| `POST /propiedades/{codprop}/unidades` | `UnidadController` / `UnidadService` | P; 201; `UnidadRequest`. |
| `GET /propiedades/{codprop}/unidades` | `UnidadController` / `UnidadService` | P; 200; `estadoOperativo`, página. |
| `GET /unidades/{coduni}` | `UnidadController` / `UnidadService` | P; 200. |
| `PUT /unidades/{coduni}` | `UnidadController` / `UnidadService` | P; 200; el estado se cambia por acciones. |
| `PATCH /unidades/{coduni}/activar` | `UnidadController` / `UnidadService` | P; 200. |
| `PATCH /unidades/{coduni}/desactivar` | `UnidadController` / `UnidadService` | P; 200. |
| `POST /unidades/{coduni}/fotos` JSON | `UnidadFotoController` / `UnidadFotoService` | P; 201; URL externa. |
| `POST /unidades/{coduni}/fotos` multipart | `UnidadFotoController` / `UnidadFotoService` | P; 201; archivo interno. |
| `GET /unidades/{coduni}/fotos` | `UnidadFotoController` / `UnidadFotoService` | P; 200; lista. |
| `PUT /unidades/{coduni}/fotos/{id}` JSON | `UnidadFotoController` / `UnidadFotoService` | P; 200; reemplaza metadata/URL. |
| `PATCH /unidades/{coduni}/fotos/{id}/metadata` | `UnidadFotoController` / `UnidadFotoService` | P; 200. |
| `PUT /unidades/{coduni}/fotos/{id}/archivo` multipart | `UnidadFotoController` / `UnidadFotoService` | P; 200; reemplaza archivo. |
| `GET /unidades/{coduni}/fotos/{id}/archivo` | recurso binario | P; 200. |
| `PATCH /unidades/{coduni}/fotos/{id}/portada` | `UnidadFotoController` / `UnidadFotoService` | P; 200. |
| `DELETE /unidades/{coduni}/fotos/{id}` | `UnidadFotoController` / `UnidadFotoService` | P; 204. |

### Contratos y cuotas

| Método y ruta | Controller/servicio | Seguridad y resultado |
|---|---|---|
| `POST /unidades/{coduni}/contratos` | `ContratoController` / `ContratoService` | P; 201; crea BORRADOR. |
| `GET /unidades/{coduni}/contratos` | `ContratoController` / `ContratoService` | P; 200; página. |
| `GET /contratos` | `ContratoController` / `ContratoService` | P; 200; `coduni`, `estado`, página. |
| `GET /contratos/{codcon}` | `ContratoController` / `ContratoService` | P; 200. |
| `PUT /contratos/{codcon}` | `ContratoController` / `ContratoService` | P; 200; solo BORRADOR. |
| `PATCH /contratos/{codcon}/confirmar` | `ContratoController` / `ContratoService` | P; 200; genera cuotas. |
| `PATCH /contratos/{codcon}/finalizar` | `ContratoController` / `ContratoService` | P; 200; solo VIGENTE. |
| `POST /contratos/{codcon}/renovaciones` | `ContratoController` / `ContratoService` | P; 201; crea renovación BORRADOR. |
| `PATCH /contratos/{codcon}/rescindir` | `ContratoController` / `ContratoService` | P; 200; solo VIGENTE. |
| `POST /contratos/{codcon}/archivos` | `ContratoArchivoController` / `ContratoArchivoService` | P; 201; URL externa, no multipart. |
| `GET /contratos/{codcon}/archivos` | `ContratoArchivoController` / `ContratoArchivoService` | P; 200. |
| `GET /contratos/{codcon}/cuotas` | `CuotaController` / `CuotaService` | P; 200; lista completa sin paginación. |

### Pagos, cuentas y recibos

| Método y ruta | Controller/servicio | Seguridad y resultado |
|---|---|---|
| `POST /cuentas-pago` | `CuentaPagoController` / `CuentaPagoService` | P; 201; `CuentaPagoRequest`. |
| `GET /cuentas-pago` | `CuentaPagoController` / `CuentaPagoService` | P; 200; `estado`, página. |
| `GET /cuentas-pago/{codcta}` | `CuentaPagoController` / `CuentaPagoService` | P; 200. |
| `PUT /cuentas-pago/{codcta}` | `CuentaPagoController` / `CuentaPagoService` | P; 200. |
| `PATCH /cuentas-pago/{codcta}/activar` | `CuentaPagoController` / `CuentaPagoService` | P; 200. |
| `PATCH /cuentas-pago/{codcta}/desactivar` | `CuentaPagoController` / `CuentaPagoService` | P; 200. |
| `POST /cuotas/{codcuo}/pagos` | `PagoController` / `PagoService` | P; 201; `PagoRequest` con UUID de idempotencia. |
| `GET /cuotas/{codcuo}/pagos` | `PagoController` / `PagoService` | P; 200; lista completa sin paginación. |
| `GET /pagos` | `PagoController` / `PagoService` | P; 200; `estado`, `metodo`, página limitada a 100. |
| `GET /pagos/{codpag}` | `PagoController` / `PagoService` | P; 200. |
| `PATCH /pagos/{codpag}/confirmar` | `PagoController` / `PagoService` | P; 200; crea recibo y evento. |
| `PATCH /pagos/{codpag}/rechazar` | `PagoController` / `PagoService` | P; 200; requiere motivo y evento. |
| `PATCH /pagos/{codpag}/anular` | `PagoController` / `PagoService` | P; 200; requiere motivo. |
| `POST /pagos/{codpag}/comprobantes` | `PagoComprobanteController` / `PagoComprobanteService` | P; 201; URL externa. |
| `GET /pagos/{codpag}/comprobantes` | `PagoComprobanteController` / `PagoComprobanteService` | P; 200. |
| `DELETE /pagos/{codpag}/comprobantes/{id}` | `PagoComprobanteController` / `PagoComprobanteService` | P; 204. |
| `GET /pagos/{codpag}/recibo` | `ReciboController` / `ReciboService` | P; 200. |
| `GET /recibos/{codrec}` | `ReciboController` / `ReciboService` | P; 200. |

### Notificaciones

| Método y ruta | Controller/servicio | Seguridad y resultado |
|---|---|---|
| `GET /notificaciones` | `NotificacionController` / `NotificacionService` | P; 200; `tipo`, `leida`, página limitada a 100. |
| `GET /notificaciones/resumen` | `NotificacionController` / `NotificacionService` | P; 200; no leídas. |
| `GET /notificaciones/{codnot}` | `NotificacionController` / `NotificacionService` | P; 200; solo destinatario propio. |
| `PATCH /notificaciones/{codnot}/leer` | `NotificacionController` / `NotificacionService` | P; 200; lectura idempotente. |
| `POST /cuotas/{codcuo}/notificar` | `CuotaNotificacionController` / `NotificacionGeneracionService` | P; 200; recordatorio manual. |

No se encontraron endpoints públicos de inquilinos, endpoints de dashboard, mantenimiento o reportes, ni carga binaria de archivos de contrato/comprobante. Esas capacidades son **NO IMPLEMENTADAS** o siguen siendo URL externas según el código real.

## 8. Flujo Controller → Service → Repository

| Módulo | Controller | Service principal | Repository/infraestructura |
|---|---|---|---|
| Auth | `AuthController` | `AuthService`, `SessionService`, `AuthContextService` | usuarios, sesiones, OTP, roles/menús/procesos |
| Persona | `PersonaController` | `PersonaService`, `PersonaPhotoService` | `PersonaRepository`, filesystem |
| Usuario | `UsuarioController` | `UsuarioService` | `UsuarioRepository`, `SessionService`, `PasswordEncoder` |
| Authorization | method security | `AuthorizationService`, `OwnerProtectionService` | usuario, persona, rol y asignaciones |
| Roles | tres controllers | `RolService`, `RolUsuService`, `RolMeService` | `RolRepository`, `RolUsuRepository`, `RolMeRepository` |
| Menú/proceso | tres controllers | `MenuService`, `MeProService`, `ProcesoService` | repositorios propios |
| Propiedad | tres controllers | `PropiedadService`, `UnidadService`, `PropertyOwnershipService` | propiedad, unidad y foto |
| Contrato | tres controllers | `ContratoService`, `ContratoArchivoService`, `CuotaService` | contrato, archivo, cuota y ownership |
| Pago | cuatro controllers | `CuentaPagoService`, `PagoService`, `PagoComprobanteService`, `ReciboService` | repositorios de pagos y cuotas |
| Notificación | dos controllers + scheduler | `NotificacionService`, `NotificacionGeneracionService` | `NotificacionRepository`, `CuotaRepository` |

Los controllers son mayormente delgados. Las excepciones de diseño observadas son la bifurcación de alcance en `UsuarioController.list(...)` y el límite de `Pageable`, que siguen siendo reglas pequeñas y no justifican extraer una capa adicional.

## 9. Entidades, JPA y base de datos

### Migraciones reales

Flyway contiene V1–V15 en `src/main/resources/db/migration`:

| Versión | Cambio |
|---|---|
| V1 | `personas`, PK identity, CI único, género/estado/tipo con checks. |
| V2 | `fecha_registro` de persona obligatoria. |
| V3 | `usuarios`, FK y relación uno-a-uno lógica con persona. |
| V4 | `roles`. |
| V5 | `rolusu`, relación usuario-rol. |
| V6 | `sesiones_usuario`, refresh hash, revocación y dispositivo. |
| V7 | `menus`, `procesos`, `rolme`, `mepro`. |
| V8 | `personas.correo` obligatorio; falla deliberadamente si existen nulos. |
| V9 | `otp_challenges`, digest, estados y unicidad de desafío pendiente. |
| V10 | `propiedades`, `unidades`, `unidad_fotos`. |
| V11 | `contratos`, `contrato_archivos`, `cuotas`. |
| V12 | `cuentas_pago`, `pagos`, `pago_comprobantes`, `recibos`. |
| V13 | `notificaciones`. |
| V14 | `propiedades.portada_ref`. |
| V15 | `unidad_fotos.foto_ref`, URL nullable y check XOR de fuente. |

Hay 21 tablas de negocio/persistencia. Las migraciones usan nombres explícitos para PK/FK/UK/CHECK y tipos compatibles. `application.yml:19-28` configura `ddl-auto: validate`, Flyway con `validate-on-migrate=true`, sin baseline automático y `clean-disabled=true`.

### Relaciones

| Relación | Implementación |
|---|---|
| Usuario–Persona | `Usuario.persona` `@OneToOne(fetch=LAZY)` con FK única `usuarios.codper`. |
| Persona–Propiedad | `PropiedadEntity.propietaria` `@ManyToOne`. |
| Propiedad–Unidad | `UnidadEntity.propiedad` `@ManyToOne`. |
| Unidad–Foto | `UnidadFotoEntity.unidad` `@ManyToOne`. |
| Unidad–Contrato | `ContratoEntity.unidad` `@ManyToOne`; no colección inversa. |
| Contrato–Inquilino | `ContratoEntity.inquilino` `@ManyToOne` a `Persona`. |
| Contrato–origen | `ContratoEntity.contratoOrigen` `@ManyToOne` opcional autorreferente. |
| Contrato–Cuota | `CuotaEntity.contrato` `@ManyToOne`. |
| Contrato–Archivo | `ContratoArchivoEntity.contrato` `@ManyToOne`. |
| Cuota–Pago | `PagoEntity.cuota` `@ManyToOne`. |
| Pago–Cuenta | `PagoEntity.cuentaPago` `@ManyToOne` opcional. |
| Pago–Comprobante | `PagoComprobanteEntity.pago` `@ManyToOne`. |
| Pago–Recibo | `ReciboEntity.pago` `@ManyToOne` con unicidad en base. |
| Notificación–Usuario | `NotificacionEntity.destinatario` `@ManyToOne`. |
| Rol/Menú/Proceso | Relaciones mediante entidades de unión `RolUsu`, `RolMe`, `MePro`. |

No se encontraron `@OneToMany` ni `@ManyToMany` en producción. El diseño unidireccional evita recursividad de serialización y limita cargas implícitas. Las consultas de lectura importantes usan `@EntityGraph` y proyecciones; con `open-in-view=false` en `application.yml:22` se reduce el riesgo de lazy loading accidental en la capa web.

### Integridad y concurrencia

La base contiene checks para estados, fechas mensuales, montos no negativos, contratos vigentes, revocación de sesiones, fuentes de fotos y relaciones únicas. Los cambios críticos de propietario, sesiones, OTP, contratos, cuotas y pagos usan bloqueos pesimistas en los servicios o repositorios correspondientes.

No hay `@Version` en las entidades. Por ello, actualizaciones ordinarias de persona, propiedad, unidad, menú, proceso, cuenta o metadata pueden sufrir “last write wins” si dos requests concurrentes editan el mismo registro. Para transiciones críticas sí hay bloqueos y restricciones únicas, por lo que el riesgo es selectivo, no general.

No se observó un N+1 evidente en los flujos principales porque se usan `EntityGraph`, consultas agrupadas y proyecciones. Sí hay riesgos de volumen:

- `CuotaController.listByContrato` y `PagoController.listByCuota` devuelven listas sin paginación.
- `SessionServiceImpl.list` carga las sesiones activas del usuario completas y filtra expiradas en memoria.
- Consultas de pagos y recibos cargan grafos profundos mediante `EntityGraph`; deben medirse con datos de producción.
- El scheduler procesa todas las cuotas elegibles en una sola ejecución y no tiene paginación por lotes.

La ejecución de pruebas confirma coherencia entre entidades y esquema actual, pero no sustituye una prueba de carga ni una revisión de planes `EXPLAIN`.

## 10. Seguridad

### Controles implementados

Evidencia: `auth/config/SecurityConfig.java:32-94`, `auth/security/JwtAuthenticationFilter.java:38-73`, `auth/config/JwtProperties.java`, `auth/config/OtpProperties.java`.

- La aplicación es stateless (`SessionCreationPolicy.STATELESS`).
- Solo `POST /auth/login`, `refresh`, `otp/verify`, `otp/resend` y `OPTIONS` son públicos en la filter chain.
- El JWT usa HS256 y valida firma antes de claims; se valida `sub`, `iss`, `iat`, `exp` y `sid`.
- El `sid` debe corresponder a una sesión no revocada, no expirada y perteneciente al login del token.
- Los refresh tokens se almacenan como hash SHA-256 y se rotan; la reutilización revoca la sesión.
- Las contraseñas de usuarios se codifican mediante `BCryptPasswordEncoder` en `config/PasswordEncodingConfig.java`.
- OTP no se almacena en claro: se persiste digest HMAC y se limitan intentos, expiración, cooldown y reenvíos.
- CORS no permite `*`, usa una lista de orígenes y credenciales habilitadas.
- CSRF se aplica específicamente al refresh por cookie WEB; el token SPA se expone como cookie no HTTP-only y header.
- Los controllers de dominio tienen `@PreAuthorize`; la autorización por recurso evita que administrador gestione propietarios activos y protege al último propietario.
- DTOs y `toString()` de autenticación redacted no exponen contraseñas, refresh tokens ni hashes.

### Problemas de seguridad

1. No existe rate limiting por IP, login, dispositivo o cuenta para `POST /auth/login`, `otp/verify` y `otp/resend`. El OTP limita intentos por challenge, pero no impide abuso distribuido para generar correos, bloquear desafíos o provocar gasto/denegación de servicio. Los parámetros `ipAddress` y `userAgent` llegan como `null` desde `AuthServiceImpl`.
2. `security.cookie.secure` es `false` por defecto en `application.yml:54` y en el ejemplo de entorno. Si el despliegue olvida sobrescribirlo bajo HTTPS, el refresh cookie podría viajar por HTTP. El valor es aceptable para desarrollo local, pero no es un default seguro para producción y no hay perfil productivo que fuerce la configuración.
3. No se encontró política de rotación de `JWT_SECRET`/`OTP_HMAC_SECRET`, gestión externa de secretos, TLS del servidor ni headers de seguridad para el reverse proxy. La existencia de placeholders en `.env.example` es correcta; el `.env` local está ignorado y no se incluyeron sus valores en este informe.
4. `SecurityAccessDeniedException` se transforma en `ErrorCode.INVALID_REQUEST` en `GlobalExceptionHandler.java:97-102`, mientras `AccessDeniedException` usa `ACCESS_DENIED`. Esto puede confundir al cliente y a la monitorización, aunque ambos resulten en 403.

No se encontró exposición de `passwd`, `password_hash`, refresh token o hash BCrypt en DTOs de respuesta. La constante de hash dummy usada para igualar tiempos de login no se reproduce aquí por la regla de no divulgar hashes.

## 11. Validaciones

Hay `@Valid` en los bodies principales y `@Validated` en propiedades de configuración. Los servicios además validan existencia, ownership, estados, fechas, importes, cuenta activa, comprobantes y último propietario.

### Defecto concreto

`src/main/java/com/orman/backend/person/dto/CreatePersonaRequest.java:16` declara simultáneamente:

```java
@Max(1) @Pattern(regexp = "[01]") String estado
```

`@Max` es una restricción numérica aplicada a un `String`. El `@Pattern` sí expresa correctamente el contrato textual, pero la combinación puede producir `UnexpectedTypeException` al validar un valor no nulo y terminar en el handler genérico 500 en vez de 400. El caso no está cubierto por las pruebas actuales. Es un defecto de contrato y validación, no una suposición de diseño.

Otros puntos observados:

- Los estados se representan de forma mixta: `Short` en algunas requests y `String` en persona.
- Hay reglas de estado en DTO, servicio y checks SQL; la duplicación es útil como defensa, pero requiere mantener sincronizadas las tres capas.
- No todos los parámetros de conversión de Spring tienen handlers específicos en `GlobalExceptionHandler`; tipos de path/query inválidos pueden quedar sujetos al comportamiento por defecto o al handler genérico.
- Los montos usan `BigDecimal` y `@DecimalMin`; las migraciones usan `NUMERIC(14,2)`, una correspondencia adecuada en lo revisado.

## 12. Manejo de excepciones

`common/error/GlobalExceptionHandler.java:45-270` usa `ProblemDetail`, content type `application/problem+json`, `errorCode`, timestamp, instance, traceId y `fieldErrors` para validación. Hay handlers específicos para credenciales, JWT, refresh, sesión, autorización, último propietario, not found, conflicto, regla de negocio, multipart y JSON ilegible.

Aspectos correctos:

- No se devuelve stack trace, SQL, constraint interna ni secreto al cliente.
- Los errores de negocio se separan en 409 o 422.
- El error interno devuelve texto genérico y registra path, traceId y tipo de excepción.

Observaciones:

- No hay handler específico para `DataIntegrityViolationException`. La eliminación de persona en `PersonaServiceImpl.delete(...)` puede chocar con la FK `usuarios.codper` si la persona tiene usuario, y no traduce ese caso a 409; puede terminar en 500.
- No hay handler explícito para `HttpMediaTypeNotSupportedException`, conversiones de path/query o errores de binding distintos de `MethodArgumentNotValidException`/`ConstraintViolationException`.
- El `traceId` se genera por respuesta, pero no se observó un filtro que lo propague desde una petición entrante, MDC o header de respuesta para correlacionarlo con toda la traza.
- La captura genérica registra solo el tipo de excepción, lo que protege información pero dificulta diagnóstico operativo. Debe complementarse con logging estructurado seguro y correlación.

## 13. Transacciones

La mayoría de los services de escritura tienen `@Transactional`; las lecturas importantes usan `readOnly=true`. Se observan transacciones para CRUD, asignaciones, autenticación, sesiones, OTP, contratos, pagos y notificaciones.

Riesgos transaccionales reales:

1. `AuthServiceImpl.resendOtp(...)` prepara el reenvío en una transacción, envía SMTP y luego confirma el cambio en otra llamada transaccional (`prepareResend`/`confirmResend`). Dos requests concurrentes pueden preparar códigos diferentes antes de que cualquiera marque el envío; el código finalmente válido puede no corresponder al correo que el usuario recibió.
2. `PagoServiceImpl.confirm(...)` publica `PagoConfirmadoEvent` y el listener `PagoNotificacionEventHandler` invoca notificación de forma síncrona dentro del mismo flujo. Si la generación de notificación falla, la excepción puede hacer rollback de la confirmación del pago y del recibo. El pago queda funcionalmente acoplado a una bandeja secundaria.
3. Los servicios de archivos locales escriben filesystem y base de datos en una misma operación lógica. `PropiedadPortadaServiceImpl` y `UnidadFotoServiceImpl` tienen compensaciones más completas; `PersonaPhotoServiceImpl` elimina el archivo anterior después del commit, pero no registra de la misma forma la limpieza del nuevo archivo si la transacción se revierte después de escribirlo. Puede dejar huérfanos.
4. `PagoServiceImpl.create(...)` valida saldo confirmado sin bloquear la cuota durante la creación. Requests concurrentes pueden crear varios pagos pendientes cuya suma exceda el saldo; la confirmación posterior evita sobreconfirmar, pero deja pagos pendientes que no podrán confirmarse y requiere intervención/estado claro.

## 14. Lógica de negocio

### Reglas bien conectadas

- Estados de personas, usuarios, roles, menús, procesos, propiedades y unidades tienen acciones explícitas de activar/desactivar e idempotencia razonable.
- `OwnerProtectionServiceImpl` bloquea operaciones que dejarían al sistema sin propietario activo y tiene prueba de concurrencia.
- La asignación de roles/menús/procesos valida existencia, estado activo y duplicados con claves compuestas.
- Contratos validan periodo mensual, unidad operativa, inquilino activo y unicidad de contrato vigente por unidad.
- Confirmar contrato genera una cuota mensual por periodo, protegida por `uk_cuotas_codcon_periodo`.
- Pagos confirmados actualizan la cuota de forma atómica bajo bloqueo y crean un recibo único.

### Inconsistencia contractual-financiera

`ContratoServiceImpl.rescind(...)` (`contract/service/impl/ContratoServiceImpl.java:134-145`) solo cambia el contrato a `RESCINDIDO`, fecha y motivo. No cambia cuotas futuras a `ANULADA` ni las marca como no cobrables.

`PagoServiceImpl.validateCuotaCanReceivePayment(...)` (`payment/service/impl/PagoServiceImpl.java:173-177`) solo rechaza cuotas `ANULADA` o `PAGADA`; no consulta el estado del contrato. Por tanto, una cuota de un contrato rescindido puede seguir aceptando un pago pendiente.

`CuotaNotificacionScheduler.generateFor(...)` (`notification/service/impl/CuotaNotificacionScheduler.java:31-40`) consulta cuotas PENDIENTE/PARCIAL vencidas o próximas sin filtrar el estado del contrato. Puede seguir generando recordatorios después de una rescisión.

Esta cadena es el riesgo funcional más importante del modelo: el contrato dice que terminó, pero sus cuotas siguen habilitadas para cobro y notificación. Debe definirse explícitamente la política de cuotas ya vencidas, cuotas futuras, pagos pendientes y notificaciones al rescindir/finalizar.

### Otras reglas incompletas o de alcance

- `OrigenPago.MOVIL` existe en el modelo, pero `PagoMapper` genera siempre `OrigenPago.MANUAL` y `PagoRequest` no permite enviar origen. El flujo móvil de pago está **PARCIALMENTE IMPLEMENTADO**.
- No hay endpoints para que un inquilino consulte o registre pagos; el alcance actual es propietario.
- Renovación (`ContratoServiceImpl.renew`, líneas 122-129) solo rechaza origen BORRADOR y valida fechas, pero no verifica explícitamente continuidad, solapamiento o relación temporal con el contrato origen más allá de la FK.
- No hay eliminación de contratos, cuentas, propiedades, unidades, roles, menús o procesos. En los casos revisados parece una decisión de ciclo de vida por estado, pero no está siempre documentada como tal.

## 15. Rendimiento

### Fortalezas

- Las listas principales son paginadas y los controllers limitan el tamaño a 100 en varios módulos.
- Las relaciones son LAZY y unidireccionales.
- Se usan `@EntityGraph`, proyecciones y consultas agregadas para evitar cargas repetidas.
- Existen índices sobre FKs, estados, fechas, unicidades y búsquedas relevantes en migraciones.
- `open-in-view=false` evita consultas perezosas fuera del servicio.

### Riesgos

| Impacto | Evidencia | Riesgo |
|---|---|---|
| Alto a medio | `PagoServiceImpl.create`, líneas 54-67 | La validación de saldo no bloquea cuota al crear; concurrencia puede producir pendientes inviables. |
| Medio | `CuotaController.listByContrato` y `PagoController.listByCuota` | Listas no paginadas crecen con contratos/pagos. |
| Medio | `CuotaNotificacionScheduler.generateFor` | Procesa todo el conjunto elegible en una transacción y sin lotes. |
| Medio | `Pageable` aceptado directamente en múltiples controllers | El cliente puede pedir sorts arbitrarios; una propiedad inválida puede producir error y el contrato de sort no está documentado/whitelisteado. |
| Bajo/medio | `ProcesoRepository.findAllByOrderByNombreAsc(Pageable)` | El repositorio fuerza nombre ascendente aunque el endpoint reciba otro sort. |

No se ejecutó prueba de carga ni `EXPLAIN ANALYZE`; su resultado queda **NO VERIFICADO**.

## 16. Calidad y mantenibilidad

Fortalezas:

- Nombres de dominio claros y módulos coherentes.
- Inyección por constructor predominante.
- Interfaces de servicio presentes en los módulos de negocio.
- Mappers explícitos.
- Entidades sin `@Data` indiscriminado y sin relaciones dentro de `equals`/`hashCode`/`toString`.
- No se encontró `System.out.println`, `TODO`, `FIXME` o `HACK` en producción.

Deuda observada:

- `MenuServiceImpl`, `MeProServiceImpl`, `ProcesoServiceImpl` y partes de `RolMeServiceImpl` están comprimidos en métodos de una línea, lo que dificulta depuración, revisión y futuros cambios.
- Varios servicios convierten cualquier `DataIntegrityViolationException` en “duplicado”, pudiendo ocultar un FK, CHECK u otra causa real. Esto ocurre, por ejemplo, en asignaciones y alta de roles/menús.
- Hay repetición de ownership y normalización entre property, contract y payment. Actualmente es comprensible, pero puede divergir si cada módulo evoluciona sus reglas de acceso.
- Algunas entidades usan `getClass()`/`instanceof` para igualdad en vez de una estrategia uniforme con proxies Hibernate; no se observó relación incluida en igualdad, por lo que el impacto actual es bajo.
- `shared` está vacío mientras lo transversal vive en `common` y `config`; no es un defecto funcional, pero la estructura puede inducir a crear duplicados.

## 17. Tests

Conteo estático: 71 archivos de prueba y 337 anotaciones `@Test`; la ejecución Maven actual informó 351 pruebas porque incluye casos parametrizados/infraestructura adicional.

| Tipo | Cantidad observada |
|---|---:|
| `@SpringBootTest` | 26 archivos |
| `@WebMvcTest` | 13 archivos |
| `@DataJpaTest` | 0 |
| Testcontainers | 0 |
| H2 | 0 |
| `@Disabled` | 0 |
| Resultado de `clean test` | 351/351 exitosas |

La suite cubre autenticación, OTP, JWT, sesiones, matriz de autorización, último propietario, usuarios, personas, roles, menús, procesos y varios flujos de integración del modelo inmobiliario/pagos/notificaciones. Las pruebas de integración usan PostgreSQL real, coherente con `AGENTS.md`.

No se puede declarar porcentaje de cobertura: no existe JaCoCo ni otra herramienta que lo calcule.

Tests prioritarios pendientes:

- rescisión/finalización y comportamiento de cuotas, pagos y notificaciones posteriores;
- carrera concurrente en creación de pagos pendientes y reenvío OTP;
- validación de `CreatePersonaRequest.estado` con valor no nulo;
- eliminación de persona con usuario y contrato esperado de error 409;
- sort inválido, parámetros de path inválidos y errores 400/500;
- rollback de filesystem cuando falla la transacción;
- scheduler con una zona distinta de `America/La_Paz`;
- SMTP real o contrato de integración con proveedor simulado de manera operativa;
- prueba de despliegue con configuración segura de producción y refresh cookie `Secure`.

Advertencias no bloqueantes observadas durante Maven: agente inline de Mockito/Byte Buddy dinámico, APIs deprecadas en una prueba de autorización y warnings de annotation processing/unchecked operations. No causaron fallos, pero conviene eliminarlos antes de actualizar el JDK o endurecer la build.

## 18. Configuración y despliegue

Evidencia: `src/main/resources/application.yml`, `src/test/resources/application.yml`, `.env.example`, `.gitignore`, `pom.xml`.

Configuración correcta:

- Puerto 9090.
- PostgreSQL parametrizado por `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`.
- Credenciales JWT/OTP y correo obligatorias mediante variables de entorno.
- Import opcional de `.env` local, ignorado por Git.
- límites multipart de 20 MB por archivo y 21 MB por request.
- Flyway valida migraciones y clean está deshabilitado.
- rutas de storage externalizables por variables.
- scheduler parametrizable por cron y zona.

Riesgos operativos:

- `REFRESH_COOKIE_SECURE` por defecto `false`.
- No hay `application-prod.yml` ni validación que impida arrancar producción con defaults de desarrollo, especialmente storage local y cookie no segura.
- No hay Dockerfile, compose, scripts de despliegue, CI/CD, health checks, Actuator, configuración de pool, métricas, tracing, rotación de logs o documentación de rollback.
- Las fotos de persona, propiedad y unidad se guardan en `./storage` o una ruta local. En contenedores efímeros, múltiples instancias o despliegues sin volumen persistente pueden perderse o quedar divergentes de la base.
- No se verificó backup/restore de PostgreSQL, migración en un entorno limpio, TLS, reverse proxy ni gestión de secretos: **NO VERIFICADO**.
- `spring-boot-devtools` está en runtime opcional; debe confirmarse que nunca se incluya en el artefacto productivo final.

## 19. Código incompleto, TODO y FIXME

No se encontraron tokens `TODO`, `FIXME` o `HACK` en el código de producción.

Código/alcance pendiente identificado por estructura y documentación:

| Elemento | Estado real |
|---|---|
| `dashboard` | Directorio vacío; **NO IMPLEMENTADO**. |
| `maintenance` | Directorio vacío; **NO IMPLEMENTADO**. |
| `report` | Directorio vacío; **NO IMPLEMENTADO**. |
| `config/openapi` y documentación OpenAPI | **NO IMPLEMENTADO**; la Fase 18 sigue marcada pendiente. |
| Reportes, métricas y dashboard inmobiliario | **NO IMPLEMENTADOS**. |
| Inquilinos como actores HTTP | **NO IMPLEMENTADO**. |
| Origen real `MOVIL` en pagos | **PARCIALMENTE IMPLEMENTADO**: enum existe, flujo no lo produce. |
| Archivos binarios de contratos/comprobantes | **NO IMPLEMENTADO**; solo URL externa. |
| Canales externos de notificación | **NO IMPLEMENTADO**; existe correo OTP, pero notificaciones son internas. |
| Preparación productiva | Pendiente según Fase 21; no hay artefactos de despliegue. |

## 20. Problemas encontrados

Esta sección resume los problemas que se detallan y clasifican en las secciones siguientes.

1. Rescisión de contrato no invalida cuotas y el pago/scheduler no consideran estado contractual.
2. No hay rate limiting de login/OTP ni contexto IP/User-Agent persistido.
3. Eventos de notificación síncronos pueden hacer rollback de pagos confirmados.
4. Cookie refresh `Secure=false` por defecto sin perfil productivo seguro.
5. `@Max` numérico aplicado a `String estado` en creación de persona.
6. FK al eliminar persona puede regresar 500 en vez de conflicto controlado.
7. Reenvío OTP se divide alrededor de SMTP y tiene carrera de códigos.
8. Scheduler declara zona configurable pero calcula fecha con zona fija.
9. Timestamps financieros usan hora del sistema mientras otros módulos usan UTC.
10. Escritura de fotos de persona puede dejar archivos huérfanos tras rollback; storage local no tiene estrategia de persistencia/reconciliación.
11. Creación concurrente de pagos no bloquea cuota al validar saldo.
12. Listas de cuotas/pagos por relación no tienen paginación.
13. `Pageable.sort` no está whitelisteado y el handler de errores de conversión es incompleto.
14. No hay `@Version` para ediciones ordinarias concurrentes.
15. README y documentación de integración no reflejan completamente V10–V15 y módulos actuales.

## 21. Riesgos

### Riesgo de datos y negocio

El principal riesgo es la divergencia entre estado contractual y estado cobrable: rescindir no cierra automáticamente cuotas ni detiene recordatorios, y el endpoint de pagos no verifica el contrato. Puede generar saldos, pagos pendientes e indicadores incorrectos.

### Riesgo de seguridad

La ausencia de rate limiting permite abuso del login y OTP. La cookie insegura por defecto puede exponer refresh tokens si se despliega sin override. No hay evidencia de una barrera de configuración que impida esos defaults en producción.

### Riesgo transaccional

La confirmación de pago depende de que una notificación secundaria se inserte correctamente. Un fallo de notificación puede afectar una operación financiera primaria.

### Riesgo operacional

Las imágenes dependen del filesystem local. Sin volumen persistente o almacenamiento externo, un redeploy o escalamiento puede perder archivos o servir resultados distintos por instancia. La falta de health checks, métricas, backups documentados y CI reduce la capacidad de operar y recuperar el sistema.

### Riesgo de mantenimiento

La documentación desactualizada, métodos comprimidos y traducción genérica de integridad dificultan diagnosticar y evolucionar el backend, aunque la suite actual esté verde.

## 22. Deuda técnica

- Contratos de estados y transición aún no están centralizados entre contrato, cuota, pago y notificación.
- Tiempo/fecha no está completamente normalizado: algunos servicios usan `Clock` UTC y pagos usan `LocalDateTime.now()` del sistema.
- La política de paginación y sort no es uniforme.
- Falta estrategia de auditoría de cambios funcionales; el directorio `config/audit` está vacío y no se encontraron componentes de auditoría de negocio.
- Falta observabilidad y correlación distribuible.
- Falta estrategia de storage productivo y limpieza/reconciliación de huérfanos.
- Falta automatización de calidad de build, cobertura y seguridad de dependencias.
- Falta OpenAPI como contrato formal.
- Falta documentación sincronizada con la evolución real del esquema.

## 23. Hallazgos críticos

**Ninguno (0).**

No se observó un hallazgo que, con la evidencia disponible, implique por sí mismo pérdida inmediata de datos, exposición de un secreto real o imposibilidad de arrancar/compilar el backend.

## 24. Hallazgos altos

### ALTO-01 — Rescisión deja cuotas cobrables y notificables

**Evidencia:** `contract/service/impl/ContratoServiceImpl.java:134-145`, `payment/service/impl/PagoServiceImpl.java:173-177`, `notification/service/impl/CuotaNotificacionScheduler.java:31-40`.

`rescind(...)` solo cambia el contrato. La validación de pago acepta cualquier cuota no ANULADA/PAGADA y el scheduler consulta cuotas sin revisar contrato. Consecuencia: un contrato RESCINDIDO puede seguir recibiendo pagos pendientes y recordatorios.

**Por qué es alto:** afecta saldos, cobranza y confianza en el estado financiero. Debe definirse y aplicar una transición de cuotas/pagos/notificaciones.

### ALTO-02 — Sin rate limiting de autenticación y OTP

**Evidencia:** `auth/controller/AuthController.java:55-83`, `auth/service/impl/AuthServiceImpl.java:72-95`, `auth/service/impl/OtpChallengeServiceImpl.java`, configuración OTP en `application.yml:59-64`.

Hay límites por challenge, pero no por IP, login, dispositivo o ventana global. Los datos IP/User-Agent se pasan como `null`. Login, resend y verify son públicos en la filter chain.

**Por qué es alto:** permite abuso de SMTP, denegación de servicio contra OTP y ataques de credenciales a escala si la API se expone públicamente.

### ALTO-03 — Notificación síncrona puede revertir operación financiera

**Evidencia:** `payment/service/impl/PagoServiceImpl.java:110-121` publica evento durante la confirmación; `notification/service/impl/PagoNotificacionEventHandler.java:17-30` usa `@EventListener` síncrono.

La confirmación del pago, cambio de cuota y recibo quedan en el mismo flujo de persistencia que la inserción de notificación. Un fallo de generación puede propagar excepción y hacer rollback de la operación primaria.

**Por qué es alto:** disponibilidad y consistencia de una operación financiera quedan condicionadas a una bandeja secundaria.

### ALTO-04 — Refresh cookie no segura por defecto en configuración compartida

**Evidencia:** `src/main/resources/application.yml:52-55`, `.env.example` y `auth/config/SecurityConfig.java:43-52`.

`secure` usa default `false`. No existe perfil productivo ni validación de entorno que lo haga obligatorio en despliegues HTTPS.

**Por qué es alto:** un despliegue real con la configuración por defecto puede transmitir el refresh cookie por HTTP y comprometer la sesión.

## 25. Hallazgos medios

### MEDIO-01 — Restricción `@Max` incompatible con `String`

**Evidencia:** `person/dto/CreatePersonaRequest.java:16`.

`@Max(1)` se aplica a `String estado`; el contrato ya tiene `@Pattern`. Un valor no nulo puede provocar error de tipo de validación y no la respuesta 400 prevista.

### MEDIO-02 — Eliminación de persona no traduce FK a 409

**Evidencia:** `person/controller/PersonaController.java:89-93`, `person/service/impl/PersonaServiceImpl.java` y FK `fk_usuarios_personas` en `V3__create_usuarios_table.sql`.

El endpoint puede intentar borrar una persona con usuario asociado. No hay handler global de `DataIntegrityViolationException`; el resultado probable es 500, no un conflicto de dominio controlado.

### MEDIO-03 — Carrera en reenvío OTP alrededor de SMTP

**Evidencia:** `auth/service/impl/AuthServiceImpl.java:123-140` y `auth/service/impl/OtpChallengeServiceImpl.java:70-108`.

`prepareResend` y `confirmResend` están separados por una operación externa. Requests concurrentes pueden enviar códigos distintos y confirmar en orden diferente.

### MEDIO-04 — Zona horaria configurada no se usa al calcular fecha

**Evidencia:** `notification/service/impl/CuotaNotificacionScheduler.java:19-29`.

El cron usa `${notification.scheduler.zone}`, pero `generateDaily()` usa constante fija `America/La_Paz`. Cambiar la propiedad no cambia la fecha de negocio.

### MEDIO-05 — Timestamps inconsistentes

**Evidencia:** `payment/service/impl/PagoServiceImpl.java:111,130,143`, `payment/mapper/PagoMapper.java:24`, `payment/mapper/ReciboMapper.java:15` usan `LocalDateTime.now()`; contratos/notificaciones usan UTC o `Clock`.

Si el servidor no está en UTC, las fechas financieras pueden diferir de confirmación, sesiones y notificaciones.

### MEDIO-06 — Filesystem y transacción de persona pueden divergir

**Evidencia:** `person/service/impl/PersonaPhotoServiceImpl.java` y propiedades `storage.personas` en `application.yml:66-70`.

Se escribe un archivo antes de completar todo el commit. La compensación de rollback no es tan completa como en fotos de propiedad/unidad. Además el storage local no tiene reconciliación ni estrategia multiinstancia.

### MEDIO-07 — Creación de pagos no bloquea cuota para validar saldo

**Evidencia:** `payment/service/impl/PagoServiceImpl.java:54-67,166-171`; el bloqueo se obtiene en confirmación, no en creación.

Concurrentemente pueden registrarse pagos pendientes cuya suma exceda el importe, aunque la confirmación posterior rechace el exceso.

### MEDIO-08 — Listas de cuotas y pagos sin paginación

**Evidencia:** `contract/controller/CuotaController.java:22-23`, `payment/controller/PagoController.java:50-52` y repositorios correspondientes.

La respuesta puede crecer linealmente con la vida del contrato y las operaciones de pago.

### MEDIO-09 — Sort de `Pageable` abierto y manejo incompleto de conversiones

**Evidencia:** múltiples controllers aceptan `Pageable` directamente, por ejemplo `person/controller/PersonaController.java:64-71`; no existe whitelist transversal; `GlobalExceptionHandler` no maneja todas las excepciones de binding/conversión.

Un sort inválido puede generar error de ejecución y el contrato de campos ordenables no está controlado de forma uniforme.

### MEDIO-10 — Sin preparación operacional suficiente para producción

**Evidencia:** ausencia de Docker/CI/Actuator/perfil productivo; `application.yml:66-78` usa filesystem local por defecto; no hay documentación de backup/restore.

No impide las pruebas actuales, pero sí dificulta despliegue repetible, observabilidad, escalamiento y recuperación.

## 26. Hallazgos bajos

### BAJO-01 — Sort de procesos puede ser ignorado

**Evidencia:** `process/repository/ProcesoRepository.java:18` usa `findAllByOrderByNombreAsc(Pageable)`; el controller permite `Pageable`.

El endpoint parece aceptar sort, pero el método derivado fuerza nombre ascendente.

### BAJO-02 — Métodos excesivamente comprimidos

**Evidencia:** `menu/service/impl/MenuServiceImpl.java`, `menu/service/impl/MeProServiceImpl.java`, `process/service/impl/ProcesoServiceImpl.java`.

Varias operaciones están en una sola línea, con menor legibilidad y mayor coste de revisión.

### BAJO-03 — Integridad traducida de forma demasiado genérica

**Evidencia:** catches de `DataIntegrityViolationException` en servicios de roles/menús/asignaciones.

Algunas causas distintas de duplicado pueden terminar con mensajes engañosos. Es deuda diagnóstica y de contrato, no evidencia de corrupción actual.

### BAJO-04 — Documentación desactualizada frente al código

**Evidencia:** `README.md:9` aún indica fases antiguas y que la Fase 12.3 no inició; el código y migraciones actuales incluyen V9–V15, propiedad, pagos y notificaciones. `docs/informe-integracion-backend-angular.md` también conserva pendientes que ya no coinciden completamente.

Esto puede llevar a integrar el frontend contra un estado incorrecto.

### BAJO-05 — Estructura vacía y transversal no uniforme

**Evidencia:** `dashboard`, `maintenance`, `report`, `shared` y varios subdirectorios de `config` vacíos.

No es un fallo funcional, pero crea señales de arquitectura futura no implementada y puede inducir abstracciones o duplicados.

### BAJO-06 — Sin cobertura, SBOM ni escaneo automatizado en Maven

**Evidencia:** `pom.xml:105-120` solo configura `spring-boot-maven-plugin`; no hay JaCoCo, OWASP dependency-check, SBOM ni quality gate.

La suite verde no cuantifica cobertura ni riesgo de dependencias transitivas.

### BAJO-07 — Estrategia de igualdad de entidades no completamente uniforme

**Evidencia:** algunas entidades usan `Hibernate.getClass`, otras `getClass()`/`instanceof` en `equals`/`hashCode`.

No se observó relación incluida en igualdad y las pruebas no fallan, por lo que el impacto actual es bajo; conviene uniformar antes de introducir colecciones o caching de entidades.

## 27. Funcionalidades pendientes

Estas ausencias no deben confundirse con fallos de lo que sí está implementado:

- Dashboard y KPIs avanzados: **NO IMPLEMENTADO**.
- Mantenimiento/operaciones: **NO IMPLEMENTADO**.
- Reportes: **NO IMPLEMENTADO**.
- OpenAPI/Swagger: **NO IMPLEMENTADO**; la Fase 18 está pendiente en `docs/PLAN_GENERAL.md:205-207`.
- Pruebas de integración ampliadas y auditoría técnica formal: pendientes en el plan, esta auditoría cubre la parte solicitada.
- Preparación productiva: **NO COMPLETA**.
- Actor inquilino y sus permisos: **NO IMPLEMENTADO**.
- Pagos de origen móvil: **PARCIALMENTE IMPLEMENTADO**; existe enum, no flujo que lo genere.
- Archivos binarios de contratos y comprobantes: **NO IMPLEMENTADO**; actualmente se almacenan URLs.
- Canales externos para las notificaciones internas: **NO IMPLEMENTADO**.
- Auditoría de negocio de cambios: **NO IMPLEMENTADO**; no hay listeners/aspectos/tablas de auditoría visibles.

## 28. Recomendaciones

1. Definir una máquina de estados contractual-financiera: qué pasa con cuotas futuras, cuotas vencidas, pagos pendientes, comprobantes, recibos y notificaciones al rescindir o finalizar.
2. Hacer que pago y scheduler consulten/reciban una regla común de elegibilidad basada en estado del contrato.
3. Añadir rate limiting y protección de abuso para login/OTP por IP, login, dispositivo y ventana temporal; conservar únicamente datos necesarios y seguros.
4. Forzar `Secure=true` en configuración de producción y fail-fast si el entorno productivo intenta usar cookie no segura.
5. Separar eventos secundarios del commit financiero mediante eventos after-commit/outbox o una estrategia de reintento que no haga rollback del pago.
6. Corregir el contrato de validación de persona y agregar prueba MVC para estados nulos, válidos e inválidos.
7. Mapear integridad referencial a conflictos de dominio y probar específicamente el DELETE de persona con usuario vinculado.
8. Unificar el uso de `Clock` y UTC en pagos, recibos y notificaciones.
9. Diseñar la estrategia de almacenamiento productivo: volumen persistente, objeto externo o política clara de reconciliación/limpieza.
10. Bloquear cuota también en la creación de pago o introducir una reserva de saldo explícita.
11. Paginar cuotas/pagos por relación y procesar scheduler en lotes.
12. Whitelistear campos de sort y centralizar errores de binding/conversión.
13. Añadir `@Version` donde se requiera control de edición concurrente y documentar dónde los bloqueos pesimistas son la decisión intencional.
14. Sincronizar README, integración, changelog y plan con V10–V15 y el estado actual.
15. Antes de Fase 21, incorporar perfil productivo, health checks, métricas, logs estructurados, tracing, CI, escaneo de dependencias, SBOM, backup/restore y runbook.

## 29. Orden recomendado de corrección

### Bloque 1 — Antes de continuar el desarrollo funcional

1. Resolver ALTO-01: contrato/cuotas/pagos/notificaciones.
2. Resolver ALTO-02: rate limiting y abuso de autenticación/OTP.
3. Resolver ALTO-03: desacoplar notificaciones de la transacción financiera.
4. Resolver ALTO-04: defaults y guardas de producción.

### Bloque 2 — Correcciones funcionales y de consistencia

5. Corregir validación de persona y mapeo de errores de integridad.
6. Unificar UTC/Clock.
7. Resolver carrera OTP y concurrencia de pagos.
8. Paginación y sort seguro.
9. Pruebas de regresión de contratos rescindidos, pagos, scheduler, rollback de archivos y errores HTTP.

### Bloque 3 — Operación y calidad

10. Definir storage persistente y recuperación.
11. Añadir observabilidad, health checks, CI, cobertura y análisis de dependencias.
12. Crear OpenAPI y sincronizar documentación.
13. Preparar Docker/entorno productivo, backup/restore y runbook.

### Bloque 4 — Expansión de alcance

14. Implementar inquilinos, origen móvil real, canales de notificación, dashboard, reportes y mantenimiento solo con requisitos autorizados.

## 30. Conclusión sobre el estado actual

El backend actual está **implementado y compilable**, con una arquitectura modular razonable, seguridad base bien conectada, persistencia PostgreSQL/Flyway coherente y una suite de 351 pruebas completamente verde en el entorno auditado.

Su estado correcto es **“funcional para continuar con correcciones prioritarias”**, no “listo para producción”. Las cuatro prioridades altas deben resolverse antes de ampliar significativamente el desarrollo: consistencia después de rescisión contractual, protección contra abuso de autenticación/OTP, aislamiento transaccional de notificaciones y cookie de refresh segura por entorno.

La auditoría no encontró hallazgos críticos, pero sí una cadena de riesgo alto en el dominio financiero y carencias operativas importantes. Las funcionalidades ausentes —dashboard, reportes, mantenimiento, OpenAPI, inquilinos y preparación productiva— están identificadas como **NO IMPLEMENTADAS** y no deben inferirse a partir de la existencia de directorios o documentación de fases futuras.

El repositorio queda sin cambios de código. El único archivo creado por esta tarea es `docs/INFORME_AUDITORIA_BACKEND.md`.
