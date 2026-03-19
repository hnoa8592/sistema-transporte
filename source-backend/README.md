# Sistema Transporte — Backend

API REST para un sistema SaaS de transporte de buses en Bolivia.
Construido con **Java 21 + Spring Boot 3.3.4 + PostgreSQL 16**.

---

## Contenido

- [Requisitos previos](#requisitos-previos)
- [Variables de entorno](#variables-de-entorno)
- [Inicio local (sin Docker)](#inicio-local-sin-docker)
- [Inicio con Docker Compose](#inicio-con-docker-compose)
- [Verificación](#verificación)
- [Perfiles disponibles](#perfiles-disponibles)

---

## Requisitos previos

### Para ejecución local
| Herramienta | Versión mínima |
|-------------|---------------|
| JDK | 21 |
| Maven | 3.9 |
| PostgreSQL | 16 |

### Para Docker Compose
| Herramienta | Versión mínima |
|-------------|---------------|
| Docker | 24 |
| Docker Compose | 2.20 |

---

## Variables de entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/sistema_transporte` | URL JDBC de la base de datos |
| `DB_USERNAME` | `postgres` | Usuario de PostgreSQL |
| `DB_PASSWORD` | `postgres` | Contraseña de PostgreSQL |
| `JWT_SECRET` | *(valor de prueba inseguro)* | Secreto JWT en Base64 — **cambiar en producción** |
| `SERVER_PORT` | `8080` | Puerto HTTP del servidor |
| `JWT_ACCESS_EXPIRATION` | `900000` | Expiración del access token en ms (15 min) |
| `JWT_REFRESH_EXPIRATION` | `604800000` | Expiración del refresh token en ms (7 días) |
| `RESERVATION_EXPIRY_MINUTES` | `30` | Minutos antes de que expire una reserva |

### Generar un JWT_SECRET seguro

```bash
openssl rand -base64 64
```

---

## Inicio local (sin Docker)

### 1. Crear la base de datos

```bash
psql -U postgres -c "CREATE DATABASE sistema_transporte;"
```

### 2. Configurar variables de entorno (opcional)

Las variables tienen valores por defecto para desarrollo local. Solo es necesario exportarlas si los valores difieren:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/sistema_transporte
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=<base64-secret>
```

### 3. Compilar

```bash
mvn clean package -DskipTests
```

### 4. Ejecutar

**Perfil por defecto:**
```bash
java -jar target/sistema-transporte-1.0.0-SNAPSHOT.jar
```

**Perfil de desarrollo** (SQL visible en consola):
```bash
java -jar target/sistema-transporte-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

**Alternativamente con Maven:**
```bash
mvn spring-boot:run
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

> Flyway ejecuta automáticamente las migraciones del esquema `public` al arrancar.

---

## Inicio con Docker Compose

### Opción A — Stack completo (base de datos + aplicación)

#### 1. Crear el archivo `.env`

```bash
cp .env.example .env
```

Editar `.env` y establecer al menos `JWT_SECRET` con un valor seguro:

```bash
# Generar secreto
JWT_SECRET=$(openssl rand -base64 64)
echo "JWT_SECRET=$JWT_SECRET" >> .env
```

#### 2. Levantar los servicios

```bash
docker compose up -d
```

La primera vez Docker construirá la imagen de la aplicación (~2-3 minutos).
PostgreSQL estará listo antes de que la app arranque gracias al `healthcheck`.

#### 3. Ver logs

```bash
# Todos los servicios
docker compose logs -f

# Solo la aplicación
docker compose logs -f app
```

#### 4. Detener

```bash
docker compose down
```

Para eliminar también los datos de PostgreSQL:
```bash
docker compose down -v
```

---

### Opción B — Solo la base de datos en Docker (app corre local)

Útil durante el desarrollo para no reconstruir la imagen en cada cambio.

```bash
# Levantar solo PostgreSQL
docker compose up -d db

# Ejecutar la app localmente con perfil dev
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

### Reconstruir la imagen tras cambios en el código

```bash
docker compose build app
docker compose up -d
```

---

## Verificación

Una vez iniciado el backend, verificar que responde:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

### Swagger UI

Abrir en el navegador:

```
http://localhost:8080/swagger-ui.html
```

Permite explorar y probar todos los endpoints de la API.
Para endpoints protegidos, usar el botón **Authorize** con un token JWT obtenido desde `POST /api/v1/auth/login`.

### Endpoints principales

| Módulo | Base path |
|--------|-----------|
| Autenticación | `/api/v1/auth` |
| Usuarios | `/api/v1/users` · `/api/v1/profiles` · `/api/v1/resources` |
| Operación | `/api/v1/buses` · `/api/v1/drivers` · `/api/v1/routes` · `/api/v1/schedules` |
| Pasajes | `/api/v1/tickets` · `/api/v1/seat-maps` · `/api/v1/reservations` |
| Encomiendas | `/api/v1/parcels` |
| Finanzas | `/api/v1/cash-registers` · `/api/v1/invoices` · `/api/v1/parameters` |

---

## Perfiles disponibles

| Perfil | Activación | Descripción |
|--------|-----------|-------------|
| *(default)* | ninguno | Configuración base, logs nivel INFO |
| `dev` | `--spring.profiles.active=dev` | SQL visible en consola, logs DEBUG |
| `prod` | `--spring.profiles.active=prod` | Variables de entorno obligatorias, logs mínimos |

---

## Ejecutar tests

```bash
mvn test
```

Los tests usan H2 en memoria y no requieren PostgreSQL.

---

## Multitenancy

El sistema utiliza un esquema PostgreSQL por tenant.
Al crear un nuevo tenant, llamar a `FlywayConfig.migrateTenantSchema(tenantId)` para provisionar
automáticamente su esquema con todas las tablas.

Cada request debe incluir el JWT con el claim `tenantId` para que Hibernate
resuelva el esquema correcto.
