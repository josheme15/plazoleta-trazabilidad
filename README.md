# plazoleta-trazabilidad

Microservicio de **trazabilidad** de la plazoleta de comidas (Spring Boot 3 + arquitectura hexagonal + MongoDB).
Guarda un registro de cada cambio de estado de los pedidos, permite que cada cliente consulte el historial de
sus propios pedidos y que cada propietario mida la eficiencia de los pedidos de su restaurante.

## Historias de usuario implementadas

| HU | Descripción | Endpoint | Rol |
|----|-------------|----------|-----|
| HU-17 | Registrar un cambio de estado de un pedido | `POST /traces` | CLIENT o EMPLOYEE (lo llama plazoleta) |
| HU-17 | Consultar la trazabilidad de un pedido | `GET /traces/orders/{orderId}` | CLIENT (dueño del pedido) |
| HU-18 | Tiempo de inicio y fin de cada pedido | `GET /traces/restaurants/{restaurantId}/orders` | OWNER (dueño del restaurante) |
| HU-18 | Ranking de empleados por tiempo medio | `GET /traces/restaurants/{restaurantId}/employees/ranking` | OWNER (dueño del restaurante) |

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

### Reglas de la HU-18

- Un pedido **inicia** cuando el cliente lo hace (registro sin estado anterior → `PENDIENTE`) y **termina** cuando
  se entrega (→ `ENTREGADO`). La duración es el tiempo entre esos dos registros.
- **Tiempo de cada pedido** (`GET /traces/restaurants/{id}/orders`): para cada pedido entregado del restaurante
  devuelve `orderId`, `employeeId` (quien lo atendió), `startDate`, `endDate`, `durationSeconds` y
  `durationMinutes`, del más antiguo al más reciente. Los pedidos en proceso o cancelados no tienen fin y no se
  incluyen.
- **Ranking de empleados** (`GET /traces/restaurants/{id}/employees/ranking`): por cada empleado, `completedOrders`
  y `averageDurationMinutes` (tiempo medio entre que sus pedidos inician y se terminan), ordenado del más rápido
  (`position` 1) al más lento. En empate va primero quien entregó más pedidos.
- Solo el **propietario del restaurante**: este servicio consulta `GET /restaurants/{id}` en `plazoleta-plazoleta`
  (reenviando el token) para comparar el `ownerId` con el usuario del token. Otro propietario u otro rol: 403.
  Restaurante inexistente: 404. Plazoleta caída: 503.

## Requisitos

- Java 21+
- Maven 3.9+
- Docker (para MongoDB)
- `plazoleta-usuarios` (8081) para obtener los tokens y `plazoleta-plazoleta` (8082), que registra los cambios
  y al que se consulta el propietario del restaurante (HU-18)

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
| `PLAZOLETA_URL` | `http://localhost:8082` |

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

# 4. (HU-18) Eficiencia de los pedidos entregados y ranking de empleados, con el token del PROPIETARIO
curl http://localhost:8084/traces/restaurants/1/orders -H "Authorization: Bearer <TOKEN_PROPIETARIO>"
curl http://localhost:8084/traces/restaurants/1/employees/ranking -H "Authorization: Bearer <TOKEN_PROPIETARIO>"
```

Respuesta del historial:

```json
[
  { "id": "6729...", "orderId": 1, "clientId": 30, "restaurantId": 1, "employeeId": null,
    "previousStatus": null, "newStatus": "PENDIENTE", "date": "2026-11-04T11:51:18.493" },
  { "id": "672a...", "orderId": 1, "clientId": 30, "restaurantId": 1, "employeeId": 8,
    "previousStatus": "PENDIENTE", "newStatus": "EN_PREPARACION", "date": "2026-11-04T11:58:02.110" }
]
```

Respuesta del ranking:

```json
[
  { "position": 1, "employeeId": 9, "completedOrders": 3, "averageDurationMinutes": 12.5 },
  { "position": 2, "employeeId": 8, "completedOrders": 1, "averageDurationMinutes": 20.0 }
]
```

## Pruebas con Postman

En la carpeta `postman/`:

- `plazoleta-trazabilidad.postman_collection.json`: 53 peticiones y 75 aserciones.
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
`03 - HU-18 Eficiencia pedidos` registra dos pedidos completos (de PENDIENTE a ENTREGADO), uno por cada empleado
(el primero tarda 3 segundos más), y comprueba el inicio, fin y duración de cada pedido, que el pedido aún en
preparación no aparezca y que el ranking ponga primero al empleado más rápido; prueba también otro propietario
(403), restaurante inexistente (404), roles y token.
El flujo completo con todos los estados está en la carpeta `15 - HU-17` de la colección de `plazoleta-plazoleta`.

## Arquitectura

```
domain/            Java puro: modelos (Trace, OrderEfficiency, EmployeeRanking), puertos (api / spi),
                   casos de uso (TraceUseCase, OrderEfficiencyUseCase), excepciones
application/       DTOs, mapper y handler
infrastructure/    Controllers REST, adaptador MongoDB (documento, repositorio), cliente HTTP a plazoleta,
                   seguridad JWT,
                   manejo de errores y configuración
```

## Pruebas

```bash
mvn test                      # Reporte de cobertura en target/site/jacoco/index.html
```
