# Coffee Shop API

> **Rama:** `main` — Produccion (version `1.1.0`, perfil Spring activo por defecto: `prod`).

Backend REST para gestionar una cafeteria. El proyecto esta construido con Spring Boot, PostgreSQL, JPA y Flyway. Cubre el dominio completo del negocio: catalogo (sucursales, categorias, productos, ingredientes), usuarios/clientes/empleados, flujo de ventas (ordenes y pagos) e inventario (recetas y stock por sucursal con descuento automatico al vender). La API esta versionada (`/api/v1`), autenticada con JWT y autorizada por rol, paginada, documentada con OpenAPI/Swagger y empaquetada con Docker + CI.

## Stack Tecnico

- Java 25
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- PostgreSQL 16
- Flyway
- Lombok
- Jakarta Validation
- Spring Security + JWT (jjwt) para autenticacion/autorizacion
- springdoc-openapi (Swagger UI)
- Testcontainers (tests de integracion)
- Maven Wrapper
- Docker / Docker Compose

## Configuracion

La aplicacion se configura por variables de entorno. En el perfil `prod` las credenciales de base de datos y el secreto JWT **no tienen valor por defecto**: si faltan, la app no arranca. Es deliberado, para que un despliegue mal configurado falle al iniciar en vez de caer silenciosamente a las credenciales de desarrollo.

| Variable | Obligatoria en `prod` | Default | Descripcion |
|---|---|---|---|
| `DB_URL` | si | — | JDBC de PostgreSQL, ej. `jdbc:postgresql://host:5432/cafeteria_db` |
| `DB_USERNAME` | si | — | Usuario de la base |
| `DB_PASSWORD` | si | — | Contrasena de la base |
| `JWT_SECRET` | si | — | Secreto de firma de los JWT. Cadena larga y aleatoria, minimo 32 caracteres |
| `JWT_EXPIRATION_MINUTES` | no | `60` | Vigencia del token emitido en el login |
| `SPRING_PROFILES_ACTIVE` | no | `prod` | Perfil Spring |
| `APP_TAX_RATE` | no | `0.16` | Tasa de impuesto aplicada al total de la orden (`app.tax-rate`) |

El esquema lo crea y evoluciona **Flyway** (`src/main/resources/db/migration/`), que corre automaticamente al arrancar la app. `spring.jpa.hibernate.ddl-auto=validate`, asi que Hibernate solo verifica que las entidades coincidan con la base y nunca la altera.

### Perfiles

- **`prod`** (el de esta rama): logging `INFO`, variables de entorno obligatorias sin default, y documentacion OpenAPI/Swagger deshabilitada.
- `dev` y `test` existen para desarrollo y para la suite de pruebas. Su configuracion y los datos de arranque que traen se documentan en la rama `dev`; nada de eso se activa en `prod`.

## Despliegue

### Con Docker Compose (app + base de datos)

`docker-compose.yml` levanta `postgres-cafeteria` (Postgres 16 con healthcheck y volumen persistente) y `app` (construida desde el `Dockerfile` multi-stage). El servicio `app` espera a que la BD este sana (`depends_on: condition: service_healthy`) antes de arrancar.

Copiar la plantilla de variables y ajustarla — como minimo, definir un `JWT_SECRET` real y credenciales de BD propias:

```bash
cp .env.example .env
```

Levantar:

```bash
docker compose up -d --build
```

La API queda en `http://localhost:8080`. Flyway aplica las migraciones al arrancar la app.

> Los valores de `.env.example` son una plantilla, no credenciales de produccion. Cambiar `JWT_SECRET`, `DB_USERNAME` y `DB_PASSWORD` antes de exponer el servicio. El archivo `.env` esta gitignoreado.

### Con el jar

El `Dockerfile` es multi-stage: compila con `eclipse-temurin:25-jdk-alpine` usando el Maven Wrapper y corre el fat jar sobre `eclipse-temurin:25-jre-alpine` como usuario no-root. Para desplegar el jar directamente hace falta un JDK 25:

```bash
./mvnw -DskipTests package

DB_URL=jdbc:postgresql://host:5432/cafeteria_db \
DB_USERNAME=... \
DB_PASSWORD=... \
JWT_SECRET=... \
java -jar target/coffeeshop-1.1.0.jar
```

### Primer administrador

Una base recien migrada por Flyway queda vacia, y en `prod` no se siembra ningun usuario. Como `POST /api/v1/users` exige rol `ADMIN`, hace falta un arranque en frio manual. Los unicos endpoints publicos son `/api/v1/auth/**` y `POST /api/v1/customers`, asi que el camino mas seguro es registrar el usuario por la API (para que la contrasena se cifre con el mismo `BCryptPasswordEncoder` que usa el login) y promoverlo despues en la base:

```bash
curl -X POST http://localhost:8080/api/v1/customers \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@tudominio.com","password":"<contrasena-fuerte>","firstName":"...","lastName":"..."}'
```

```sql
-- Promover ese usuario a ADMIN y quitarle el perfil de cliente.
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@tudominio.com';
DELETE FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'admin@tudominio.com');
```

A partir de ahi, ese usuario puede crear el resto de empleados y administradores via `POST /api/v1/employees`. Conviene cerrar el ciclo cuanto antes y no dejar el acceso a la base abierto.

## Integracion Continua

`.github/workflows/ci.yml` corre en cada push y pull request a `main`/`dev`: instala JDK 25 (Temurin), cachea `~/.m2` y ejecuta `./mvnw verify` (compila y corre toda la suite: unitarios con Mockito, de controlador con MockMvc y de integracion end-to-end con Testcontainers sobre Postgres real). Los runners de GitHub traen Docker, asi que Testcontainers funciona sin configuracion extra; para correr `verify` localmente hace falta un daemon de Docker disponible.

El pipeline compila y prueba, pero no publica imagen a un registry ni despliega.

## Estructura

```text
src/main/java/com/raze/coffeeshop
  config/          Configuracion (seguridad, OpenAPI, encoders)
  controller/      Controladores REST
  dto/             Requests y responses de la API
  enums/           Enums del dominio
  exception/       Excepciones y handler global
  model/           Entidades JPA
  repository/      Repositorios Spring Data
  security/        Filtro JWT y manejo de 401/403
  service/         Interfaces de servicios
  service/impl/    Implementaciones de servicios

src/main/resources
  application.yml           Configuracion base
  application-{dev,test,prod}.yml
  db/migration/             Migraciones Flyway (V1..V5)
```

## Autenticacion

```text
POST   /api/v1/auth/login
```

Request:

```json
{
  "email": "ana@example.com",
  "password": "SuperSecreta123"
}
```

Response (`200`):

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresInMinutes": 60,
  "role": "ADMIN"
}
```

El resto de la API (excepto `POST /api/v1/customers`, que sigue publico para auto-registro) requiere el header `Authorization: Bearer <token>`. Sin token responde `401`; con un rol sin permiso responde `403`. La sesion es stateless: no hay cookie ni estado en el servidor.

Autorizacion por rol:

| Recurso | Lectura | Escritura |
|---|---|---|
| Branches / Categories / Products / Ingredients | cualquier rol autenticado | `ADMIN`, `MANAGER` |
| Users | `ADMIN` | `ADMIN` |
| Employees | `ADMIN`, `MANAGER` | `ADMIN`, `MANAGER` |
| Customers | el propio usuario o `ADMIN`/`MANAGER` | alta publica (auto-registro); baja/edicion: el propio usuario o `ADMIN`/`MANAGER` |
| Orders | staff (`ADMIN`/`MANAGER`/`CASHIER`/`BARISTA`) o el `customer` dueno de esa orden | `ADMIN`, `MANAGER`, `CASHIER`, `BARISTA` |
| Recipes / Inventory | cualquier rol autenticado | `ADMIN`, `MANAGER` |

La lista de rutas publicas vive en un solo lugar (`PublicEndpoints`), que consumen tanto `SecurityConfig` como el generador de OpenAPI, para que la documentacion no pueda desincronizarse de lo que realmente se deja pasar sin token.

## Convenciones De La API

- **Versionado**: todos los endpoints cuelgan de `/api/v1/...`.
- **Paginacion**: los listados (`GET` de `branches`, `categories`, `products`, `ingredients`, `users`, `customers`, `employees`, `orders`) devuelven una pagina Spring Data (`{ "content": [...], "totalElements": ..., "totalPages": ..., "number": ..., "size": ... }`). Aceptan los parametros estandar `page` (0-based), `size` y `sort` (ej. `?page=0&size=20&sort=name,asc`).
- **Filtros**: `GET /api/v1/products?categoryId=1` filtra productos por categoria; `GET /api/v1/orders?status=PAID` filtra ordenes por estado. Ambos combinables con la paginacion.
- **Errores**: todas las respuestas de error usan el mismo esquema `ApiError` (`400` validacion / JSON malformado, `401`, `403`, `404`, `409` duplicados y conflictos de bloqueo optimista, `500`). Una excepcion no anticipada responde `500` sin exponer el mensaje ni el stacktrace real, que quedan en el log del servidor.
- **Borrado logico**: los `DELETE` de sucursales, categorias, productos, ingredientes, usuarios, clientes y empleados marcan `active=false`; no borran filas.
- **Dinero y tiempo**: `BigDecimal` para importes, `OffsetDateTime` para timestamps con zona.
- **OpenAPI/Swagger**: `GET /v3/api-docs` y `/swagger-ui.html` estan **deshabilitados en `prod`** (`springdoc.api-docs.enabled=false`), porque el documento describe la superficie completa de la API incluidos los endpoints de administracion. En `dev`/`test` se publican sin token. El documento se genera desde el codigo (descripciones del Javadoc via `therapi-runtime-javadoc`, roles leidos de `@PreAuthorize`, errores desde el contrato real del handler global), no desde anotaciones duplicadas.

## Endpoints

### Branches

```text
GET    /api/v1/branches
GET    /api/v1/branches/{id}
POST   /api/v1/branches
PUT    /api/v1/branches/{id}
DELETE /api/v1/branches/{id}
```

Request:

```json
{
  "name": "Sucursal Centro",
  "address": "Av. Principal 123",
  "city": "Ciudad de Mexico",
  "state": "CDMX"
}
```

`DELETE /api/v1/branches/{id}` desactiva la sucursal con `active=false`.

### Categories

```text
GET    /api/v1/categories
GET    /api/v1/categories/{id}
POST   /api/v1/categories
PUT    /api/v1/categories/{id}
DELETE /api/v1/categories/{id}
```

Request:

```json
{
  "name": "Coffee",
  "active": true
}
```

`DELETE /api/v1/categories/{id}` desactiva la categoria con `active=false`.

### Products

```text
GET    /api/v1/products
GET    /api/v1/products/{id}
POST   /api/v1/products
PUT    /api/v1/products/{id}
DELETE /api/v1/products/{id}
```

Request:

```json
{
  "name": "Latte",
  "basePrice": 55.00,
  "active": true,
  "categoryId": 1
}
```

`DELETE /api/v1/products/{id}` desactiva el producto con `active=false`.

### Ingredients

```text
GET    /api/v1/ingredients
GET    /api/v1/ingredients/{id}
POST   /api/v1/ingredients
PUT    /api/v1/ingredients/{id}
DELETE /api/v1/ingredients/{id}
```

Request:

```json
{
  "name": "Leche entera",
  "measureUnit": "ml"
}
```

`DELETE /api/v1/ingredients/{id}` desactiva el ingrediente con `active=false`.

### Users

```text
GET    /api/v1/users
GET    /api/v1/users/{id}
POST   /api/v1/users
PUT    /api/v1/users/{id}
DELETE /api/v1/users/{id}
```

Request:

```json
{
  "email": "ana@example.com",
  "password": "SuperSecreta123",
  "firstName": "Ana",
  "lastName": "Lopez",
  "role": "ADMIN"
}
```

Roles disponibles: `ADMIN`, `MANAGER`, `CASHIER`, `BARISTA`, `CUSTOMER`. La contrasena se cifra con `BCryptPasswordEncoder` antes de guardarse; nunca se almacena en texto plano ni se devuelve en las respuestas. `DELETE /api/v1/users/{id}` desactiva el usuario con `active=false`.

### Customers

```text
GET    /api/v1/customers
GET    /api/v1/customers/{userId}
POST   /api/v1/customers
PUT    /api/v1/customers/{userId}
DELETE /api/v1/customers/{userId}
```

Request de `POST` (registra el `user` con rol `CUSTOMER` y su perfil en un solo paso, en la misma transaccion, de modo que `user.role` siempre queda acorde al perfil):

```json
{
  "email": "ana@example.com",
  "password": "SuperSecreta123",
  "firstName": "Ana",
  "lastName": "Lopez",
  "loyaltyPoints": 0,
  "birthDate": "1998-05-12"
}
```

Request de `PUT` (solo datos del perfil, sin credenciales de login):

```json
{
  "loyaltyPoints": 10,
  "birthDate": "1998-05-12"
}
```

`POST /api/v1/customers` responde `409` si ya existe un `user` con ese correo. `DELETE /api/v1/customers/{userId}` desactiva el perfil con `active=false`.

### Employees

```text
GET    /api/v1/employees
GET    /api/v1/employees/{userId}
POST   /api/v1/employees
PUT    /api/v1/employees/{userId}
DELETE /api/v1/employees/{userId}
```

Request de `POST` (registra el `user` con el rol operativo indicado en `type` y su perfil en un solo paso):

```json
{
  "email": "carlos@example.com",
  "password": "SuperSecreta123",
  "firstName": "Carlos",
  "lastName": "Ruiz",
  "type": "BARISTA",
  "branchId": "7c2e5a10-4321-4f9c-9a1b-0987654321ba",
  "position": "Barista",
  "hireDate": "2026-01-15"
}
```

Request de `PUT` (solo datos del perfil y del rol operativo, sin credenciales de login; reasignar `type` sincroniza `user.role`):

```json
{
  "type": "BARISTA",
  "branchId": "7c2e5a10-4321-4f9c-9a1b-0987654321ba",
  "position": "Barista",
  "hireDate": "2026-01-15"
}
```

`type` acepta cualquier rol operativo (`ADMIN`, `MANAGER`, `CASHIER`, `BARISTA`); `CUSTOMER` se rechaza con `400` (esa alta va por `/api/v1/customers`). `POST /api/v1/employees` responde `409` si ya existe un `user` con ese correo y `404` si `branchId` no existe. `DELETE /api/v1/employees/{userId}` desactiva el perfil con `active=false`.

### Orders (flujo de ventas)

```text
GET    /api/v1/orders
GET    /api/v1/orders/{id}
POST   /api/v1/orders
POST   /api/v1/orders/{orderId}/items
DELETE /api/v1/orders/{orderId}/items/{itemId}
PATCH  /api/v1/orders/{orderId}/status
POST   /api/v1/orders/{orderId}/payments
```

Request de `POST /api/v1/orders` (crea la orden vacia en estado `PENDING`; `customerId` es opcional):

```json
{
  "branchId": "7c2e5a10-4321-4f9c-9a1b-0987654321ba",
  "employeeId": "a1b2c3d4-1111-2222-3333-444455556666",
  "customerId": null
}
```

Request de `POST /api/v1/orders/{orderId}/items` (agrega un producto; el precio se toma de `Product.basePrice` en ese instante y queda congelado en el item, sin importar que el precio del producto cambie despues; `quantity` acepta de 1 a 500):

```json
{
  "productId": "b2c3d4e5-2222-3333-4444-555566667777",
  "quantity": 2,
  "notes": "sin azucar"
}
```

Request de `PATCH /api/v1/orders/{orderId}/status` (transiciones manuales del staff; `PAID` nunca se setea aqui, solo la dispara un pago que cubre el total):

```json
{
  "status": "PREPARING"
}
```

Transiciones validas: `PENDING -> CANCELLED`, `PAID -> PREPARING`, `PAID -> CANCELLED`, `PREPARING -> DELIVERED`, `PREPARING -> CANCELLED`. `DELIVERED` y `CANCELLED` son terminales.

Request de `POST /api/v1/orders/{orderId}/payments` (admite varios pagos parciales con distinto metodo por orden; `amount` acepta hasta 8 digitos enteros y 2 decimales):

```json
{
  "method": "CASH",
  "amount": 100.00
}
```

Subtotal, impuestos (`app.tax-rate`, default `0.16`) y total se recalculan en cada alta/baja de item. Los items y el estado solo se pueden modificar mientras la orden este `PENDING`; cuando la suma de los pagos cubre el total, la orden pasa automaticamente a `PAID`. Un pago que exceda el saldo pendiente responde `400`. Si dos operaciones concurrentes chocan sobre la misma orden (bloqueo optimista via columna `version`), la que pierde la carrera responde `409` y debe reintentarse: dos pagos simultaneos nunca se pisan en silencio.

Al pasar la orden a `PAID`, se descuenta automaticamente el stock de ingredientes de la sucursal segun la receta de cada producto vendido (ver **Recipes** e **Inventory**); si algun ingrediente no tiene stock suficiente, el pago se rechaza con `400` y la transaccion completa se revierte (la orden sigue `PENDING`).

El flujo completo de ordenes y pagos deja rastro de auditoria en el log (`OrderServiceImpl`, via SLF4J): creacion de orden, alta/baja de items, cambios de estado, pagos registrados e intentos rechazados.

### Recipes (receta por producto)

```text
GET    /api/v1/products/{productId}/recipes
POST   /api/v1/products/{productId}/recipes
DELETE /api/v1/products/{productId}/recipes/{ingredientId}
```

Request de `POST` (agrega un ingrediente a la receta del producto, con la cantidad requerida por unidad vendida):

```json
{
  "ingredientId": "b2c3d4e5-2222-3333-4444-555566667777",
  "requiredQuantity": 150.000
}
```

Escritura restringida a `ADMIN`/`MANAGER`. `POST` responde `409` si el ingrediente ya esta en la receta, y `404` si el producto o el ingrediente no existen.

### Inventory (stock por sucursal)

```text
GET    /api/v1/branches/{branchId}/inventory
POST   /api/v1/branches/{branchId}/inventory/movements
GET    /api/v1/branches/{branchId}/inventory/{ingredientId}/movements
```

Request de `POST .../movements` (registra un movimiento manual de stock; el usuario que lo registra se toma del token):

```json
{
  "ingredientId": "b2c3d4e5-2222-3333-4444-555566667777",
  "type": "INCOMING",
  "quantity": 500.000,
  "reason": "Compra semanal"
}
```

`type` acepta `INCOMING` (suma stock), `WASTE` y `ADJUSTMENT` (restan stock). `SALE` se rechaza con `400`: esos movimientos solo los genera automaticamente el pago de una orden. Registrar movimientos esta restringido a `ADMIN`/`MANAGER`. Un `WASTE`/`ADJUSTMENT` que deje el stock negativo responde `400`.

## Modelo De Dominio

Tablas por area:

- Sucursales: `branches`
- Usuarios: `users`
- Clientes: `customers`
- Empleados: `employees`
- Catalogo: `categories`, `products`
- Recetas: `ingredients`, `recipes`
- Inventario: `branch_inventory`, `inventory_movements`
- Ventas: `orders`, `order_items`, `payments`

Enums PostgreSQL:

- `user_role`
- `order_status`
- `payment_method`
- `movement_type`

## Roadmap

- Publicar la imagen Docker a un registry (ghcr.io) y agregar un job de despliegue al pipeline.
- Refresh tokens / renovacion de JWT.
- Rate limiting en `POST /api/v1/auth/login`.
- Observabilidad: Actuator y metricas.
