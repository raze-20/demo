# Coffee Demo API

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
- `customers` y `employees` extienden un `user` existente mediante clave compartida (`user_id`).
- Tests unitarios de servicios (`Branch`, `Customer`, `Employee`) con Mockito.
- Tests de controlador con `MockMvc` para `branches`.
- Test de integracion end-to-end para `branches` con Testcontainers (Postgres real + Flyway).

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

En Windows, con JDK 25:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd spring-boot:run
```

Ejecutar pruebas:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot'
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

Request:

```json
{
  "userId": "b3f1c9d0-1234-4a2b-8b3e-0a1234567890",
  "loyaltyPoints": 0,
  "birthDate": "1998-05-12"
}
```

Crea un perfil de cliente para un `user` ya existente (`userId`). `DELETE /api/customers/{userId}` desactiva el perfil con `active=false`.

### Employees

```text
GET    /api/employees
GET    /api/employees/{userId}
POST   /api/employees
PUT    /api/employees/{userId}
DELETE /api/employees/{userId}
```

Request:

```json
{
  "userId": "b3f1c9d0-1234-4a2b-8b3e-0a1234567890",
  "branchId": "7c2e5a10-4321-4f9c-9a1b-0987654321ba",
  "position": "Barista",
  "role": "BARISTA",
  "hireDate": "2026-01-15"
}
```

Crea un perfil de empleado para un `user` ya existente, asociado a una `branch`. `DELETE /api/employees/{userId}` desactiva el perfil con `active=false`.

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
- Ya existen tests unitarios, de controlador y de integracion (Testcontainers) como referencia para el resto del proyecto.

### Riesgos Y Deuda Tecnica

- La cobertura de tests todavia es parcial: solo `Branch`, `Customer` y `Employee` tienen tests unitarios, y solo `Branch` tiene test de controlador e integracion.
- No hay endpoints para recetas, inventario, ordenes ni pagos.
- No hay autenticacion/autorizacion real (login, JWT o sesiones); solo se cifran contraseñas.
- No hay paginacion, filtros ni ordenamiento en listados.
- No hay perfiles separados para `dev`, `test` y `prod`.
- Los tests de integracion dependen de Docker disponible (Testcontainers); hay que asegurarlo en CI.

## Siguientes Pasos Recomendados

1. Completar cobertura de tests
   - Tests unitarios de servicios para `Category`, `Product`, `Ingredient` y `User`.
   - Tests de controlador (`MockMvc`) para el resto de los endpoints.
   - Tests de integracion con Testcontainers para los flujos criticos.

2. Completar flujo de ventas
   - Crear orden.
   - Agregar items.
   - Calcular subtotal, impuestos y total.
   - Registrar pago.
   - Cambiar estado de orden.

3. Implementar inventario
   - Recetas por producto.
   - Stock por sucursal.
   - Movimientos de inventario.
   - Descuento automatico de ingredientes al vender productos.

4. Agregar seguridad
   - Spring Security.
   - Login.
   - JWT o sesiones.
   - Autorizacion por rol: `ADMIN`, `MANAGER`, `CASHIER`, `BARISTA`, `CUSTOMER`.

5. Mejorar API publica
   - Paginacion en `GET`.
   - Filtros por `active`, categoria, sucursal o nombre.
   - OpenAPI/Swagger.
   - Versionado de API (`/api/v1/...`).

6. Separar configuraciones
   - `application-dev.yml`
   - `application-test.yml`
   - `application-prod.yml`
   - Variables de entorno obligatorias para produccion.

7. Preparar entrega
   - Dockerfile para la aplicacion.
   - Compose completo con app + database.
   - Configurar GitHub Actions (CI/CD) para compilar y correr pruebas en cada push.

## Prioridad Sugerida

El proximo paso mas valioso es cerrar la brecha de tests en los CRUDs que aun no tienen cobertura (`Category`, `Product`, `Ingredient`, `User`). Despues de eso, conviene construir el flujo de ordenes y pagos, porque es el centro del negocio.
