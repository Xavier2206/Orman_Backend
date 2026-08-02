# Tablas postergadas

## Motivo de postergación

Estas tablas dependen de decisiones de autenticación o autorización que todavía no están diseñadas. Implementarlas ahora fijaría prematuramente relaciones, permisos y ciclos de vida. Permanecen documentadas para análisis futuro, sin formar parte del núcleo inicial.

| Tabla | Propósito preliminar | Fase futura prevista |
|---|---|---|
| `menus` | Representar opciones de navegación autorizables. | Fase 12 — Menús y procesos dinámicos |
| `procesos` | Representar acciones o capacidades asociables a menús y roles. | Fase 12 — Menús y procesos dinámicos |
| `rol_menu` | Relacionar roles con menús disponibles. | Fase 12 — Menús y procesos dinámicos |
| `menu_proceso` | Relacionar menús con procesos permitidos. | Fase 12 — Menús y procesos dinámicos |
| `login_challenges` | Gestionar desafíos temporales del proceso OTP. | Fase 13 — OTP y desafíos de autenticación |
| `sesiones_usuario` | Controlar una sola sesión activa, revocación y hash de refresh token. | Fase 10 — JWT y control de sesiones |

## Condición

Los nombres y propósitos son preliminares. Cada tabla deberá analizarse en su fase, junto con restricciones, índices, estados, auditoría, caducidad y seguridad. Ninguna se implementa antes de la fase que la autorice.
