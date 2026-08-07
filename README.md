# Coffee Shop API

> **Rama:** `dev` — Desarrollo (version `1.2.0-SNAPSHOT`, perfil Spring activo por defecto: `dev`).

Backend REST para gestionar una cafeteria. El proyecto esta construido con Spring Boot, PostgreSQL, JPA y Flyway. Cubre el dominio completo del negocio: catalogo (sucursales, categorias, productos, ingredientes), usuarios/clientes/empleados, flujo de ventas (ordenes y pagos) e inventario (recetas y stock por sucursal con descuento automatico al vender). La API esta versionada (`/api/v1`), autenticada con JWT y autorizada por rol, paginada, documentada con OpenAPI/Swagger y empaquetada con Docker + CI.

## Estado Actual

El proyecto ya tiene una base solida de dominio:

- Base de datos PostgreSQL definida con Flyway.
- Entidades JPA para usuarios, clientes, empleados, sucursales, categorias, productos, ingredientes, recetas, inventario, ordenes, items de orden y pagos.
- Repositorios Spring Data para todas las entidades principales.
- API REST funcional para `branches`, `categories`, `products`, `ingredients`, `users`, `customers` y `employees`.
- DTOs de entrada/salida para no exponer entidades JPA directamente en los controladores.
- Validacion con Jakarta Validation.
- Manejo global de errores para recursos no encontrados, duplicados y errores de validacion.
- Borrado logico (`active=false`) para sucursales, categorias, productos, ingredientes, usuarios, clientes y empleados.
- Contraseñas de usuario cifradas con `spring-security-crypto` (`PasswordEncoder`).
- `customers` y `employees` se registran en un solo paso: `POST /api/v1/customers` y `POST /api/v1/employees` crean el `user` (con clave compartida `user_id`) y su perfil en la misma transaccion, en vez de requerir un `userId` de un `user` creado antes por separado. Asi la invariante `user.role` acorde al perfil queda garantizada por construccion.
- Tests unitarios de servicios con Mockito para `Branch`, `Customer`, `Employee`, `Category`, `Product`, `Ingredient` y `User`.
- Tests de controlador con `MockMvc` para `branches`, `categories`, `products`, `ingredients`, `users`, `customers` y `employees`.
- Tests de integracion end-to-end con Testcontainers (Postgres real + Flyway) para `branches`, catalogo (`categories` + `products`), alta de usuarios (cifrado de contraseña, correo duplicado), registro de `customers`/`employees`, autenticacion/autorizacion por rol, ventas (orden -> items -> pagos), inventario y bloqueo optimista.

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
- Docker Compose para base de datos local

## Estructura

```text
src/main/java/com/raze/coffeeshop
  controller/      Controladores REST
  dto/             Requests y responses de la API
  enums/           Enums del dominio
  exception/       Excepciones y handler global
  model/           Entidades JPA
  repository/      Repositorios Spring Data
  service/         Interfaces de servicios
  service/impl/    Implementaciones de servicios

src/main/resources
  application.yml
  db/migration/V1__init_schema.sql

src/test/java/com/raze/coffeeshop
  controller/      Tests de controlador (MockMvc)
  integration/     Tests de integracion end-to-end (Testcontainers)
  service/impl/    Tests unitarios de servicios (Mockito)
```

## Configuracion

La aplicacion usa variables de entorno con valores por defecto para desarrollo local:

```yaml
DB_URL: jdbc:postgresql://localhost:5432/cafeteria_db
DB_USERNAME: root
DB_PASSWORD: rootpassword
```

El puerto sale de `PORT` (default `8080`), que es la variable con la que los PaaS asignan el puerto en tiempo de arranque.

`/actuator/health` queda publico y sin detalle (`{"status":"UP"}`): lo consulta el host sin credenciales para decidir si enruta trafico a la instancia, y mientras Flyway migra responde `503`. Es el unico endpoint de Actuator publicado — `management.endpoints.web.exposure.include` lo limita explicitamente para que el starter no publique de paso `env`, `beans` o `mappings`.

`application.yml` usa `spring.jpa.hibernate.ddl-auto=validate`, por lo que Hibernate solo valida que las entidades coincidan con la base. El esquema se crea y evoluciona con Flyway.

Perfiles Spring (se elige con `SPRING_PROFILES_ACTIVE`):

- `dev` (default en la rama `dev`): logging verboso (`DEBUG` + SQL), secreto JWT de conveniencia por defecto, documentacion OpenAPI publicada y siembra del empleado inicial (ver abajo).
- `test`: se activa automaticamente al correr `mvnw test` (via surefire); logging minimo y secreto JWT de prueba. El datasource lo aporta Testcontainers.
- `prod` (default en la rama `main`): logging `INFO`, y `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` **obligatorias sin default** — la app no arranca si faltan (evita usar credenciales de desarrollo por accidente). `JWT_SECRET` tambien es obligatoria.

### Empleado inicial (arranque en frio)

Una base recien migrada por Flyway queda vacia y no hay forma de entrar: `POST /api/v1/auth/login` necesita un `user` existente y el resto de la API exige token. El unico endpoint publico crea un `CUSTOMER`, que no puede operar el front de personal, y el punto de venta manda el uid de la sesion como `OrderRequest.employeeId` — asi que la primera cuenta tiene que ser un **empleado con sucursal**, no un usuario `ADMIN` suelto.

`BootstrapSeeder` resuelve ese arranque en frio: crea una sucursal y un empleado con rol `ADMIN` si no existen todavia. No depende del perfil sino de `app.seed.enabled`, que en `dev` viene encendido y en cualquier otro entorno hay que pedir a proposito.

| Campo | Valor por defecto en `dev` | Variable de entorno |
|---|---|---|
| Correo | `raze.armando@gmail.com` | `APP_SEED_EMPLOYEE_EMAIL` |
| Contrasena | `password` | `APP_SEED_EMPLOYEE_PASSWORD` |
| Rol | `ADMIN` | — (siempre `ADMIN`) |
| Sucursal | `Sucursal Centro` | `APP_SEED_BRANCH_NAME` |

El seeder es idempotente: si ya existe un usuario con ese correo no toca nada, asi que arrancar la app varias veces (o cambiar la contrasena despues) es seguro. Correo y contrasena son los unicos valores sin default: si `app.seed.enabled=true` y falta alguno, la app **no arranca**, en vez de crear un acceso adivinable.

En produccion el procedimiento es encenderlo para el primer despliegue, entrar, cambiar la contrasena y volver a apagarlo (ver [Despliegue](#despliegue)). Asi las credenciales iniciales viajan por variables de entorno y no quedan versionadas en una migracion de Flyway.

### Datos de prueba (solo perfil `dev`)

`db/seed/V900__seed_dev_data.sql` deja la base lista para ejecutar una compra completa sin cargar nada a mano: dos sucursales, tres categorias, ocho ingredientes, siete productos con sus recetas, stock por sucursal, usuarios de cada rol y una orden ya pagada para que los listados no arranquen vacios.

Es una migracion Flyway normal, pero vive en `db/seed/` y no en `db/migration/`: **solo el perfil `dev` incluye esa location** (`spring.flyway.locations` en `application-dev.yml`), asi que los datos de demostracion no pueden aplicarse en `prod` aunque la rama se mergee. Numera desde `V900` para no competir nunca con la numeracion del esquema, y `dev` activa `spring.flyway.out-of-order` para que agregar un `V6` mas adelante no falle en una base que ya tiene el seed.

Usuarios sembrados (contrasena `password` en todos):

| Correo | Rol | Notas |
|---|---|---|
| `gerente@coffeeshop.dev` | `MANAGER` | Empleado en Sucursal Centro |
| `cajero@coffeeshop.dev` | `CASHIER` | Empleado en Sucursal Centro; es quien vende en el ejemplo |
| `barista@coffeeshop.dev` | `BARISTA` | Empleado en Sucursal Centro |
| `cliente@coffeeshop.dev` | `CUSTOMER` | Perfil de cliente con 120 puntos |

Los UUID son fijos y legibles para pegarlos directo en curl o Postman:

```text
Sucursal Centro   11111111-1111-1111-1111-111111111111
Sucursal Norte    11111111-1111-1111-1111-222222222222
Cajero            44444444-0000-0000-0000-000000000002
Cliente           44444444-0000-0000-0000-000000000004
Latte             33333333-0000-0000-0000-000000000003
Croissant         33333333-0000-0000-0000-000000000007
```

Compra de punta a punta: login como cajero, crear la orden, agregar items, pagar. Al cubrirse el total la orden pasa a `PAID` y se descuenta el inventario segun las recetas.

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"cajero@coffeeshop.dev","password":"password"}' | jq -r .token)

ORDER=$(curl -s -X POST localhost:8080/api/v1/orders -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"branchId":"11111111-1111-1111-1111-111111111111","employeeId":"44444444-0000-0000-0000-000000000002","customerId":"44444444-0000-0000-0000-000000000004"}' | jq -r .id)

curl -s -X POST localhost:8080/api/v1/orders/$ORDER/items -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"productId":"33333333-0000-0000-0000-000000000003","quantity":3}'   # 3 Latte -> total 191.40

curl -s -X POST localhost:8080/api/v1/orders/$ORDER/payments -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"method":"CASH","amount":100.00}'   # parcial: sigue PENDING

curl -s -X POST localhost:8080/api/v1/orders/$ORDER/payments -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"method":"CARD","amount":91.40}'    # cubre el total: pasa a PAID
```

Dos casos limite quedan sembrados a proposito para probarlos sin preparar nada:

- El **Croissant no tiene receta**: se vende bien y no mueve inventario, que es el comportamiento correcto para un producto que no se arma con ingredientes registrados.
- **Sucursal Norte tiene solo 10 g de chocolate** y un Mocha consume 20 g. Pagar dos Mocha ahi responde `400` por stock insuficiente, la orden se queda en `PENDING` y no se descuenta nada: sirve para comprobar que el pago revierte la transaccion completa.

## Docker

`docker-compose.yml` levanta dos servicios: `postgres-cafeteria` (Postgres 16, con healthcheck) y `app` (la aplicacion, construida desde el `Dockerfile` multi-stage). El servicio `app` espera a que la BD este sana (`depends_on: condition: service_healthy`) antes de arrancar, y tiene su propio healthcheck contra `/actuator/health` con `start_period` holgado para no marcarse `unhealthy` mientras la JVM levanta y Flyway migra.

Copiar la plantilla de variables y ajustarla (define `JWT_SECRET`, credenciales de BD, perfil):

```bash
cp .env.example .env
```

Levantar todo (app + base de datos):

```bash
docker compose up -d --build
```

La API queda en `http://localhost:8080` (Swagger en `http://localhost:8080/swagger-ui.html`). Flyway aplica las migraciones de `src/main/resources/db/migration/` al arrancar la app.

Para levantar solo la base de datos (p. ej. corriendo la app desde el IDE):

```bash
docker compose up -d postgres-cafeteria
```

El `Dockerfile` es multi-stage: compila con `eclipse-temurin:25-jdk-alpine` usando el Maven Wrapper y corre el fat jar sobre `eclipse-temurin:25-jre-alpine` como usuario no-root.

## Despliegue

La imagen no necesita nada especial del host: basta un PaaS que construya el `Dockerfile`, inyecte `PORT` y ofrezca un Postgres gestionado. Flyway aplica las migraciones al arrancar, asi que no hay paso manual de esquema.

Variables que hay que definir en el host:

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | `jdbc:postgresql://HOST:PUERTO/BASE` (formato JDBC, no la `postgres://` que suelen dar los PaaS) |
| `DB_USERNAME` / `DB_PASSWORD` | credenciales del Postgres gestionado |
| `JWT_SECRET` | cadena larga y aleatoria: `openssl rand -base64 48` |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75` para que la JVM respete el limite del contenedor |

En Railway las credenciales se referencian desde el servicio de Postgres sin copiarlas a mano:

```
DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DB_USERNAME=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
```

Configurar el health check del servicio en `/actuator/health`.

**Primer despliegue (arranque en frio).** La base recien migrada no tiene con quien hacer login, asi que solo la primera vez se agregan:

```
APP_SEED_ENABLED=true
APP_SEED_EMPLOYEE_EMAIL=<correo real>
APP_SEED_EMPLOYEE_PASSWORD=<clave temporal fuerte>
```

Tras el arranque, verificar y cerrar:

```bash
curl https://<dominio>/actuator/health          # {"status":"UP"}
curl -X POST https://<dominio>/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"<correo real>","password":"<clave temporal>"}'
```

Con el token, cambiar la contrasena; despues poner `APP_SEED_ENABLED=false` y redesplegar. En `prod` la documentacion OpenAPI esta deshabilitada a proposito, asi que `/v3/api-docs` y Swagger UI no responden.

> Si el host usa red privada solo-IPv6 (es el caso de Railway) y la app no logra conectar a Postgres, agregar `-Djava.net.preferIPv6Addresses=true` a `JAVA_TOOL_OPTIONS`: la JVM prefiere IPv4 por defecto.

## Integracion Continua

`.github/workflows/ci.yml` corre en cada push y pull request a `main`/`dev`: instala JDK 25 (Temurin), cachea `~/.m2` y ejecuta `./mvnw verify` (compila y corre toda la suite). Los runners de GitHub traen Docker, asi que los tests de integracion con Testcontainers funcionan sin configuracion extra.

## Ejecutar

En Windows, con JDK 25. La ruta de `JAVA_HOME` depende de donde este instalado el JDK en cada maquina; usa la que corresponda:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot'  # instalacion estandar de Eclipse Adoptium
$env:JAVA_HOME='D:\new\jdk'                                              # ruta usada en esta maquina
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd spring-boot:run
```

Ejecutar pruebas:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot'  # instalacion estandar de Eclipse Adoptium
$env:JAVA_HOME='D:\new\jdk'                                              # ruta usada en esta maquina
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

> Los tests de integracion (`src/test/java/com/raze/coffeeshop/integration/`) usan Testcontainers y necesitan un daemon de Docker disponible localmente o en CI.

Empaquetar sin pruebas:

```powershell
.\mvnw.cmd -DskipTests package
```

## Endpoints Actuales

### Auth

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

El resto de la API (excepto `POST /api/v1/customers`, que sigue publico para auto-registro) requiere el header `Authorization: Bearer <token>`. Sin token responde `401`; con un rol sin permiso responde `403`. Autorizacion por rol:

| Recurso | Lectura | Escritura |
|---|---|---|
| Branches / Categories / Products / Ingredients | cualquier rol autenticado | `ADMIN`, `MANAGER` |
| Users | `ADMIN` | `ADMIN` |
| Employees | `ADMIN`, `MANAGER` | `ADMIN`, `MANAGER` |
| Customers | el propio usuario o `ADMIN`/`MANAGER` | alta publica (auto-registro); baja/edicion: el propio usuario o `ADMIN`/`MANAGER` |
| Orders | staff (`ADMIN`/`MANAGER`/`CASHIER`/`BARISTA`) o el `customer` dueno de esa orden | `ADMIN`, `MANAGER`, `CASHIER`, `BARISTA` |
| Recipes / Inventory | cualquier rol autenticado | `ADMIN`, `MANAGER` |

Variables de entorno: `JWT_SECRET` (obligatoria en el perfil `prod`; la app no arranca si falta), `JWT_EXPIRATION_MINUTES` (opcional, default `60`).

### Paginacion, filtros y documentacion

- **Versionado**: todos los endpoints cuelgan de `/api/v1/...`.
- **Paginacion**: los listados (`GET` de `branches`, `categories`, `products`, `ingredients`, `users`, `customers`, `employees`, `orders`) devuelven una pagina Spring Data (`{ "content": [...], "totalElements": ..., "totalPages": ..., "number": ..., "size": ... }`). Aceptan los parametros estandar `page` (0-based), `size` y `sort` (ej. `?page=0&size=20&sort=name,asc`).
- **Filtros**: `GET /api/v1/products?categoryId=1` filtra productos por categoria; `GET /api/v1/orders?status=PAID` filtra ordenes por estado. Ambos combinables con la paginacion.
- **OpenAPI/Swagger**: `GET /v3/api-docs` (JSON) y `/swagger-ui.html` (UI interactiva), publicos (sin token) en `dev`/`test` y **deshabilitados en `prod`** (`springdoc.api-docs.enabled=false`), porque el documento describe la superficie completa de la API incluidos los endpoints de administracion. La UI incluye el boton "Authorize" para pegar el JWT y probar los endpoints protegidos.
- **Contenido del documento**: las descripciones de cada endpoint salen del Javadoc de controllers y DTOs (via `therapi-runtime-javadoc`), sin duplicarlo en anotaciones `@Operation`/`@Schema`. Ademas se generan automaticamente: los roles que exige cada operacion (leidos de `@PreAuthorize`), las respuestas de error con el esquema `ApiError` (`400`/`404`/`409`/`500`, mas `401`/`403` en los endpoints protegidos) y los parametros `page`/`size`/`sort` de los listados (`@ParameterObject Pageable`). Los endpoints publicos aparecen sin candado porque `SecurityConfig` y Swagger leen la misma lista (`PublicEndpoints`).

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

Roles disponibles: `ADMIN`, `MANAGER`, `CASHIER`, `BARISTA`, `CUSTOMER`. La contraseña se cifra con `PasswordEncoder` antes de guardarse. `DELETE /api/v1/users/{id}` desactiva el usuario con `active=false`.

### Customers

```text
GET    /api/v1/customers
GET    /api/v1/customers/{userId}
POST   /api/v1/customers
PUT    /api/v1/customers/{userId}
DELETE /api/v1/customers/{userId}
```

Request de `POST` (registra el `user` con rol `CUSTOMER` y su perfil en un solo paso):

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

Subtotal, impuestos (`app.tax-rate`, default `0.16`) y total se recalculan en cada alta/baja de item. Los items y el estado solo se pueden modificar mientras la orden este `PENDING`; cuando la suma de los pagos cubre el total, la orden pasa automaticamente a `PAID`. Un pago que exceda el saldo pendiente responde `400`. Si dos operaciones concurrentes chocan sobre la misma orden (bloqueo optimista via `version`), la que pierde la carrera responde `409` y debe reintentarse.

Al pasar la orden a `PAID`, se descuenta automaticamente el stock de ingredientes de la sucursal segun la receta de cada producto vendido (ver **Recipes** e **Inventory**); si algun ingrediente no tiene stock suficiente, el pago se rechaza con `400` y la transaccion completa se revierte (la orden sigue `PENDING`).

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

La migracion inicial define estas areas:

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

## Analisis Actual

### Fortalezas

- Buen uso de Flyway como fuente de verdad del esquema.
- `ddl-auto=validate` evita que Hibernate modifique la base accidentalmente.
- El modelo ya cubre la mayor parte del negocio de una cafeteria.
- Los CRUDs existentes usan DTOs y validaciones.
- El manejo global de errores ya da respuestas HTTP mas limpias.
- Se usa `BigDecimal` para dinero y `OffsetDateTime` para timestamps con zona.
- Borrado logico consistente (`active=false`) en todas las entidades con CRUD expuesto.
- Contraseñas cifradas con `spring-security-crypto`, nunca se guardan en texto plano.
- Cobertura de tests unitarios, de controlador y de integracion (Testcontainers) para todos los CRUDs expuestos hasta ahora.
- El alta de `customers` y `employees` registra el `user` en el mismo paso, eliminando por construccion el riesgo de un `user.role` desalineado con el perfil (ya no se referencia un `userId` de entrada que se pudiera desalinear).
- `Order` usa bloqueo optimista (columna `version`): dos escrituras concurrentes sobre la misma orden (dos pagos, o un pago y un cambio de estado) nunca se pisan en silencio, la segunda recibe `409 Conflict` para reintentar.
- `GlobalExceptionHandler` tambien cubre violaciones de integridad de datos, conflictos de bloqueo optimista y JSON malformado con el mismo esquema `ApiError`; cualquier excepcion no anticipada responde `500` sin exponer el mensaje/stacktrace real (que si queda en el log del servidor).
- El flujo de ordenes/pagos registra logging de auditoria (`OrderServiceImpl`, via SLF4J): creacion de orden, alta/baja de items, cambios de estado, pagos registrados e intentos rechazados.
- Autenticacion/autorizacion real con Spring Security + JWT sin estado (`POST /api/v1/auth/login`); todos los endpoints previos ahora exigen rol via `@PreAuthorize`/`@PostAuthorize`, con reglas de "dueño del recurso" para `customers` y `orders`.
- Inventario completo: receta (bill of materials) por producto, stock por sucursal con movimientos auditables (`INCOMING`/`WASTE`/`ADJUSTMENT`), y descuento automatico de ingredientes al pagar una orden (movimiento `SALE`), todo dentro de la misma transaccion del pago: si el stock no alcanza, el pago se revierte por completo.
- API versionada (`/api/v1`), listados paginados (Spring Data `Page` con `page`/`size`/`sort`), filtros (`products?categoryId`, `orders?status`) y documentacion OpenAPI/Swagger (`/swagger-ui.html`, `/v3/api-docs`) con el esquema de seguridad JWT ya declarado.
- El documento OpenAPI se genera a partir del codigo y no de anotaciones duplicadas: descripciones desde el Javadoc, roles desde `@PreAuthorize`, errores desde el contrato real de `GlobalExceptionHandler` y rutas publicas desde la misma lista que usa `SecurityConfig` (`PublicEndpoints`), asi la documentacion no se desincroniza del comportamiento. En `prod` la documentacion no se publica.
- Entrega lista: `Dockerfile` multi-stage (Temurin 25, usuario no-root), `docker-compose` con app + Postgres (healthcheck + `depends_on`), y CI en GitHub Actions que corre `./mvnw verify` (incluye Testcontainers) en cada push/PR.

### Riesgos Y Deuda Tecnica

- El pipeline de CI compila y prueba, pero no publica la imagen a ningun registry ni despliega (fuera de alcance por ahora).
- El JWT dura 60 minutos y no hay refresh: en una jornada larga la sesion del punto de venta expira a media venta y hay que volver a entrar.

## Siguientes Pasos Recomendados

El roadmap de "calidad de produccion" (seguridad, inventario, API publica, configuracion por perfil y entrega) esta completo. Mejoras opcionales a futuro:

- Publicar la imagen Docker a un registry (ghcr.io) y agregar un job de despliegue al pipeline.
- Refresh tokens / expiracion-renovacion de JWT y rate limiting en el login.
- Ampliar la observabilidad: hoy Actuator solo publica `health`; faltan metricas (Micrometer) y trazas.
