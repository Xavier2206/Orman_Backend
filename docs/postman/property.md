# Propiedades, Unidades y UnidadFotos

Todas las rutas requieren Bearer JWT válido de un Usuario con `ROLE_PROPIETARIO`.
El backend comprueba además que la Persona asociada al Usuario sea la propietaria
de cada recurso solicitado. Use datos de prueba.

## Propiedades

`POST {{baseUrl}}/api/v1/propiedades`

```json
{
  "nombre": "Casa Sur",
  "tipo": "CASA",
  "direccion": "Calle Ejemplo 123",
  "ciudad": "La Paz",
  "referencia": "Zona Sur",
  "latitud": -16.540000,
  "longitud": -68.080000,
  "portadaUrl": "https://example.test/casa-sur.jpg",
  "codperPropietaria": 1,
  "inversionInicial": 100000.00,
  "estado": 1
}
```

Devuelve `201 Created` y `Location`. `codperPropietaria` debe coincidir con la
Persona asociada al token.

- `GET {{baseUrl}}/api/v1/propiedades?q=sur&tipo=CASA&estado=1&page=0&size=20&sort=nombre,asc`
- `GET {{baseUrl}}/api/v1/propiedades/{{codprop}}`
- `PUT {{baseUrl}}/api/v1/propiedades/{{codprop}}`
- `PATCH {{baseUrl}}/api/v1/propiedades/{{codprop}}/activar`
- `PATCH {{baseUrl}}/api/v1/propiedades/{{codprop}}/desactivar`

## Unidades

`POST {{baseUrl}}/api/v1/propiedades/{{codprop}}/unidades`

```json
{
  "nombre": "Unidad 101",
  "tipoUnidad": "DEPARTAMENTO",
  "descripcion": "Unidad de prueba",
  "area": 45.50,
  "dormitorios": 1,
  "banos": 1,
  "piso": 1,
  "ubicacionInterna": "Torre A",
  "precioBase": 2500.00,
  "estadoOperativo": 1
}
```

- `GET {{baseUrl}}/api/v1/propiedades/{{codprop}}/unidades?page=0&size=20&sort=nombre,asc`
- `GET {{baseUrl}}/api/v1/unidades/{{coduni}}`
- `PUT {{baseUrl}}/api/v1/unidades/{{coduni}}`

## UnidadFotos

`POST {{baseUrl}}/api/v1/unidades/{{coduni}}/fotos`

```json
{
  "url": "https://example.test/unidad-101-sala.jpg",
  "titulo": "Sala",
  "ambiente": "Sala",
  "orden": 0
}
```

- `GET {{baseUrl}}/api/v1/unidades/{{coduni}}/fotos`
- `PUT {{baseUrl}}/api/v1/unidades/{{coduni}}/fotos/{{id}}`
- `PATCH {{baseUrl}}/api/v1/unidades/{{coduni}}/fotos/{{id}}/portada`
- `DELETE {{baseUrl}}/api/v1/unidades/{{coduni}}/fotos/{{id}}`

La foto inicia sin portada. El PATCH desmarca la portada previa y marca la
solicitada; PostgreSQL impide dos portadas en una Unidad.

## Errores esperados

- `400 VALIDATION_ERROR`: campos, URL, estados, coordenadas o valores numéricos inválidos.
- `403 ACCESS_DENIED`: sin `ROLE_PROPIETARIO` o sin propiedad del recurso.
- `404 RESOURCE_NOT_FOUND`: recurso inexistente o incoherente con la ruta.
- `409 CONFLICT`: nombre de Unidad u orden de foto duplicado.
- `422 BUSINESS_RULE_VIOLATION`: latitud y longitud incompletas.
