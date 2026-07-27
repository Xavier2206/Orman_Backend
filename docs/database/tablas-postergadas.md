# Tablas postergadas

## Motivo de postergación

Estas tablas dependen de decisiones de autenticación o autorización que todavía no están diseñadas. Implementarlas ahora fijaría prematuramente relaciones, permisos y ciclos de vida. Permanecen documentadas para análisis futuro, sin formar parte del núcleo inicial.

| Tabla | Propósito preliminar | Etapa futura prevista |
|---|---|---|
| `menus` | Representar opciones de navegación autorizables. | Etapa 4, Fase 16 |
| `procesos` | Representar acciones o capacidades asociables a menús y roles. | Etapa 4, Fase 16 |
| `rol_menu` | Relacionar roles con menús disponibles. | Etapa 4, Fase 16 |
| `menu_proceso` | Relacionar menús con procesos permitidos. | Etapa 4, Fase 16 |
| `login_challenges` | Gestionar desafíos temporales del proceso OTP. | Etapa 4, Fase 17 |

## Condición

Los nombres y propósitos son preliminares. Cada tabla deberá analizarse en su fase, junto con restricciones, índices, estados, auditoría, caducidad y seguridad. Ninguna se implementa en la Fase 00.
