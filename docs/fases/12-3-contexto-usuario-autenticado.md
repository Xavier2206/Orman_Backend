# Fase 12.3 — Contexto del usuario autenticado

## Estado

`COMPLETADA` el 2026-08-13 mediante autorización explícita para el contexto post-login. No incorpora funcionalidades fuera de ese alcance.

## Objetivo

Incorporar `GET /api/v1/auth/context` para que un cliente autenticado recupere su estado vigente de Usuario, Persona, Roles, Menús y Procesos después de login, OTP, refresh o una recarga de página.

## Identidad y seguridad

La identidad se toma solamente del `AuthenticatedUser(login, sid)` que el filtro JWT ya instala en el `SecurityContext`. El controller recibe ese principal con `@AuthenticationPrincipal`; el endpoint no acepta login, codper ni otro identificador de Usuario como parte de su contrato. Un query parameter ajeno no interviene en la consulta.

`/api/v1/auth/context` exige autenticación mediante la cadena HTTP y declara `@PreAuthorize("isAuthenticated()")`. No exige `ROLE_PROPIETARIO`, `ROLE_ADMINISTRADOR` ni ningún Rol concreto. Así, el Usuario autenticado solo consulta su propio contexto.

La autorización de backend conserva el flujo existente: Roles activos -> `GrantedAuthority` -> Spring Security / `@PreAuthorize`. Menú y Proceso no se convierten en authorities; esta respuesta solo representa navegación para el cliente. No cambian JWT, sesiones, filtro, refresh, OTP, logout, CORS, CSRF, ni APIs administrativas.

## Contrato

La respuesta correcta es `200 OK`:

```json
{
  "usuario": { "login": "usuario.demo", "codper": 15 },
  "persona": { "nombre": "Walter", "ap": "Pérez", "am": null, "foto": "https://ejemplo.test/perfiles/walter.png" },
  "roles": [
    {
      "codr": 1,
      "nombre": "ADMINISTRADOR",
      "menus": [
        {
          "codm": 10,
          "nombre": "USUARIOS",
          "icono": "users",
          "procesos": [
            { "codp": 100, "nombre": "LISTAR USUARIOS", "enlace": "usuarios/listar" }
          ]
        }
      ]
    }
  ]
}
```

No contiene `passwd`, hash BCrypt, correo, CI, teléfono, estado, tokens, sid, refresh token, authorities ni entidades JPA.

La Persona se obtiene mediante `Usuario.persona`, relación uno a uno por `usuarios.codper`. Solo se devuelven los datos visuales mínimos: `nombre`, `ap`, `am` y `foto`.

`Persona.foto` es la referencia `String` opcional ya persistida en `personas.foto` (`VARCHAR(255)`). No existe carga binaria, storage ni endpoint de foto en el backend. El contexto devuelve la referencia exactamente como se almacenó; si es nula, `foto` es `null`.

`Proceso.enlace` se incluye conservando su significado actual: dato persistente único del Proceso, no authority, permiso, endpoint REST ni ruta Angular reinterpretada por esta fase.

## Datos vigentes, filtros y orden

- Solo aparecen Roles con `roles.estado = 1` asignados al Usuario por `rolusu`.
- Cada Rol conserva sus asociaciones `rolme`; se incluyen solo Menús con `menus.estado = 1`.
- Cada Menú conserva sus asociaciones `mepro`; se incluyen solo Procesos con `procesos.estado = 1`.
- Un Menú compartido se devuelve dentro de cada Rol que lo tenga asociado. Un Menú sin Procesos activos se conserva con `procesos: []`.
- Las relaciones quitadas desaparecen en la siguiente solicitud; no hay cache ni información de navegación dentro del JWT.
- Roles, Menús y Procesos se ordenan por `nombre` ascendente y su identificador como desempate (`codr`, `codm`, `codp`).

## Persistencia y rendimiento

La consulta de Usuario usa un `EntityGraph` para cargar su Persona en una operación. La navegación se recupera en una proyección JPQL única con joins izquierdos desde `RolUsu` a `RolMe` y `MePro`. La proyección permite mantener Roles sin Menús y Menús sin Procesos activos, y el mapper explícito reconstruye el árbol con mapas de inserción ordenada. No se hace HTTP interno ni una consulta por Rol, Menú o Proceso, evitando un N+1 evidente.

No se modificó Flyway ni se creó tabla, columna, índice, relación, ruta Angular o jerarquía de Menús.

## Pruebas

- `AuthContextServiceImplTest`: la consulta usa exclusivamente el login del principal autenticado.
- `AuthContextIntegrationTest`: rechazo sin Bearer, Usuario normal, ADMINISTRADOR y PROPIETARIO; Persona correcta, foto presente y nula; Roles/Menús/Procesos activos e inactivos; Menú compartido entre Roles; eliminación de Rol–Menú y Menú–Proceso visible en la siguiente consulta; y parámetro `login` ajeno ignorado.

Validación de cierre: `./mvnw.cmd clean test` finalizó con **BUILD SUCCESS**; 218 pruebas, 0 fallos, 0 errores y 0 omitidas. PostgreSQL 17.6 estuvo conectado; Flyway validó V1–V9 sin migraciones nuevas y Hibernate mantuvo `ddl-auto=validate`.

## Documentación y pendientes

La guía Postman de autenticación describe el endpoint. Angular debe llamar `GET /api/v1/auth/context` con el Bearer vigente tras autenticación/OTP, refresh y restauración de la aplicación; debe usar `persona.foto` como referencia disponible y recorrer `roles -> menus -> procesos` sin deduplicar Menús entre Roles.

No se modificó Angular, ni se implementaron cache, polling, WebSocket, localStorage, nuevos permisos, authorities de navegación, orden configurable, jerarquía de Menús o rutas Angular.
