# Fase 13 — OTP y autenticación de doble factor

## Objetivo general

Incorporar una protección adicional para los accesos administrativos WEB sin alterar el flujo actual de autenticación hasta que se completen las subfases posteriores.

## Política OTP aprobada

- WEB + PROPIETARIO: OTP obligatorio.
- WEB + ADMINISTRADOR: OTP obligatorio.
- WEB + INQUILINO: sin OTP.
- MOBILE + PROPIETARIO, ADMINISTRADOR o INQUILINO: sin OTP.

El único canal aprobado es correo electrónico. No forman parte de esta fase SMS, WhatsApp, llamadas, TOTP, aplicaciones autenticadoras, QR ni canales múltiples.

## Arquitectura prevista

El flujo final previsto es: login y contraseña, evaluación de política OTP, creación de challenge y envío de correo cuando corresponda, verificación, y solo entonces creación de sesión/JWT. En 13.1 únicamente existe la persistencia del challenge: todavía no hay generación, HMAC, verificación, correo, endpoints ni integración con login.

## 13.1 Modelo y persistencia OTP

### Implementación

La migración V9 crea `otp_challenges` con `id UUID`, `login VARCHAR(30)`, `client_type`, `purpose`, `otp_digest VARCHAR(64)`, `status`, los contadores de intentos y reenvíos, fechas, IP y agente de usuario. UUID se genera desde Java. Las fechas se representan con `LocalDateTime`, de acuerdo con la estrategia UTC del `Clock.systemUTC()` existente y las columnas `TIMESTAMP` del proyecto.

`login` se persiste como `String`, no como relación JPA hacia `Usuario`. El challenge requiere conservar el identificador y no navegar el agregado Usuario; esto evita carga innecesaria y simplifica la persistencia. PostgreSQL mantiene la integridad mediante `fk_otp_challenges_login` hacia `usuarios(login)`.

La FK no declara acción `ON DELETE`: conserva el comportamiento PostgreSQL por defecto, no usa cascada y no introduce una variante nueva de borrado para esta tabla.

Los enums persistidos son `ClientType` existente (`WEB`, `MOBILE`), `OtpPurpose` (`LOGIN`) y `OtpChallengeStatus` (`PENDING`, `VERIFIED`, `LOCKED`, `CANCELLED`). No existe el estado persistido `EXPIRED`: la expiración es lógica cuando un registro PENDING alcanza `expires_at`.

### Constraints e índices

- PK UUID, FK de `login`, y checks de cliente, propósito y estado.
- `attempts` y `resend_count` son `SMALLINT NOT NULL DEFAULT 0` y no admiten valores negativos.
- `otp_digest` es obligatorio y reservado para el digest hexadecimal HMAC-SHA-256 de 64 caracteres; en esta subfase no se implementa HMAC ni se almacena OTP en texto plano.
- `expires_at` es obligatorio y debe ser posterior a `created_at`.
- El índice único parcial `uk_otp_challenges_login_client_type_purpose_pending` permite un solo PENDING por `(login, client_type, purpose)`. El historial no PENDING puede coexistir. Los PENDING expirados seguirán siendo responsabilidad lógica de 13.2/13.3.

### Pruebas

Las pruebas de persistencia validan V9, esquema, defaults, UUID, FK, checks, longitudes, nulabilidad, IPv4/IPv6, expiración, el índice parcial y que borrar un challenge no afecta a Usuario. También confirman que las tablas previas siguen disponibles. No prueban OTP real, HMAC, aleatoriedad, SMTP, verify, resend, login ni JWT.

Validación final: `./mvnw.cmd clean test` terminó con **BUILD SUCCESS**: 202
pruebas, 0 fallos, 0 errores y 0 omitidas. Flyway validó V1–V9 y confirmó el
esquema en versión 9; no existe V10.

### Alcance y seguridad

No se modificaron Usuario, AuthService, login, SessionService, JWT, SecurityFilterChain, filtros, autorización ni contratos WEB/MOBILE. No se añadieron dependencias ni canales de entrega. Las subfases futuras deberán cancelar o sustituir el PENDING previo antes de crear otro, respetando el índice único parcial.

## 13.2 Generación, validación y seguridad OTP

Estado: COMPLETADA.

La lógica interna usa `SecureRandom` para generar códigos decimales de seis
dígitos, incluidos ceros iniciales. El digest es HMAC-SHA-256 de
`challengeId + ":" + otp`, en hexadecimal lowercase de 64 caracteres, con
comparación constante mediante `MessageDigest.isEqual`.

`OTP_HMAC_SECRET` es externo, obligatorio y debe tener al menos 32 bytes; no se
incluye ningún secreto real. `security.otp` centraliza expiración de 300 segundos,
cinco intentos, cooldown de 60 segundos y tres reenvíos máximos.

`OtpChallengeService` crea, verifica, prepara reenvíos y marca envíos reales. La
creación bloquea el Usuario, cancela y hace flush del PENDING anterior antes de
persistir el nuevo challenge, evitando dos PENDING concurrentes. Verify y resend
bloquean el challenge. Un error incrementa intentos; el quinto deja el estado en
LOCKED. Un challenge expirado, CANCELLED, VERIFIED o LOCKED no se reutiliza.

El reenvío conserva el mismo UUID, cambia digest y OTP, renueva expiración,
incrementa `resend_count` y no reinicia intentos. `last_sent_at` solo se escribe
con `markSent`, previsto para el éxito SMTP de 13.4; por eso 13.2 no finge un
envío ni aplica cooldown mientras no exista uno real. El OTP solo vive en el
resultado interno temporal, nunca en entidad, BD, logs o DTO HTTP.

Validación final: `./mvnw.cmd clean test` terminó con **BUILD SUCCESS**: 209
pruebas, 0 fallos, 0 errores y 0 omitidas. Flyway validó V1–V9 y confirmó el
esquema en versión 9; no existe V10.

## 13.3 Integración con login WEB administrativo

Estado: COMPLETADA.

`OtpPolicyService` centraliza la matriz: WEB requiere OTP cuando los Roles
activos actuales contienen PROPIETARIO o ADMINISTRADOR; WEB INQUILINO o sin Rol y
todo MOBILE continúan con autenticación directa. El login consulta las
authorities actuales desde BD antes de decidir, sin incluir Roles en JWT.

El login WEB administrativo devuelve `status=OTP_REQUIRED`, `challengeId` y
`expiresIn`, sin sesión, JWT, refresh, cookie ni actualización de
`ultimo_acceso`. El OTP no se expone. `POST /api/v1/auth/otp/verify` es
preautenticado y valida UUID, código de seis dígitos y los datos de dispositivo
necesarios para reutilizar la creación existente de sesión después del segundo
factor; V9 no almacena esos datos y no requirió V10.

Tras OTP correcto se recargan y bloquean Usuario y Persona, se vuelven a cargar
Roles activos y se reevalúa la política WEB. Si la cuenta, Persona o contexto
administrativo cambió, se rechaza y exige un login nuevo. Solo entonces se crea
la sesión, JWT y refresh WEB existentes. El endpoint se añadió como ruta pública
mínima; CSRF conserva su política actual, aplicable únicamente al refresh WEB
que recibe cookie.

Validación final: `./mvnw.cmd clean test` terminó con **BUILD SUCCESS**: 212
pruebas, 0 fallos, 0 errores y 0 omitidas. Flyway validó V1–V9 y confirmó el
esquema en versión 9; no existe V10.

## 13.4 Correo, reenvío y cierre

Estado: COMPLETADA.

Se agregó `spring-boot-starter-mail` y un `OtpMailService` SMTP síncrono. La
configuración se obtiene exclusivamente del entorno: `MAIL_HOST`, `MAIL_PORT`,
`MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, opciones de autenticación y
STARTTLS, y timeouts de conexión, lectura y escritura de 5 segundos por
defecto. `.env.example` contiene solo placeholders; no se versionan
credenciales ni se registran código OTP, contraseña SMTP o correo completo.

En el login WEB administrativo se crea el challenge, se obtiene el correo
actual de `Persona` y se envía el código de seis dígitos. Solo tras éxito SMTP
se ejecuta `markSent` y se responde `OTP_REQUIRED`. Si el envío inicial falla,
el challenge se cancela y se devuelve `OTP_DELIVERY_FAILED`, sin sesión, JWT,
refresh ni cookie. El correo OTP usa una plantilla HTML ligera de ORMAN con
cabecera oscura, franja visual de verificación, cuerpo claro, código destacable,
botón visual “Copiar código”, vigencia, bloque de seguridad y footer separado.
Usa tablas y estilos inline para Gmail, Outlook y Apple Mail, UTF-8, sin
JavaScript, imágenes externas, fuentes externas ni recursos pesados. No se
reutilizó SVG: no existe una variante rasterizada compatible en el proyecto y
la marca textual ORMAN es más portable entre clientes. El botón es solo visual,
sin enlace ni clipboard, porque los clientes de correo bloquean esa interacción.

`POST /api/v1/auth/otp/resend` es preautenticado y recibe únicamente el
`challengeId`. Solo admite challenges LOGIN, WEB, PENDING, no expirados, no
bloqueados y dentro de los límites existentes: cooldown de 60 segundos desde
`last_sent_at` real y máximo tres reenvíos. Mantiene el mismo UUID, no reinicia
intentos y renueva OTP/digest, vencimiento, contador y `last_sent_at` solo
después de enviar exitosamente.

Para preservar consistencia, el reenvío prepara el OTP y digest nuevos en
memoria, envía el correo y confirma el cambio bajo bloqueo únicamente si SMTP
responde correctamente. Un fallo de SMTP conserva exactamente el OTP/digest,
vencimiento, contador de reenvíos y `last_sent_at` anteriores; por tanto el
código anterior permanece verificable. No se requirió V10 ni se modificó V9.

Las pruebas cubren la composición del correo, éxito y fallo SMTP, login WEB
administrativo, ausencia de correo en MOBILE y WEB INQUILINO, reenvío,
cooldown, máximo de reenvíos y la preservación del OTP previo ante un fallo de
entrega. `docs/postman/auth.md` documenta login OTP, verify, resend, cookies,
límites y errores.

Validación de cierre: `./mvnw.cmd clean test` terminó con **BUILD SUCCESS**:
214 pruebas, 0 fallos, 0 errores y 0 omitidas. Flyway validó V1–V9 y confirmó
el esquema en V9; no existe V10. La Fase 13 queda completada.
