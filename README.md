# plazoleta-trazabilidad

Microservicio de **trazabilidad** de la plazoleta de comidas (Spring Boot 3 + arquitectura hexagonal + MongoDB).
Guarda un registro de cada cambio de estado de los pedidos y permite que cada cliente consulte el historial de
sus propios pedidos.

## Historias de usuario implementadas

| HU | Descripción | Endpoint | Rol |
|----|-------------|----------|-----|
| HU-17 | Registrar un cambio de estado de un pedido | `POST /traces` | CLIENT o EMPLOYEE (lo llama plazoleta) |
| HU-17 | Consultar la trazabilidad de un pedido | `GET /traces/orders/{orderId}` | CLIENT (dueño del pedido) |

### Reglas de la HU-17

- **Se crea un registro por cada cambio de estado**. `plazoleta-plazoleta` llama a `POST /traces` cada vez que un
  pedido cambia de estado, reenviando el token de quien hizo el cambio:

  | Acción en plazoleta | Quién | Registro |
  |---------------------|-------|----------|
  | Realizar pedido | Cliente | — → `PENDIENTE` |
  | Asignarse el pedido | Empleado | `PENDIENTE` → `EN_PREPARACION` |
  | Pedido listo | Empleado | `EN_PREPARACION` → `LISTO` |
  | Pedido entregado | Empleado | `LISTO` → `ENTREGADO` |
  | Cancelar pedido | Cliente | `PENDIENTE` → `CANCELADO` |

- Cada registro guarda: `orderId`, `clientId`, `restaurantId`, `employeeId`, `previousStatus`, `newStatus` y
  `date`. La fecha la asigna este servicio en el momento del cambio.
- Al registrar se valida quién lo hace: un **cliente** solo puede registrar cambios de sus propios pedidos y un
  **empleado** solo de pedidos de su restaurante (403 en otro caso).
- **Cada cliente consulta solo sus propios pedidos**: `GET /traces/orders/{orderId}` devuelve el historial del
  más antiguo al más reciente. Pedido de otro cliente: 403. Sin registros: 404.

## Requisitos

- Java 21+
- Maven 3.9+
- Docker (para MongoDB)
- `plazoleta-usuarios` (8081) para obtener los tokens y `plazoleta-plazoleta` (8082), que registra los cambios

## Cómo levantarlo

```bash
docker compose up -d          # MongoDB en localhost:27018
mvn spring-boot:run           # Servicio en http://localhost:8084
```

MongoDB se publica en el puerto **27018** del equipo para no chocar con un MongoDB instalado localmente
(que usa el 27017).

Swagger UI: http://localhost:8084/swagger-ui.html
OpenAPI JSON: http://localhost:8084/v3/api-docs

### Variables de entorno

| Variable | Default |
|----------|---------|
| `SERVER_PORT` | `8084` |
| `MONGODB_URI` | `mongodb://localhost:27018/plazoleta_trazabilidad` |
| `JWT_SECRET` | misma clave que `plazoleta-usuarios` |

## Uso rápido

```bash
# 1. Token de un CLIENTE (microservicio de usuarios)
curl -X POST http://localhost:8081/auth/login -H "Content-Type: application/json" \
  -d '{"email":"<correo_cliente>","password":"<clave>"}'

# 2. Hacer un pedido en plazoleta (queda registrado: -> PENDIENTE)
curl -X POST http://localhost:8082/orders \
  -H "Authorization: Bearer <TOKEN_CLIENTE>" -H "Content-Type: application/json" \
  -d '{"restaurantId":1,"dishes":[{"dishId":1,"quantity":2}]}'

# 3. Consultar el historial de ese pedido
curl http://localhost:8084/traces/orders/1 -H "Authorization: Bearer <TOKEN_CLIENTE>"
```

Respuesta:

```json
[
  { "id": "6729...", "orderId": 1, "clientId": 30, "restaurantId": 1, "employeeId": null,
    "previousStatus": null, "newStatus": "PENDIENTE", "date": "2026-11-04T11:51:18.493" },
  { "id": "672a...", "orderId": 1, "clientId": 30, "restaurantId": 1, "employeeId": 8,
    "previousStatus": "PENDIENTE", "newStatus": "EN_PREPARACION", "date": "2026-11-04T11:58:02.110" }
]
```

## Pruebas con Postman

En la carpeta `postman/`:

- `plazoleta-trazabilidad.postman_collection.json`: 31 peticiones y 44 aserciones.
- `plazoleta-local.postman_environment.json`: `baseUrl` (8084), `usuariosUrl` (8081), `plazoletaUrl` (8082)
  y credenciales del admin.

1. En Postman: **Import** → seleccionar ambos archivos.
2. Seleccionar el entorno **Plazoleta - Local (trazabilidad)**.
3. Clic derecho sobre la colección → **Run collection** → **Run**.

`00 - Preparacion` crea propietario, restaurante, plato, empleado y dos clientes; el primer cliente hace un pedido
y el empleado se lo asigna (plazoleta registra esos dos cambios). `01 - HU-17 Consultar trazabilidad` consulta el
historial y prueba otro cliente (403), pedido sin registros (404), roles y token. `02 - HU-17 Registrar cambio de
estado` prueba `POST /traces` directamente: cliente y empleado autorizados (201), cliente en nombre de otro y
empleado de otro restaurante (403), validaciones (400), roles y token.
El flujo completo con todos los estados está en la carpeta `15 - HU-17` de la colección de `plazoleta-plazoleta`.

## Arquitectura

```
domain/            Java puro: modelo Trace y OrderStatus, puertos (api / spi), caso de uso, excepciones
application/       DTOs, mapper y handler
infrastructure/    Controller REST, adaptador MongoDB (documento, repositorio), seguridad JWT,
                   manejo de errores y configuración
```

## Pruebas

```bash
mvn test                      # Reporte de cobertura en target/site/jacoco/index.html
```
