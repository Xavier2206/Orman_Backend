# Índice de guías Postman

Las guías usan `{{baseUrl}}` y Bearer de un Usuario autorizado. No contienen tokens, contraseñas ni datos permanentes.

| Guía | Contenido |
|---|---|
| [menu.md](menu.md) | Catálogo de Menús |
| [proceso.md](proceso.md) | Catálogo de Procesos |
| [rolme.md](rolme.md) | Asignación Roles–Menús |
| [mepro.md](mepro.md) | Asignación Menús–Procesos |
| [auth.md](auth.md) | Login, JWT, refresh, sesiones y contexto autenticado |
| [persona.md](persona.md) | CRUD de Personas |
| [usuario.md](usuario.md) | CRUD de Usuarios y contraseñas |
| [rol.md](rol.md) | Catálogo de Roles |
| [rolusu.md](rolusu.md) | Asignación Usuario–Rol |
| [property.md](property.md) | Propiedades, Unidades y UnidadFotos |

Orden sugerido para Fase 12.2: [auth.md](auth.md) → [rol.md](rol.md) → [menu.md](menu.md) y [proceso.md](proceso.md) → [rolme.md](rolme.md) y [mepro.md](mepro.md).

Las relaciones nuevas solo pueden administrarse con `ROLE_PROPIETARIO`. Los Roles no se incluyen en el JWT; el backend carga authorities desde PostgreSQL en cada petición protegida.
