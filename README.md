# API Base — Spring Boot 3 + JWT + Kafka

API REST base lista para escalar a microservicios. Incluye autenticación JWT (access + refresh), roles y permisos con Spring Security, CRUD de ejemplo con paginación, Flyway para migraciones, Kafka para eventos asíncronos y todo el entorno en Docker.

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5.x |
| Build | Maven (módulo único, con wrapper `mvnw`) |
| Persistencia | Spring Data JPA (Hibernate) + PostgreSQL 16 |
| Migraciones | Flyway |
| Seguridad | Spring Security + JWT (HS256, nimbus-jose-jwt) |
| Validación | `spring-boot-starter-validation` (Bean Validation) |
| Mappers | MapStruct 1.6.x (+ Lombok) |
| API docs | springdoc-openapi (Swagger UI) |
| Mensajería | Kafka (spring-kafka) |
| Serialización | Jackson |
| Testing | JUnit 5 + Mockito + AssertJ |
| Configuración | YAML + archivo `.env` (spring-dotenv) |

## Estructura del proyecto

```
projectJava/
├── pom.xml                      # dependencias y build
├── mvnw / .mvn                  # Maven wrapper (Maven 3.9.9)
├── docker-compose.yml           # postgres + kafka + kafdrop + backend
├── docker/Dockerfile            # multi-stage: build Maven -> JRE 21
├── .env / .env.example          # variables de configuración
└── src/
    ├── main/resources/
    │   ├── application.yml              # config principal
    │   ├── application-dev.yml          # perfil dev (SQL, logs)
    │   └── db/migration/                # migraciones Flyway
    │       ├── V1__create_security_schema.sql
    │       ├── V2__create_product_schema.sql
    │       └── V3__seed_security_data.sql
    └── main/java/com/example/api/
        ├── ApiApplication.java
        ├── common/          # excepciones, ApiResponse, PageResponse
        ├── security/
        │   ├── config/      # SecurityConfig, CORS, OpenAPI, JwtProperties
        │   ├── token/       # JwtService, JwtAuthFilter, TokenType
        │   ├── model/       # AppUserDetails
        │   ├── service/     # AppUserDetailsService, CurrentUserService
        │   └── auth/        # AuthService, AuthController, RefreshToken, DTOs
        ├── user/            # entidades User/Role/Permission, repos, service, mapper, controller
        ├── product/         # CRUD de ejemplo (model, service, mapper, controller, evento)
        └── kafka/           # config, topic, ProductEventProducer, ProductEventConsumer
```

## Cómo se creó (resumen)

1. **Base**: proyecto Spring Boot 3.5.x con Maven (wrapper) y Java 21. Se usó Spring Initializr solo como referencia; el pom se definió a mano para controlar todas las versiones.
2. **Configuración**: `application.yml` con valores por defecto que son sobreescritos por variables de entorno (binding relajado de Spring). El archivo `.env` se carga con `me.paulschwarz:spring-dotenv`.
3. **Base de datos**: Flyway con migraciones versionadas (`V1`, `V2`, `V3`). `ddl-auto: validate` para que Hibernate valide que las entidades coinciden con el esquema.
4. **Seguridad**: tokens JWT firmados con HS256 (clave en `APP_JWT_SECRET`, base64 de 32+ bytes). Access token de 15 min, refresh de 7 días. El refresh token se guarda con hash SHA-256 en PostgreSQL y se rota en cada renovación.
5. **Roles y permisos**: tablas `users`, `roles`, `permissions`, `user_roles`, `role_permissions`. Las authorities se derivan como `ROLE_<NOMBRE>` + nombre de cada permiso y se usan con `@PreAuthorize`.
6. **Kafka**: broker en modo KRaft, tópico `product-events`. Al crear un producto se publica `ProductEvent` (JSON) y un consumidor lo registra en el log — patrón base para comunicación entre microservicios.
7. **Docker**: `docker-compose.yml` orquesta PostgreSQL (healthcheck), Kafka (KRaft single node), Kafdrop (UI) y el backend. El `Dockerfile` es de dos etapas (build con Maven → runtime con JRE 21 y usuario sin privilegios).

## Requisitos

- Docker + Docker Compose (no necesitas Java ni Maven instalados)
- Para desarrollo local: JDK 21 y Maven 3.9+ (o usa `./mvnw`)

## Levantar el entorno

```bash
# 1. (opcional) ajusta credenciales, secretos y puertos en .env
cp .env.example .env

# 2. construir y levantar todo (postgres + kafka + kafdrop + backend)
docker compose up -d --build

# 3. ver logs del backend
docker compose logs -f backend
```

Puntos de entrada:

| Servicio | URL |
|---|---|
| API | http://localhost:8090 |
| Swagger UI | http://localhost:8090/swagger-ui.html |
| OpenAPI JSON | http://localhost:8090/v3/api-docs |
| Health | http://localhost:8090/actuator/health |
| Kafdrop (UI Kafka) | http://localhost:9000 |

> Si el puerto 8080 del host está disponible, puedes volver a `"8080:8080"` en `docker-compose.yml`.

### Desarrollo local sin Docker para la API

```bash
# usar la base y kafka de docker y solo la app en local
docker compose up -d postgres kafka
./mvnw spring-boot:run
```

> **Recarga automática:** `spring-boot-devtools` viene activado (dependencia `runtime`/`optional`).
> Con `./mvnw spring-boot:run` la app se reinicia sola al modificar código, y el Swagger
> (`/swagger-ui.html`) refleja los cambios al instante. En el contenedor Docker no aplica:
> ahí es necesario reconstruir (`docker compose up -d --build backend`).

## Configuración (.env)

Variable | Descripción | Default
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL de la base de datos | `jdbc:postgresql://localhost:5432/api` |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | credenciales de BD | `api` / `api` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | broker Kafka | `localhost:29092` |
| `SPRING_KAFKA_CONSUMER_GROUP_ID` | grupo del consumidor | `api-group` |
| `APP_JWT_SECRET` | clave de firma JWT (base64) | valor de desarrollo |
| `APP_JWT_ACCESS_TOKEN_EXPIRATION_MS` | caducidad access token | `900000` (15 min) |
| `APP_JWT_REFRESH_TOKEN_EXPIRATION_MS` | caducidad refresh | `604800000` (7 días) |
| `APP_JWT_ISSUER` | emisor del token | `api` |
| `APP_CORS_ALLOWED_ORIGINS` | orígenes permitidos (CORS) | localhost:3000/4200/8080 |
| `APP_KAFKA_PRODUCT_TOPIC` | tópico de eventos | `product-events` |

El orden de precedencia es: variables reales del entorno (Docker/OS) > `.env` > valores por defecto del `application.yml`.

## Endpoints

Autenticación (`/auth`):

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/login` | Login → access + refresh tokens (público) |
| POST | `/auth/refresh` | Rota tokens con el refresh token (público) |
| POST | `/auth/logout` | Revoca el refresh token (autenticado) |

> No existe registro público. Los usuarios se crean con `POST /api/users` (requiere `USER:CREATE`), asignando el rol directamente. El borrado es lógico (`enabled=false`): el usuario no puede iniciar sesión ni renovar tokens, pero se conserva en la lista.

Usuarios (`/api/users`):

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/users/me` | autenticado | Perfil actual |
| GET | `/api/users` | `USER:READ` | Lista paginada de usuarios (incluye deshabilitados) |
| POST | `/api/users` | `USER:CREATE` | Crear usuario con roles (si roles se omite, asigna `USER`) |
| PUT | `/api/users/{id}/roles` | `USER:UPDATE` | Reemplaza los roles de un usuario existente |
| PUT | `/api/users/{id}` | `USER:UPDATE` | Actualiza username/email/password/enabled (solo los campos enviados) |
| DELETE | `/api/users/{id}` | `USER:UPDATE` | Borrado lógico: deshabilita la cuenta |

Productos (`/api/products`, CRUD de ejemplo):

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/products` | `PRODUCT:READ` | Lista paginada + filtros `search`, `available` |
| GET | `/api/products/{id}` | `PRODUCT:READ` | Detalle |
| POST | `/api/products` | `PRODUCT:CREATE` | Crear (publica evento Kafka) |
| PUT | `/api/products/{id}` | `PRODUCT:UPDATE` | Actualizar |
| DELETE | `/api/products/{id}` | `PRODUCT:DELETE` | Eliminar |

Tipos de respuesta: `ApiResponse<T>` envuelve datos (`success`, `message`, `data`) y los errores usan `ErrorResponse` (`status`, `message`, `fieldErrors`).

### Credenciales del seed

Usuario por defecto creado por Flyway (`V3`):

```
usuario: admin
contraseña: admin123
```

```bash
TOKEN=$(curl -s -X POST http://localhost:8090/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.data.accessToken')

curl http://localhost:8090/api/products -H "Authorization: Bearer $TOKEN"
```

## Permisos definidos

| Permiso | Rol ADMIN | Rol USER |
|---|---|---|
| `USER:CREATE` | ✅ | ❌ |
| `USER:UPDATE` | ✅ | ❌ |
| `PRODUCT:CREATE` | ✅ | ✅ |
| `PRODUCT:READ` | ✅ | ✅ |
| `PRODUCT:UPDATE` | ✅ | ✅ |
| `PRODUCT:DELETE` | ✅ | ❌ |
| `USER:READ` | ✅ | ✅ |
| `USER:UPDATE` | ✅ | ❌ |

## Tests

```bash
# con docker
docker run --rm -v "$PWD":/app -w /app -v mavenrepo:/root/.m2 \
  maven:3.9-eclipse-temurin-21 mvn test

# con maven local
./mvnw test
```

Cobertura: `JwtServiceTest` (emisión/validación), `AuthServiceTest` (registro, login, refresh con rotación, logout) y `ProductServiceTest` (CRUD, eventos y paginación), usando JUnit 5 y Mockito.

## Notas para microservicios

- El backend ya publica y consume eventos Kafka (`product-events`); la separación futura consiste en mover `product`/`user` a servicios independientes reutilizando `common` y `security`.
- La configuración es sensible a variables de entorno, pensada para que cada servicio use el mismo patrón.
- El `docker-compose.yml` sirve como base para un stack multi-servicio (un servicio por módulo).