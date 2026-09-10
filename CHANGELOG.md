# Registro de cambios

Este archivo registra cambios relevantes de ORMAN-BACKEND por fase, con una estructura inspirada en Keep a Changelog.

## Sin publicar

## Etapa 4.4 — Notificaciones internas — 2026-09-10

### Agregado

- Migración Flyway V13 para `notificaciones`, con FK restrictiva a
  `usuarios.login`, tipos y referencias controlados, fecha de lectura,
  clave única antiduplicados e índices de consulta.
- Módulo `notification` con DTO de respuesta, mapper, repositorio, servicios,
  controladores REST, listado paginado propio, detalle, resumen de no leídas y
  marcado idempotente como leída.
- Scheduler diario configurado en `America/La_Paz` para cuotas `PENDIENTE` y
  `PARCIAL`, más recordatorio manual protegido por propiedad efectiva.
- Eventos internos de comprobante recibido, pago confirmado y pago rechazado;
  la confirmación genera la notificación después del recibo.
- Pruebas de mapper, MVC, persistencia PostgreSQL e integración, y guía
  Postman de Notificaciones. Las pruebas que enumeran Flyway reconocen V13.

### Preservado

- JWT, `SecurityConfig`, Roles, Menús y Procesos no fueron modificados.
- No se incorporaron correo, WhatsApp, SMS, Firebase Push, aplicación móvil,
  usuarios inquilinos, administradores ni integraciones externas.

### Verificación

- Pruebas específicas: 8 pruebas, 0 fallos, 0 errores y 0 omitidas.
- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 290 pruebas, 0 fallos,
  0 errores y 0 omitidas. PostgreSQL real validó Flyway V1–V13 y Hibernate
  con `ddl-auto=validate`.

## Etapa 4.3 — Pagos, comprobantes, recibos y cuentas de pago — 2026-09-10

### Agregado

- Migración Flyway V12 para `cuentas_pago`, `pagos`, `pago_comprobantes` y
  `recibos`, con FKs restrictivas, checks de estado/método, importes `NUMERIC`,
  idempotencia, órdenes únicas y un recibo único por pago.
- Módulo `payment` con entidades JPA, DTO separados, mappers explícitos,
  repositorios, servicios transaccionales y controladores REST.
- Pagos pendientes de revisión, confirmación atómica con bloqueo de cuota,
  pagos parciales calculados, rechazo, anulación, comprobantes por URL, recibos
  automáticos y cuentas de pago administradas por propietaria.
- Pruebas de mapper, MVC, persistencia PostgreSQL, integración, autorización y
  guía Postman del módulo; las verificaciones globales de Flyway se actualizaron
  para reconocer V12.

### Preservado

- JWT, `SecurityConfig`, Roles, Menús y Procesos no fueron modificados.
- No se implementaron notificaciones, aplicación móvil, usuarios inquilinos,
  pasarela/API bancaria ni conciliación automática.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 282 pruebas, 0 fallos, 0 errores
  y 0 omitidas. Flyway validó V1–V12 e Hibernate mantuvo `ddl-auto=validate`.

## Etapa 4.2 — Contratos y Cuotas — 2026-09-10

### Agregado

- Migración Flyway V11 para `contratos`, `contrato_archivos` y `cuotas`, con
  claves, FKs restrictivas, checks de estados y fechas mensuales, importes
  `NUMERIC`, cuotas únicas por período y un contrato vigente por Unidad.
- Módulo `contract` con entidades JPA, DTO separados, mappers explícitos,
  repositorios, servicios transaccionales y controladores REST.
- Borradores, confirmación, cuotas mensuales `PENDIENTE`, finalización,
  renovación con `codcon_origen`, rescisión básica y archivos por URL.
- Pruebas de mapper, MVC, persistencia, integración PostgreSQL y autorización
  por propiedad, junto con la guía Postman del módulo.

### Preservado

- JWT, `SecurityConfig`, Roles, Menús y Procesos no fueron modificados.
- No se implementaron pagos, comprobantes, recibos, cuentas de pago ni
  notificaciones. La rescisión no evalúa ni actualiza pagos o cuotas.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 272 pruebas, 0 fallos, 0 errores
  y 0 omitidas. PostgreSQL validó Flyway V1–V11 y Hibernate
  `ddl-auto=validate`.

## Etapa 4.1 — Propiedades, Unidades y Fotografías — 2026-09-10

### Agregado

- Migración Flyway V10 para `propiedades`, `unidades` y `unidad_fotos`, con
  claves, FKs restrictivas, checks, importes `NUMERIC`, índices y garantía de
  una sola portada por unidad.
- Módulo `property` con entidades JPA, DTO separados, mappers explícitos,
  repositorios, servicios transaccionales y controladores REST para las tres
  entidades del alcance.
- Autorización de dominio para `ROLE_PROPIETARIO`: cada operación verifica la
  cadena Usuario autenticado -> Persona asociada -> Propiedad propietaria.
- Pruebas de mapper, servicio, MVC, persistencia e integración con PostgreSQL,
  y guía Postman del módulo.

### Preservado

- JWT, `SecurityConfig`, Roles, Menús y Procesos no fueron modificados.
- No se implementaron contratos, cuotas, pagos, recibos ni notificaciones.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 262 pruebas, 0 fallos, 0 errores
  y 0 omitidas. PostgreSQL validó Flyway V1–V10 y Hibernate
  `ddl-auto=validate`.

## Gestión remota de Menús — 2026-09-08

### Agregado

- Filtros remotos opcionales `q` y `estado` en `GET /api/v1/menus`, combinables
  con `AND`, paginados y ordenados directamente por PostgreSQL.
- `GET /api/v1/menus/resumen` y `MenuResumenResponse` con `totalMenus`,
  `activos` e `inactivos`, calculados mediante conteos de BD.
- Pruebas MVC, unitarias, de integración PostgreSQL y de autorización para
  filtros, paginación, resumen y `400 VALIDATION_ERROR` ante `estado` inválido.

### Preservado

- POST/GET por ID/PUT/PATCH existentes, autenticación/JWT, relaciones
  Rol–Menú y Menú–Proceso, permisos y ausencia de DELETE de Menús no
  cambiaron.

### Verificación

- `.\mvnw.cmd clean test`: **BUILD SUCCESS**; 244 pruebas, 0 fallos, 0 errores
  y 0 omitidas. PostgreSQL validó Flyway V1–V9 y Hibernate `ddl-auto=validate`.
- `.\mvnw.cmd package -DskipTests`: **BUILD SUCCESS**; JAR empaquetado.

## Gestión remota de Roles — 2026-09-05

### Agregado

- Filtros remotos opcionales `q` y `estado` en `GET /api/v1/roles`, combinables
  con `AND`, paginados y ordenados directamente por PostgreSQL.
- `GET /api/v1/roles/resumen` y `RolResumenResponse` con `totalRoles`,
  `activos` e `inactivos`, calculados mediante conteos de BD.
- Pruebas MVC, unitarias, integración PostgreSQL y de autorización para filtros,
  paginación, resumen y `400 VALIDATION_ERROR` ante `estado` inválido.

### Preservado

- POST/GET por ID/PUT/PATCH existentes, protección del Rol PROPIETARIO,
  autenticación/JWT, Usuario–Rol, Rol–Menú, permisos y ausencia de DELETE de
  Roles no cambiaron.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 237 pruebas, 0 fallos, 0 errores
  y 0 omitidas. PostgreSQL validó Flyway V1–V9 y Hibernate `ddl-auto=validate`.
- `./mvnw.cmd package -DskipTests`: **BUILD SUCCESS**; JAR empaquetado.
- `git diff --check`: sin errores de espacios.

## Resumen global de Personas — 2026-08-17

### Agregado

- `GET /api/v1/personas/resumen` con `totalPersonas`, `activas`, `inactivas` y
  `conUsuario` globales, independiente de filtros y paginación.
- DTO `PersonaResumenResponse` y consulta agregada PostgreSQL con `COUNT`,
  `FILTER` y `LEFT JOIN`, sin cargar Personas ni ejecutar N+1.
- Pruebas MVC, unitarias, integración PostgreSQL y autorización para la ruta
  dedicada, incluyendo Usuarios vinculados activos e inactivos.

### Seguridad y preservación

- La ruta requiere `PROPIETARIO` o `ADMINISTRADOR`, igual que el listado actual.
- `conUsuario` cuenta Personas con Usuario vinculado, independientemente del
  estado del Usuario.
- `GET /api/v1/personas`, sus filtros, paginación y `PersonaResponse` no fueron
  modificados. No se agregaron migraciones.

### Verificación

- `.\mvnw.cmd clean test`: **BUILD SUCCESS**; 232 pruebas, 0 fallos, 0 errores
  y 0 omitidas. PostgreSQL 17.6 validó Flyway V1–V9 y Hibernate
  `ddl-auto=validate`.
- `.\mvnw.cmd package -DskipTests`: **BUILD SUCCESS**; JAR empaquetado.

## Contrato Personas para Angular — 2026-08-14

### Agregado

- Filtros de Personas (`q`, `tipoPersona`, `estado`), resumen no sensible de Usuario, capacidades de UI y fotografía local JPEG/PNG.
- Consultas por lote para evitar N+1 en la página de Personas.

### Preservado

- APIs de Usuario, BCrypt, JWT y matriz de autorización. DELETE Persona continúa siendo físico; PATCH de estado continúa siendo lógico.

## Fase 12.3 — contexto del usuario autenticado

### Agregado

- `GET /api/v1/auth/context` para reconstruir el contexto vigente de Usuario, Persona, Roles, Menús y Procesos desde el principal autenticado.
- DTOs y mapper específicos de contexto; consulta de Persona junto a Usuario y proyección JPQL única de navegación activa, sin HTTP interno ni N+1 evidente.
- Pruebas unitarias y de integración PostgreSQL para identidad propia, Usuario normal, ADMINISTRADOR, PROPIETARIO, foto, filtros de estado, asociaciones eliminadas y Menú compartido.

### Seguridad

- El endpoint requiere solo autenticación y no acepta identificadores de Usuario para seleccionar el contexto.
- JWT, sesiones, OTP, refresh, authorities, `@PreAuthorize` administrativo y APIs existentes no cambiaron; Menú y Proceso siguen fuera de `GrantedAuthority`.
- `foto` conserva la referencia textual opcional existente; no se exponen secretos ni datos personales administrativos.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 218 pruebas, 0 fallos, 0 errores y 0 omitidas. PostgreSQL validó Flyway V1–V9 y Hibernate `ddl-auto=validate`.

## Corrección técnica — CSRF de refresh WEB para Angular

### Modificado

- Spring Security usa su configuración SPA de CSRF: el refresh WEB mantiene
  `CookieCsrfTokenRepository`, exige CSRF solamente cuando recibe la cookie
  `orman_refresh` y acepta el valor crudo de `XSRF-TOKEN` reenviado por Angular
  en `X-XSRF-TOKEN`.
- La prueba HTTP de refresh WEB reproduce cookie cruda → header crudo, sin
  modificar el controller, la cookie refresh HttpOnly ni la rotación.

### Seguridad

- CSRF permanece activo: ausencia o valor incorrecto de `X-XSRF-TOKEN` devuelve
  `403 INVALID_REQUEST`; Spring conserva el enmascaramiento XOR del token
  expuesto como atributo para mitigación BREACH.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 214 pruebas, 0 fallos, 0 errores
  y 0 omitidas. PostgreSQL 17.6, Flyway V1–V9 y Hibernate con
  `ddl-auto=validate` se validaron correctamente.

## Fase 13.4 — correo, reenvío y cierre OTP

### Agregado

- Envío SMTP síncrono de OTP mediante Spring Mail, configuración exclusivamente
  externa y respuesta segura `OTP_DELIVERY_FAILED` ante fallos de entrega.
- Reenvío preautenticado con cooldown, máximo de reenvíos y confirmación del
  nuevo OTP solamente después de una entrega SMTP exitosa.

### Seguridad

- Un fallo del primer envío cancela el challenge; un fallo de reenvío conserva
  el OTP anterior, su digest, vencimiento y contadores.
- No se versionan credenciales SMTP ni se exponen o registran OTP, digest,
  tokens o datos de correo innecesarios.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 214 pruebas, 0 fallos, 0 errores
  y 0 omitidas. Flyway validó V1–V9 y dejó el esquema en V9, sin V10.

## Fase 13.2 — generación, digest y ciclo de vida OTP

## Fase 13.3 — integración con login WEB administrativo

### Agregado

- Política OTP centralizada para WEB con Roles administrativos activos y verify
  preautenticado que crea sesión solamente después de OTP correcto.
- Contrato `OTP_REQUIRED`, revalidación de Usuario, Persona y Roles, y guía
  Postman de autenticación actualizada.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 212 pruebas, 0 fallos, 0 errores
  y 0 omitidas. Flyway validó V1–V9 y dejó el esquema en V9.

### Agregado

- Generación decimal de seis dígitos con `SecureRandom` y digest HMAC-SHA-256
  con secreto externo validado.
- Servicio interno transaccional para creación, cancelación, verify, bloqueo,
  expiración, reenvío y cooldown, sin integrar HTTP ni correo.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 209 pruebas, 0 fallos, 0 errores
  y 0 omitidas. Flyway validó V1–V9 y dejó el esquema en V9.

## Fase 13.1 — modelo y persistencia OTP

### Agregado

- Migración V9 con `otp_challenges`, UUID generado desde Java, FK normal hacia
  `usuarios(login)`, checks, defaults e índice único parcial para PENDING.
- Entidad, enums y repositorio mínimos de challenge OTP; el registro persiste
  `login` como String y nunca el código OTP en texto plano.
- Pruebas de persistencia para Flyway, esquema, constraints, índices, defaults,
  FK e historial.

### Exclusiones

- Sin HMAC, generación o verificación OTP, correo, endpoints, cambios de login,
  JWT, refresh, sesiones, filtros o seguridad HTTP. Las subfases 13.2, 13.3 y
  13.4 permanecen pendientes.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 202 pruebas, 0 fallos, 0 errores
  y 0 omitidas. Flyway validó V1–V9 y dejó el esquema en V9.

## Correccion tecnica — correo obligatorio de Persona

### Agregado

- Migracion V8 para hacer `personas.correo` `VARCHAR(100) NOT NULL` con
  precondicion segura para datos historicos NULL.
- Validacion obligatoria en Create y Update, pruebas y guia Postman actualizadas.

### Exclusiones

- Sin correos fabricados, sin UNIQUE, sin cambios en V1–V7, seguridad, Fase 11,
  Fase 12, Angular u OTP.

### Pendiente

- La base local contiene 1 Persona con correo NULL; V8 debe reintentarse despues
  de corregir manualmente ese registro.

## Fase 12.2 — 2026-08-06

### Agregado

- Administración REST de Menús, Procesos, RolMe y MePro mediante DTOs, mappers, servicios transaccionales y controladores exclusivos de PROPIETARIO.
- Validación de unicidad, estados, relaciones duplicadas e intentos de asignar extremos inactivos.
- Guías Postman y pruebas para contratos, mappers, servicios y persistencia existente.

### Exclusiones

- Sin V8, `rolpro`, datos iniciales, eliminación física de Menús o Procesos, authorities por Proceso ni cambios de JWT, sesiones, CORS, CSRF o seguridad de Fase 11.2.

## Fase 12.1 — 2026-08-06

### Agregado

- Migración V7 con `menus`, `procesos`, `rolme` y `mepro`.
- Entidades y repositorios persistentes para Menú, Proceso y sus relaciones explícitas por clave compuesta.
- Pruebas PostgreSQL para schema, constraints, relaciones y recorrido Usuario–Rol–Menú–Proceso.

### Exclusiones

- Sin `rolpro`, datos iniciales, endpoints, CRUD, authorities ni cambios en JWT, sesiones, CORS, CSRF o autorización de Fase 11.2.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 180 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Flyway validó V1–V7; no existe V8.

## Fase 11.2 — 2026-08-06

### Agregado

- Matriz `@PreAuthorize` para sesiones, Personas, Usuarios, Roles y asignaciones Usuario–Rol.
- `AuthorizationService` para contraseña propia o propietario y alcance de objetivos comunes.
- `OwnerProtectionService`, bloqueo pesimista y error `409 LAST_OWNER_REQUIRED` para mantener al menos un propietario activo.
- Filtrado paginado de Usuarios propietarios en el listado del ADMINISTRADOR.
- Pruebas unitarias, MVC, method security, persistencia, integración HTTP y concurrencia con PostgreSQL real.

### Seguridad

- PROPIETARIO administra los módulos actuales; ADMINISTRADOR opera Personas y Usuarios comunes; INQUILINO conserva sesiones y contraseña propias.
- El Rol exacto PROPIETARIO no puede renombrarse ni desactivarse. Las desactivaciones indirectas por `PUT`, las directas por `PATCH`, el retiro de asignación y la eliminación de Persona protegen el último propietario.
- Los cambios de Rol siguen reflejándose con el mismo JWT sin revocar sesiones. JWT, CORS, CSRF y contratos WEB/MOBILE permanecen intactos.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 165 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Flyway permanece en V6, V1–V6 intactas y sin V7.
- No se implementó alcance por propiedad ni se inició la Fase 12.
- La validación manual de Postman de los escenarios de Fase 11.2 queda pendiente; este registro no declara pruebas manuales ejecutadas.

## Fase 11.1 — 2026-08-06

### Agregado

- Consulta JPQL escalar de nombres de Roles activos por login, sin carga de entidades completas ni N+1.
- `UserAuthorityService` para normalizar, deduplicar y convertir nombres activos en una colección inmutable de authorities `ROLE_<NOMBRE>`.
- Seguridad por métodos mediante `@EnableMethodSecurity` y contrato estable `403 ACCESS_DENIED` para identidades autenticadas sin authority suficiente.
- Pruebas unitarias, MVC e integración con PostgreSQL real para authorities, cambios inmediatos, JWT mínimo, `401` y `403`.

### Modificado

- `JwtAuthenticationFilter` carga las authorities actuales después de validar JWT, sesión, Usuario y Persona, y las incorpora al `Authentication` existente.
- Los cambios confirmados de asignación o estado de Rol se reflejan en la siguiente petición con la misma sesión y el mismo access token.
- La documentación distingue autenticación de autorización y mantiene la matriz de módulos reservada para Fase 11.2.

### Seguridad

- El JWT conserva únicamente `sub`, `sid`, `iss`, `iat` y `exp`; no contiene Roles, authorities ni permisos.
- Roles inactivos y nombres vacíos/inconsistentes no conceden authority; un Usuario sin Roles continúa autenticado con colección vacía.
- Asignar, retirar, activar o desactivar Roles no revoca sesiones. CORS, CSRF y contratos WEB/MOBILE permanecen sin cambios inseguros.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 149 pruebas, 0 fallos, 0 errores y 0 omitidas.
- PostgreSQL real, Flyway V1–V6 y Hibernate `ddl-auto=validate`; no se creó V7.
- Fase 11 global permanece en desarrollo: no se aplicó la matriz de 11.2 y no se inició la Fase 12.

## Fase 10.2 — 2026-08-04

### Agregado

- Spring Security HTTP stateless, `SecurityFilterChain`, filtro Bearer JWT e identidad mínima `AuthenticatedUser(login, sid)` sin roles ni authorities reales.
- Endpoints autenticados `logout`, `logout-all`, listado de sesiones propias y revocación segura por `sid`.
- Errores `INVALID_TOKEN`, `TOKEN_EXPIRED`, `SESSION_REVOKED` y `SESSION_EXPIRED` en `application/problem+json`.
- CORS con orígenes configurables y credenciales; CSRF de doble envío para refresh WEB basado en cookie.

### Modificado

- Cambio de contraseña, desactivación de Usuario y desactivación de Persona revocan todas las sesiones activas con motivos específicos y dentro de la transacción.
- Activar nuevamente Usuario o Persona no restaura sesiones revocadas.
- La cadena permite públicamente solo login, refresh, preflight y dispatch interno de error; el resto requiere JWT y sesión vigentes.

### Verificación

- Se añadieron pruebas HTTP/integración para JWT, filtro, CORS, CSRF, logout, propiedad y secretos de sesiones y revocaciones administrativas.
- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 141 pruebas, 0 fallos, 0 errores y 0 omitidas.
- PostgreSQL real, Flyway V1–V6 y Hibernate `ddl-auto=validate`; no se creó V7.
- Fase 11 no iniciada: sin Roles en JWT, `hasRole`, `hasAuthority`, permisos, menús, procesos u OTP.

## Fase 10.1 — 2026-08-03

### Agregado

- Migración V6 con `sesiones_usuario`, PK UUID, FK a Usuario, checks, índice por login e índice único parcial para una sesión activa por `(login, device_id)`.
- Entidad, enums y repositorio de sesión; bloqueo pesimista de Usuario en login y de sesión en refresh.
- JWT HS256 con Nimbus JOSE + JWT 10.8, claims mínimos `sub`, `sid`, `iss`, `iat`, `exp` y duración predeterminada de 15 minutos.
- Refresh opaco `sid.secreto` con 256 bits aleatorios, hash SHA-256, comparación constante, duración de 30 días, rotación y detección de reutilización.
- Login WEB/MOBILE y `POST /api/v1/auth/refresh`; cookie WEB HttpOnly configurable y refresh MOBILE en JSON.
- Error seguro `INVALID_REFRESH_TOKEN`, pruebas unitarias, MVC, persistencia e integración real, documento de fase y guía Postman completa.

### Seguridad

- `JWT_SECRET` es obligatorio, externo y de al menos 32 bytes; el arranque falla con configuración débil. No se incluyó un secreto real.
- PostgreSQL almacena solo el hash del refresh; WEB nunca devuelve refresh en JSON y ningún contrato contiene password, hash, Persona completa o Roles.
- Refresh reutilizado/manipulado revoca con `REFRESH_REUSE`; expiración usa `EXPIRED`; el reemplazo del mismo dispositivo usa `REPLACED_BY_NEW_LOGIN`.
- No se agregaron filtro JWT, `SecurityFilterChain`, protección de endpoints, logout, revocaciones administrativas, autorización ni código de 10.2/11.

### Verificación

- `.\mvnw.cmd clean test`: **BUILD SUCCESS**; 133 pruebas, 0 fallos, 0 errores y 0 omitidas.
- PostgreSQL 17.6 conectado; Flyway validó V1–V6, dejó el esquema en versión 6 e Hibernate validó con `ddl-auto=validate`.
- Pruebas automatizadas verificaron contratos WEB/MOBILE, cookie, claims, firma, algoritmos, rotación, reutilización, concurrencia por bloqueos, constraints, defaults, nulabilidad, índices y cascade.
- V1–V5 permanecen intactas y no se creó V7.

## Fase 09 — 2026-08-03

### Agregado

- Endpoint `POST /api/v1/auth/login`, módulo `auth`, DTO de login, servicio transaccional, `Clock` UTC y pruebas unitarias, MVC e integración con PostgreSQL real.
- Validación BCrypt de Usuario y Persona activos, con actualización de `usuarios.ultimo_acceso` únicamente después de una autenticación correcta.
- Código `INVALID_CREDENTIALS`, excepción específica y respuesta `401 application/problem+json` uniforme para credenciales incorrectas, inexistentes, inactivas o inconsistentes.
- Guía Postman de autenticación y documento de cierre de la Fase 09.

### Seguridad

- El login inexistente ejecuta BCrypt contra un hash señuelo constante interno para reducir enumeración de Usuarios.
- La respuesta exitosa contiene exclusivamente `login` y `codper`; no expone password, `passwd`, hashes, Persona, Roles, tokens o sesiones.
- Roles se ignoran: un Usuario sin Roles, con varios Roles o con Roles inactivos puede autenticarse si sus credenciales y estados son válidos.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 114 pruebas, 0 fallos, 0 errores y 0 omitidas.
- PostgreSQL 17.6 conectó; Flyway validó V1–V5 y el esquema permaneció en versión 5; Hibernate validó con `ddl-auto=validate`.
- No se modificaron migraciones ni se implementaron JWT, sesiones, filtros, `SecurityFilterChain`, `AuthenticationManager`, `UserDetailsService` o autorización.
- Se aislaron fixtures preexistentes de integración de Roles para evitar colisiones con roles administrativos ya presentes en PostgreSQL, sin alterar datos persistentes ni cobertura.

## Fase 08 — 2026-08-02

### Agregado

- Migraciones Flyway V4 para `roles` y V5 para `rolusu`, sin modificar V1, V2 ni V3.
- Catálogo REST de Roles con creación, consulta, paginación, actualización de nombre, activación y desactivación idempotentes.
- Relación N:M Usuario–Rol mediante `rolusu`, con asignación, retiro y listados en ambos sentidos.
- Entidades `Rol`, `RolUsu` y `RolUsuId`, repositorios, DTO, mapper, servicios y controladores dentro del módulo `role`.
- Pruebas de mapper, servicio, MVC e integración real contra PostgreSQL, además de guía Postman y documento de cierre de Fase 08.

### Reglas

- `roles.nombre` se normaliza con trim y mayúsculas mediante `Locale.ROOT`; los duplicados devuelven conflicto.
- `rolusu` usa PK `(login, codr)`, conserva `fecha_asignacion`, aplica `ON DELETE CASCADE` desde Usuario y `ON DELETE RESTRICT` desde Rol.
- Un Rol inactivo conserva asignaciones, pero no acepta nuevas y devuelve regla de negocio `422`.
- Ninguna respuesta expone contraseñas, hashes ni datos completos de Persona.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 103 pruebas, 0 fallos, 0 errores y 0 omitidas.
- PostgreSQL 17.6 conectó; Flyway validó V1–V5 y el esquema quedó en versión 5; Hibernate validó el esquema con `ddl-auto=validate`.
- Confirmado que no se implementaron login, autenticación, JWT, sesiones, filtros, authorities, autorización, permisos, OTP, menús ni procesos; la Fase 09 no se inició.

## Fase 07 — 2026-08-02

### Agregado

- Administración REST de Usuario: creación, consulta, paginación, actualización de estado, activación, desactivación y cambio de contraseña.
- BCrypt mediante `PasswordEncoder` y la dependencia mínima `spring-security-crypto`.
- DTO, mapper, servicio, controlador, pruebas unitarias, MVC e integración real con PostgreSQL.
- Guía Postman de Usuario y documento de cierre de la Fase 07.

### Modificado

- `PageResponse<T>` trasladado a `common.dto` y reutilizado por Persona y Usuario.
- Plan, teoría de Etapa 2, índices y README actualizados para cerrar la Fase 07.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 83 pruebas, 0 fallos y 0 errores.
- PostgreSQL 17.6 conectó; Flyway validó V1–V3 sin nuevas migraciones; Hibernate validó el esquema y `contextLoads` pasó.
- Confirmado que no se implementaron login, autenticación, JWT, sesiones, roles ni autorización; no se creó V4 ni se inició la Fase 08.

## Corrección documental del plan — 2026-08-02

### Documentación

- Corrección documental previa al inicio de la Fase 07: se mantuvieron las Fases 00–06 como `COMPLETADA` y la Fase 07 como `PENDIENTE` en ese momento.
- Incorporado BCrypt mediante `PasswordEncoder` en la administración de Usuarios, sin declarar implementadas esas funciones.
- Separadas administración de Usuario, roles, autenticación, JWT, sesiones, autorización, menús, procesos y OTP.
- Documentada la política futura de una sola sesión activa en la Fase 10.
- Corregidas las referencias vigentes de menús/procesos a la Fase 12 y de OTP a la Fase 13.
- Conservada como decisión pendiente la elección de un único nombre para la tabla relacional Usuario–Rol.

### Alcance y validación

- Corrección exclusivamente documental. No se creó código, documento de implementación de Fase 07 ni migración; no se ejecutó Maven y no se inició la Fase 07.

## Fase 06 — 2026-07-31

### Agregado

- Migración Flyway V3 para `usuarios`, con `login` como PK, `codper` único y FK a `personas` con `ON DELETE RESTRICT`.
- Entidad `Usuario`, relación JPA uno a uno unidireccional y `UsuarioRepository` en el módulo `user`.
- Pruebas de integración de esquema, defaults, restricciones, relación, borrado restringido y ausencia de cascada desde Usuario hacia Persona.

### Modificado

- Pruebas de integración existentes actualizadas mínimamente para reconocer V3 y la tabla `usuarios`.
- Plan, documentación de Fase 06, índices, teoría de Etapa 2, documentación de base de datos y README.

### Verificación

- `.\mvnw.cmd clean test`: **BUILD SUCCESS**; 65 pruebas, 0 fallos y 0 errores.
- `contextLoads` pasó; PostgreSQL 17.6 conectó; Flyway validó V1–V3 y aplicó V3; Hibernate validó el esquema con `ddl-auto=validate`.
- Confirmado: no se modificaron V1, V2 ni `personas`; no se añadieron dependencias, CRUD, BCrypt, autenticación, JWT, roles ni sesiones.

## Fase 05 — 2026-07-31

### Agregado

- CRUD HTTP de Persona, paginación, validaciones, manejo de CI duplicado y operaciones idempotentes de activación y desactivación.
- Pruebas específicas de mapper, servicio con mocks, MVC y flujo de integración con PostgreSQL.
- Teoría de la Etapa 2 y guía Postman de Persona actualizada.

### Corregido

- La creación ahora refresca la entidad después de `saveAndFlush` para que `PersonaResponse` incluya `estado` y `fechaRegistro` generados por PostgreSQL cuando se omiten en la solicitud.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 56 pruebas, 0 fallos y 0 errores.
- `contextLoads` pasó; PostgreSQL conectó; Flyway validó V1 y V2 sin aplicar migraciones; Hibernate validó el esquema.
- Confirmado que no existe V3, no se alteró `personas`, no se agregaron tablas, dependencias ni código en `security`.

## Fase 04 — 2026-07-29

### Agregado

- Entidad `Persona` y `PersonaRepository` en el módulo `person`.
- Pruebas de persistencia y restricciones contra PostgreSQL real.
- Migración V2 para hacer obligatorio `personas.fecha_registro`.

### Corregido

- V1 había creado `fecha_registro` nullable; tras confirmar que no existían valores nulos, V2 aplicó la nulabilidad aprobada sin modificar la migración ya aplicada.

### Verificación

- `.\mvnw.cmd clean test`: **BUILD SUCCESS**, 17 pruebas, 0 fallos y 0 errores.
- Flyway registró V1 y V2; Hibernate validó el esquema sin DDL y `contextLoads` pasó.
- El esquema público final contiene únicamente `flyway_schema_history` y `personas`.

## Fase 03 — 2026-07-27

### Agregado

- Contrato de errores RFC 9457 basado en `ProblemDetail`, con código interno, instante, ruta y trazabilidad local.
- Excepciones reutilizables para recurso inexistente, conflicto y regla de negocio.
- Manejo global de validación, JSON inválido y errores inesperados sin exponer detalles internos.
- Pruebas MVC aisladas para los casos principales del contrato de error.

### Modificado

- Habilitada la propiedad oficial `spring.mvc.problemdetails.enabled`.
- Actualizados README, plan, índices, arquitectura y teoría de la Etapa 1.

### Verificación

- `.\mvnw.cmd clean test`: **BUILD SUCCESS**; 7 pruebas, 0 fallos y 0 errores.
- `contextLoads` sigue validando PostgreSQL, Hikari, Flyway e Hibernate.
- Confirmado que no se agregaron migraciones SQL, tablas de negocio ni dependencias.

### Corregido

- El primer intento de pruebas reveló que el manejador integrado de Spring tenía prioridad para validación y JSON inválido. Se aplicó `@Order(Ordered.HIGHEST_PRECEDENCE)` al consejo global y la validación final pasó.

## Fase 02 — 2026-07-27

### Agregado

- Configuración de datasource PostgreSQL mediante variables de entorno.
- Configuración inicial de Hibernate en modo `validate` y Flyway con migraciones en `classpath:db/migration`.
- `.env.example` sin credenciales reales y reglas para ignorar `.env`.

### Modificado

- `.env.example` ahora usa un marcador de contraseña y no contiene una credencial concreta.
- README, plan e índice de fases actualizados para registrar el cierre de la Fase 02.

### Documentación

- Registrada la causa del placeholder literal y su corrección mediante importación de variables de usuario al proceso que ejecuta Maven.
- Confirmado que las credenciales siguen siendo externas y no se almacenan en el repositorio.

### Verificación

- Compilación principal y de pruebas correcta.
- El árbol de dependencias confirma JPA, driver PostgreSQL, Flyway Core y `flyway-database-postgresql`.
- `.\mvnw.cmd clean`: **BUILD SUCCESS**.
- `.\mvnw.cmd test`: **BUILD SUCCESS**; `contextLoads` ejecutó 1 prueba, 0 fallos y 0 errores.
- PostgreSQL aceptó la conexión; HikariPool inició; Flyway validó 0 migraciones y creó `flyway_schema_history`.
- La consulta JDBC de solo lectura confirmó que `flyway_schema_history` es la única tabla pública y que no hay tablas de negocio.

## Fase 00 — 2026-07-27

### Agregado

- Reglas permanentes de trabajo en `AGENTS.md`.
- Plan general inicial de etapas y fases, corregido posteriormente para conservar únicamente las fases 00 a 17 confirmadas.
- Documento teórico de la Etapa 1 y registro de la Fase 00.
- Cinco ADR para arquitectura, esquema, Git, configuración y Lombok.
- Documentación inicial de arquitectura y base de datos.
- Índices documentales y README principal.
- Configuración YAML del nombre de aplicación y puerto `9090`.

### Modificado

- Formato principal de configuración migrado de properties a YAML.

### Eliminado

- `src/main/resources/application.properties`, después de trasladar su única propiedad.

### Corregido

- No se registran correcciones de código funcional.

### Documentación

- Registrados alcance, decisiones, modelo inicial, tablas postergadas, reglas de seguridad y secuencia de trabajo.
- Documentadas las restricciones de Git y los criterios de cierre por fase.
- Eliminadas temporalmente del plan confirmado las fases de propiedades, unidades y disponibilidad pública por falta de información funcional proporcionada.
- Renumeradas las fases de autenticación, autorización, calidad y producción desde la Fase 09 hasta la Fase 17.
- Incorporada una etapa futura de módulos adicionales con estado `PENDIENTE DE ANÁLISIS`, sin fases numeradas.
- Corregidas las referencias internas del objetivo y las dependencias generales del plan.

### Verificación

- Verificados 18 documentos Markdown, incluido `HELP.md`: ninguno vacío y ningún enlace relativo roto.
- Confirmado que solo existen la clase principal y la prueba Java iniciales; no se crearon clases de negocio.
- Confirmado `application.yml` como único recurso de configuración, con puerto `9090`.
- Maven compiló las clases principal y de prueba; `contextLoads` terminó con 1 error por ausencia de datasource, configuración reservada para la Fase 02.
- Maven Wrapper no pudo iniciar Maven debido a un error de su script PowerShell; se verificó adicionalmente con Maven 3.9.16 instalado.

## Fase 01 — 2026-07-27

### Modificado

- Normalizados los metadatos Maven, el empaquetado JAR y la codificación UTF-8.
- Renombradas la clase principal y la prueba inicial como `OrmanBackendApplication` y `OrmanBackendApplicationTests`.
- Actualizado el nombre de aplicación a `ORMAN-BACKEND` en `application.yml`.
- Corregido `mvnw.cmd` para manejar correctamente un directorio Maven local que no sea enlace simbólico.

### Eliminado

- Configuración personalizada e innecesaria de `maven-compiler-plugin`; se conserva la configuración administrada por Spring Boot.
- Archivos `BackendApplication.java` y `BackendApplicationTests.java`, reemplazados por sus nombres normalizados.

### Documentación

- Registrado el diagnóstico y la corrección del Maven Wrapper.
- Creado el documento de la Fase 01 y actualizados el plan, índices, README y teoría de la Etapa 1.

### Verificación

- Confirmados Java 21, Maven 3.9.16, Spring Boot 4.1.0, paquete base y puerto 9090.
- El Wrapper funciona desde PowerShell y mediante `cmd /c`.
- `clean test-compile` compila correctamente clases principales y de prueba.
- `contextLoads` sigue fallando únicamente por falta de datasource, reservada para la Fase 02.
