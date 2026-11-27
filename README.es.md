# Mini-Banco

[![CI](https://github.com/angelbelzunce/mini-banco/actions/workflows/ci.yaml/badge.svg)](https://github.com/angelbelzunce/mini-banco/actions/workflows/ci.yaml)

[English version](README.md)

Un backend bancario pequeño hecho con tres microservicios en Spring Boot. Lo construí para practicar lo que necesita un proyecto bancario real: dinero exacto, transferencias atómicas, idempotencia, control de concurrencia, eventos fiables, seguridad con JWT, tests, Docker e integración continua.

> **Fuera de alcance:** es un proyecto de práctica, no un banco real. Los usuarios todavía no tienen roles, y `notificaciones-service` no está protegido porque está pensado como un servicio interno.

## Servicios

| Servicio | Puerto | Qué hace |
|---|---|---|
| `auth-service` | 8082 | Registra usuarios (con la contraseña en BCrypt) y les permite hacer login. El login devuelve un JWT firmado con una clave privada RSA. |
| `cuentas-service` | 8080 | Cuentas y transferencias. Todas las peticiones necesitan un JWT válido. Guarda los datos en PostgreSQL y envía un evento a Kafka después de cada transferencia, con el patrón Outbox. |
| `notificaciones-service` | 8081 | Escucha los eventos de transferencia de Kafka y guarda una notificación por cada uno en su propia base de datos PostgreSQL. |

```
cliente ──login──> auth-service ──> PostgreSQL (auth_db)
   │                   │
   │                   └── devuelve un JWT firmado con la clave privada
   │
   └──HTTP + JWT──> cuentas-service ──> PostgreSQL (cuentas_db + tabla outbox)
                          │
                          └──evento──> Kafka ──> notificaciones-service ──> PostgreSQL (notificaciones_db)
```

## Tecnologías

Java 21, Spring Boot 4, Maven, Spring Web, Spring Data JPA, Spring Security, JWT (JJWT, RS256), BCrypt, Jakarta Validation, springdoc-openapi (Swagger UI), PostgreSQL 17, Flyway, Apache Kafka, JUnit 5, Mockito, Testcontainers, Docker, Docker Compose y GitHub Actions.

## Cómo arrancarlo

Solo necesitas Docker. Los servicios se compilan dentro de Docker con un build multi-stage, así que no hace falta Java ni Maven para arrancarlos.

1. Genera una pareja de claves RSA para los JWT. Si tienes Java, puedes hacerlo con `jshell`:

   ```java
   var generator = java.security.KeyPairGenerator.getInstance("RSA");
   generator.initialize(2048);
   var pair = generator.generateKeyPair();
   var encoder = java.util.Base64.getEncoder();
   System.out.println("JWT_PRIVATE_KEY=" + encoder.encodeToString(pair.getPrivate().getEncoded()));
   System.out.println("JWT_PUBLIC_KEY=" + encoder.encodeToString(pair.getPublic().getEncoded()));
   ```

2. Crea un archivo `.env` en la carpeta raíz con las credenciales de la base de datos y las dos claves:

   ```
   POSTGRES_USER=tu_usuario
   POSTGRES_PASSWORD=tu_contraseña
   JWT_PRIVATE_KEY=...
   JWT_PUBLIC_KEY=...
   ```

3. Arranca todo:

   ```bash
   docker compose up --build
   ```

Esto levanta los tres servicios, sus tres bases de datos PostgreSQL y Kafka.

## Endpoints

La forma más fácil de ver y probar la API de cuentas es Swagger UI. Se genera a partir del código:

- cuentas-service: http://localhost:8080/swagger-ui.html
- notificaciones-service: http://localhost:8081/swagger-ui.html

### auth-service (`localhost:8082`)

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| POST | `/api/auth/register` | Crear un usuario | 201, 409 |
| POST | `/api/auth/login` | Hacer login y obtener un JWT | 200, 401 |

### cuentas-service (`localhost:8080`)

Todos los endpoints necesitan la cabecera `Authorization: Bearer <token>`. Sin un token válido, la respuesta es 401.

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| POST | `/api/cuentas` | Crear una cuenta | 201, 400, 401 |
| GET | `/api/cuentas/{id}` | Ver una cuenta | 200, 401, 404 |
| GET | `/api/cuentas?page=0&size=10` | Listar cuentas (paginado) | 200, 401 |
| POST | `/api/transferencias` | Hacer una transferencia. Necesita la cabecera `Idempotency-Key` | 201, 400, 401, 404, 409, 422 |
| GET | `/api/transferencias/{id}` | Ver una transferencia | 200, 401, 404 |

### notificaciones-service (`localhost:8081`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/notificaciones` | Listar las notificaciones |
| POST | `/api/notificaciones` | Crear una notificación a mano |

### Ejemplo

```bash
# Registrar un usuario y hacer login
curl -X POST localhost:8082/api/auth/register -H "Content-Type: application/json" \
  -d '{"username": "ana", "password": "secret123"}'
curl -X POST localhost:8082/api/auth/login -H "Content-Type: application/json" \
  -d '{"username": "ana", "password": "secret123"}'
# → {"token": "eyJhbGciOiJSUzI1NiJ9..."}

TOKEN=eyJhbGciOiJSUzI1NiJ9...

# Crear dos cuentas
curl -X POST localhost:8080/api/cuentas -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"holder": "Ana", "initialBalance": 1000.00}'
curl -X POST localhost:8080/api/cuentas -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"holder": "Luis", "initialBalance": 50.00}'

# Transferir 100 de la cuenta 1 a la cuenta 2
curl -X POST localhost:8080/api/transferencias -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -H "Idempotency-Key: 7f9c2b1e-0001" \
  -d '{"sourceAccountId": 1, "targetAccountId": 2, "amount": 100.00, "transferType": "EXTERNAL"}'

# La cuenta 1 tiene ahora 899.00 (100 + 1 % de comisión externa)
curl localhost:8080/api/cuentas/1 -H "Authorization: Bearer $TOKEN"

# Unos segundos después, hay una notificación de la transferencia
curl localhost:8081/api/notificaciones
```

Respuesta de la transferencia (`201 Created`):

```json
{
  "id": 1,
  "sourceAccountId": 1,
  "targetAccountId": 2,
  "amount": 100.00,
  "status": "COMPLETED",
  "createdAt": "2026-11-17T10:30:00.123456"
}
```

Si repites la misma petición con la misma `Idempotency-Key`, te devuelve esta misma transferencia y el dinero no se vuelve a mover.

## Errores

Los errores usan el formato `ProblemDetail` (RFC 9457) y se gestionan en un solo sitio con `@RestControllerAdvice`.

| Código | Cuándo |
|---|---|
| 400 | El cuerpo de la petición no es válido |
| 401 | Usuario o contraseña incorrectos, o un token que falta, no es válido o ha caducado |
| 404 | La cuenta o la transferencia no existe |
| 409 | El nombre de usuario ya existe, u otra operación ha modificado la misma cuenta a la vez |
| 422 | Saldo insuficiente (importe + comisión) |

## Tests

Para ejecutar los tests necesitas Java 21. Docker tiene que estar arrancado, porque los tests de integración levantan un PostgreSQL real con Testcontainers. Los comandos son para Bash (en Windows, usa Git Bash).

```bash
cd cuentas-service && ./mvnw verify
```

- **Unitarios** (JUnit 5 + Mockito): `AccountServiceTest`, `TransferServiceTest`, `AccountTest` y las estrategias de comisión.
- **Capa web** (`@WebMvcTest`): `AccountControllerTest` y `TransferControllerTest`.
- **Integración** (`@SpringBootTest` + Testcontainers): `TransferIntegrationTest`. Incluye:
  - la misma `Idempotency-Key` enviada dos veces mueve el dinero una sola vez
  - dos hilos cargando la misma cuenta a la vez: solo uno lo consigue, y solo se guarda un evento en el outbox

Los tests usan sus propias claves RSA en `application-test.yaml`. La clave privada real solo está en `.env`, que no se sube al repositorio.

GitHub Actions ejecuta `mvn verify` en los tres servicios en cada push y cada pull request.

## Decisiones de diseño

- **`BigDecimal` para el dinero.** `double` no puede guardar valores como 0.1 de forma exacta. Los saldos son `NUMERIC(19,2)` en la base de datos.
- **Transferencias atómicas.** Toda la transferencia va en un único método `@Transactional`: o cambian las dos cuentas, o ninguna.
- **Idempotencia.** El cliente envía una `Idempotency-Key`. Si la clave ya se usó, el servicio devuelve la primera transferencia y no vuelve a mover el dinero. Además, la columna es `unique` en la base de datos, como segunda barrera.
- **Bloqueo optimista.** `Account` tiene un campo `@Version`. Si dos operaciones modifican la misma cuenta a la vez, una de ellas falla y la API devuelve 409.
- **Patrón Strategy para las comisiones.** `CommissionStrategy` tiene una implementación por tipo de transferencia: interna (sin comisión) y externa (1 %). Añadir un tipo nuevo es añadir una clase nueva, sin tocar `TransferService`.
- **Kafka en lugar de una llamada REST.** Al principio, `cuentas-service` llamaba a `notificaciones-service` por REST, protegido con Resilience4j (timeout, reintentos y circuit breaker). Después lo cambié por un evento de Kafka, para que la transferencia no dependa de que el otro servicio esté levantado.
- **Patrón Outbox.** Una transacción de la base de datos no puede incluir un mensaje enviado a Kafka. Por eso la transferencia no habla con Kafka: guarda un `OutboxEvent` en la misma transacción. Se guardan los dos, o ninguno. Una tarea programada (`@Scheduled`) lee los eventos pendientes cada 5 segundos, los envía a Kafka en orden y marca cada uno como enviado solo cuando Kafka lo confirma. Si Kafka está caído, los eventos esperan en la tabla y se envían cuando vuelve. No se pierde ningún evento.
- **Consumidor idempotente.** El Outbox garantiza entrega "al menos una vez": si la aplicación se cae justo después de que Kafka confirme un evento, ese evento se envía otra vez. `notificaciones-service` ignora los eventos repetidos (`saveIfAbsent`), y además `transfer_id` es `unique` en su tabla.
- **Un servicio de autenticación aparte.** `auth-service` es el único servicio que conoce a los usuarios y sus contraseñas. Las contraseñas se guardan como hash con BCrypt, nunca en texto plano. Un login fallido da el mismo mensaje tanto si el usuario existe como si no, así nadie puede descubrir qué nombres de usuario existen.
- **JWT firmado con RSA (RS256).** `auth-service` firma los tokens con una clave privada que solo tiene él. `cuentas-service` los comprueba con la clave pública en un `JwtAuthenticationFilter`, que se ejecuta antes de cada petición. Con un secreto compartido, cualquier servicio que pudiera comprobar tokens podría también crearlos; con una clave pública, solo puede comprobarlos. La API no guarda sesiones: cada petición trae su token. En producción, la parte de identidad la llevaría normalmente una herramienta específica, como Keycloak.
- **Migraciones de base de datos con Flyway.** Las tablas las crean scripts SQL versionados en `db/migration`, no Hibernate. Hibernate solo comprueba que las tablas coinciden con las entidades (`ddl-auto: validate`). Los tests de integración ejecutan los mismos scripts sobre un PostgreSQL real.
- **Una base de datos por servicio.** Cada microservicio tiene su propia base de datos PostgreSQL y sus propias migraciones de Flyway. Ningún servicio lee las tablas de otro.
- **Perfiles de Spring.** El perfil `dev` tiene la configuración de la base de datos local. Las credenciales y las claves vienen de variables de entorno, nunca del código.
