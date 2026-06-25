# Coffee Demo API

Esta es una aplicación Spring Boot que proporciona una API RESTful para la gestión de una cafetería/restaurante, incluyendo productos, categorías, órdenes, clientes, inventario de sucursales y más.

## Tecnologías Utilizadas

- **Java 17+**
- **Spring Boot 3.x**
- **Spring Web**
- **Spring Data JPA**
- **PostgreSQL**
- **Flyway** (Migraciones de base de datos)
- **Lombok**
- **Docker & Docker Compose**

## Estructura del Proyecto

El proyecto sigue una arquitectura clásica de capas para aplicaciones Spring Boot:

- `controller`: Expone los endpoints REST y maneja la comunicación HTTP.
- `service`: Contiene la lógica de negocio de la aplicación.
- `repository`: Maneja la comunicación con la base de datos a través de Spring Data JPA.
- `model`: Entidades JPA que se mapean a las tablas de la base de datos.
- `dto`: Objetos de Transferencia de Datos (Data Transfer Objects) para la comunicación entre cliente y servidor.
- `enums`: Enumeraciones usadas en varios modelos.
- `exception`: Manejo de excepciones globales (`@ControllerAdvice`) y personalizadas.

## Cómo ejecutar el proyecto

### Prerrequisitos

- Docker y Docker Compose instalados.
- Java 17 instalado (si deseas correrlo localmente sin Docker para la app).

### Ejecutar Base de Datos con Docker

Se incluye un archivo `docker-compose.yml` que levanta la instancia de PostgreSQL.

```bash
docker-compose up -d
```

### Ejecutar la Aplicación

El proyecto incluye el wrapper de Maven (`mvnw`), por lo que no es necesario tener Maven instalado globalmente.

```bash
# En Windows (Powershell / CMD)
.\mvnw.cmd spring-boot:run

# En Linux / Mac
./mvnw spring-boot:run
```

Las migraciones de Flyway (en `src/main/resources/db/migration/V1__init_schema.sql`) se ejecutarán automáticamente al iniciar la aplicación, creando el esquema de base de datos necesario.

## Resumen de la API

Actualmente existen varios controladores, como `CategoryController` y `ProductController`, que exponen las siguientes operaciones estándar (CRUD):

- `GET /api/categories` - Lista de categorías
- `GET /api/categories/{id}` - Obtener categoría por ID
- `POST /api/categories` - Crear nueva categoría
- `PUT /api/categories/{id}` - Actualizar categoría existente
- `DELETE /api/categories/{id}` - Eliminar categoría

La estructura se repite de manera similar para `/api/products` y futuras entidades.
