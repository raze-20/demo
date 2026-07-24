# Coffee Demo API

> **Rama:** `main` — Produccion (perfil Spring activo por defecto: `prod`).

Backend REST para gestionar una cafeteria. El proyecto esta construido con Spring Boot, PostgreSQL, JPA y Flyway. Actualmente cubre el modelo de datos principal y expone CRUDs para sucursales, categorias, productos, ingredientes, usuarios, clientes y empleados.

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
- `customers` y `employees` se registran en un solo paso: `POST /api/customers` y `POST /api/employees` crean el `user` (con clave compartida `user_id`) y su perfil en la misma transaccion, en vez de requerir un `userId` de un `user` creado antes por separado. Asi la invariante `user.role` acorde al perfil queda garantizada por construccion.
- Tests unitarios de servicios con Mockito para `Branch`, `Customer`, `Employee`, `Category`, `Product`, `Ingredient` y `User`.
- Tests de controlador con `MockMvc` para `branches`, `categories`, `products`, `ingredients`, `users`, `customers` y `employees`.
- Tests de integracion end-to-end con Testcontainers (Postgres real + Flyway) para `branches`, y para los flujos criticos de catalogo (`categories` + `products`) y alta de usuarios (cifrado de contraseña, correo duplicado).

## Stack Tecnico

- Java 25
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- PostgreSQL 16
- Flyway
- Lombok
- Jakarta Validation
- Spring Security Crypto (hashing de contraseñas)
- Testcontainers (tests de integracion)
- Maven Wrapper
- Docker Compose para base de datos local

## Estructura

```text
src/main/java/com/raze/demo
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

src/test/java/com/raze/demo
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

`application.yml` usa `spring.jpa.hibernate.ddl-auto=validate`, por lo que Hibernate solo valida que las entidades coincidan con la base. El esquema se crea y evoluciona con Flyway.

## Base De Datos

Levantar PostgreSQL:

```powershell
docker compose up -d
```

El archivo `docker-compose.yml` crea:

- Contenedor: `db_cafeteria`
- Base: `cafeteria_db`
- Usuario: `root`
- Password: `rootpassword`
- Puerto: `5432`

Flyway ejecuta `src/main/resources/db/migration/V1__init_schema.sql` al iniciar la aplicacion.

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

> El test de integracion (`BranchIntegrationTest`) usa Testcontainers y necesita un daemon de Docker disponible localmente o en CI.

Empaquetar sin pruebas:

```powershell
.\mvnw.cmd -DskipTests package
```

## Endpoints Actuales

### Branches

```text
GET    /api/branches
GET    /api/branches/{id}
POST   /api/branches
PUT    /api/branches/{id}
DELETE /api/branches/{id}
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

`DELETE /api/branches/{id}` desactiva la sucursal con `active=false`.

### Categories

```text
GET    /api/categories
GET    /api/categories/{id}
POST   /api/categories
PUT    /api/categories/{id}
DELETE /api/categories/{id}
```

Request:

```json
{
  "name": "Coffee",
  "active": true
}
```

`DELETE /api/categories/{id}` desactiva la categoria con `active=false`.

### Products

```text
GET    /api/products
GET    /api/products/{id}
POST   /api/products
PUT    /api/products/{id}
DELETE /api/products/{id}
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

`DELETE /api/products/{id}` desactiva el producto con `active=false`.

### Ingredients

```text
GET    /api/ingredients
GET    /api/ingredients/{id}
POST   /api/ingredients
PUT    /api/ingredients/{id}
DELETE /api/ingredients/{id}
```

Request:

```json
{
  "name": "Leche entera",
  "measureUnit": "ml"
}
```

`DELETE /api/ingredients/{id}` desactiva el ingrediente con `active=false`.

### Users

```text
GET    /api/users
GET    /api/users/{id}
POST   /api/users
PUT    /api/users/{id}
DELETE /api/users/{id}
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

Roles disponibles: `ADMIN`, `MANAGER`, `CASHIER`, `BARISTA`, `CUSTOMER`. La contraseña se cifra con `PasswordEncoder` antes de guardarse. `DELETE /api/users/{id}` desactiva el usuario con `active=false`.

### Customers

```text
GET    /api/customers
GET    /api/customers/{userId}
POST   /api/customers
PUT    /api/customers/{userId}
DELETE /api/customers/{userId}
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

`POST /api/customers` responde `409` si ya existe un `user` con ese correo. `DELETE /api/customers/{userId}` desactiva el perfil con `active=false`.

### Employees

```text
GET    /api/employees
GET    /api/employees/{userId}
POST   /api/employees
PUT    /api/employees/{userId}
DELETE /api/employees/{userId}
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

`type` acepta cualquier rol operativo (`ADMIN`, `MANAGER`, `CASHIER`, `BARISTA`); `CUSTOMER` se rechaza con `400` (esa alta va por `/api/customers`). `POST /api/employees` responde `409` si ya existe un `user` con ese correo y `404` si `branchId` no existe. `DELETE /api/employees/{userId}` desactiva el perfil con `active=false`.

### Orders (flujo de ventas)

```text
GET    /api/orders
GET    /api/orders/{id}
POST   /api/orders
POST   /api/orders/{orderId}/items
DELETE /api/orders/{orderId}/items/{itemId}
PATCH  /api/orders/{orderId}/status
POST   /api/orders/{orderId}/payments
```

Request de `POST /api/orders` (crea la orden vacia en estado `PENDING`; `customerId` es opcional):

```json
{
  "branchId": "7c2e5a10-4321-4f9c-9a1b-0987654321ba",
  "employeeId": "a1b2c3d4-1111-2222-3333-444455556666",
  "customerId": null
}
```

Request de `POST /api/orders/{orderId}/items` (agrega un producto; el precio se toma de `Product.basePrice` en ese instante y queda congelado en el item, sin importar que el precio del producto cambie despues; `quantity` acepta de 1 a 500):

```json
{
  "productId": "b2c3d4e5-2222-3333-4444-555566667777",
  "quantity": 2,
  "notes": "sin azucar"
}
```

Request de `PATCH /api/orders/{orderId}/status` (transiciones manuales del staff; `PAID` nunca se setea aqui, solo la dispara un pago que cubre el total):

```json
{
  "status": "PREPARING"
}
```

Transiciones validas: `PENDING -> CANCELLED`, `PAID -> PREPARING`, `PAID -> CANCELLED`, `PREPARING -> DELIVERED`, `PREPARING -> CANCELLED`. `DELIVERED` y `CANCELLED` son terminales.

Request de `POST /api/orders/{orderId}/payments` (admite varios pagos parciales con distinto metodo por orden; `amount` acepta hasta 8 digitos enteros y 2 decimales):

```json
{
  "method": "CASH",
  "amount": 100.00
}
```

Subtotal, impuestos (`app.tax-rate`, default `0.16`) y total se recalculan en cada alta/baja de item. Los items y el estado solo se pueden modificar mientras la orden este `PENDING`; cuando la suma de los pagos cubre el total, la orden pasa automaticamente a `PAID`. Un pago que exceda el saldo pendiente responde `400`. Si dos operaciones concurrentes chocan sobre la misma orden (bloqueo optimista via `version`), la que pierde la carrera responde `409` y debe reintentarse.

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

### Riesgos Y Deuda Tecnica

- No hay endpoints para recetas ni inventario.
- No hay autenticacion/autorizacion real (login, JWT o sesiones); solo se cifran contraseñas.
- No hay paginacion, filtros ni ordenamiento en listados.
- No hay perfiles separados para `dev`, `test` y `prod`.
- Los tests de integracion dependen de Docker disponible (Testcontainers); hay que asegurarlo en CI.
- Falta un test de integracion (Testcontainers) para el flujo de registro de `customers`/`employees`; hoy solo esta cubierto con unitarios (Mockito) y de controlador (MockMvc).

## Siguientes Pasos Recomendados

1. Implementar inventario
   - Recetas por producto.
   - Stock por sucursal.
   - Movimientos de inventario.
   - Descuento automatico de ingredientes al vender productos (ya existe el flujo de ventas en `/api/orders` que dispararia este descuento).

2. Agregar seguridad
   - Spring Security.
   - Login.
   - JWT o sesiones.
   - Autorizacion por rol: `ADMIN`, `MANAGER`, `CASHIER`, `BARISTA`, `CUSTOMER`.

4. Mejorar API publica
   - Paginacion en `GET`.
   - Filtros por `active`, categoria, sucursal o nombre.
   - OpenAPI/Swagger.
   - Versionado de API (`/api/v1/...`).

5. Separar configuraciones
   - `application-dev.yml`
   - `application-test.yml`
   - `application-prod.yml`
   - Variables de entorno obligatorias para produccion.

6. Preparar entrega
   - Dockerfile para la aplicacion.
   - Compose completo con app + database.
   - Configurar GitHub Actions (CI/CD) para compilar y correr pruebas en cada push.

## Prioridad Sugerida

Con la brecha de tests en los CRUDs ya cerrada (`Category`, `Product`, `Ingredient`, `User` con tests unitarios y de controlador; flujos criticos de catalogo y alta de usuarios con tests de integracion) y con el registro de `customers`/`employees` resuelto de raiz (ya no depende de un `userId` externo, por lo que la invariante `user.role` es imposible de romper), el proximo paso mas valioso es construir el flujo de ordenes y pagos, porque es el centro del negocio. Antes de eso, conviene cubrir el nuevo flujo de registro con un test de integracion (Testcontainers) end-to-end.
