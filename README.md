# Mini-Banco

[![CI](https://github.com/angelbelzunce/mini-banco/actions/workflows/ci.yaml/badge.svg)](https://github.com/angelbelzunce/mini-banco/actions/workflows/ci.yaml)

[Versión en español](README.es.md)

A small banking backend built with three Spring Boot microservices. I built it to practice the things a real banking project needs: exact money handling, atomic transfers, idempotency, concurrency control, reliable events, JWT security, testing, Docker and CI.

> **Out of scope:** this is a practice project, not a real bank. Users have no roles yet, and `notificaciones-service` is not protected because it is meant to be an internal service.

## Services

| Service | Port | What it does |
|---|---|---|
| `auth-service` | 8082 | Registers users (passwords hashed with BCrypt) and logs them in. The login returns a JWT signed with an RSA private key. |
| `cuentas-service` | 8080 | Accounts and transfers. Every request needs a valid JWT. Stores data in PostgreSQL and sends an event to Kafka after each transfer, using the Outbox pattern. |
| `notificaciones-service` | 8081 | Listens to the transfer events from Kafka and stores a notification for each one in its own PostgreSQL database. |

```
client ──login──> auth-service ──> PostgreSQL (auth_db)
   │                  │
   │                  └── returns a JWT signed with the private key
   │
   └──HTTP + JWT──> cuentas-service ──> PostgreSQL (cuentas_db + outbox table)
                          │
                          └──event──> Kafka ──> notificaciones-service ──> PostgreSQL (notificaciones_db)
```

## Tech stack

Java 21, Spring Boot 4, Maven, Spring Web, Spring Data JPA, Spring Security, JWT (JJWT, RS256), BCrypt, Jakarta Validation, springdoc-openapi (Swagger UI), PostgreSQL 17, Flyway, Apache Kafka, JUnit 5, Mockito, Testcontainers, Docker, Docker Compose and GitHub Actions.

## How to run it

You only need Docker. The services are compiled inside Docker with a multi-stage build, so you don't need Java or Maven to run them.

1. Generate an RSA key pair for the JWTs. With Java installed, you can do it with `jshell`:

   ```java
   var generator = java.security.KeyPairGenerator.getInstance("RSA");
   generator.initialize(2048);
   var pair = generator.generateKeyPair();
   var encoder = java.util.Base64.getEncoder();
   System.out.println("JWT_PRIVATE_KEY=" + encoder.encodeToString(pair.getPrivate().getEncoded()));
   System.out.println("JWT_PUBLIC_KEY=" + encoder.encodeToString(pair.getPublic().getEncoded()));
   ```

2. Create a `.env` file in the root folder with the database credentials and the two keys:

   ```
   POSTGRES_USER=your_user
   POSTGRES_PASSWORD=your_password
   JWT_PRIVATE_KEY=...
   JWT_PUBLIC_KEY=...
   ```

3. Start everything:

   ```bash
   docker compose up --build
   ```

This starts the three services, their three PostgreSQL databases and Kafka.

## Endpoints

The easiest way to see and try the accounts API is Swagger UI. It is generated from the code:

- cuentas-service: http://localhost:8080/swagger-ui.html
- notificaciones-service: http://localhost:8081/swagger-ui.html

### auth-service (`localhost:8082`)

| Method | Path | Description | Responses |
|---|---|---|---|
| POST | `/api/auth/register` | Create a user | 201, 409 |
| POST | `/api/auth/login` | Log in and get a JWT | 200, 401 |

### cuentas-service (`localhost:8080`)

All endpoints need the header `Authorization: Bearer <token>`. Without a valid token, the answer is 401.

| Method | Path | Description | Responses |
|---|---|---|---|
| POST | `/api/cuentas` | Create an account | 201, 400, 401 |
| GET | `/api/cuentas/{id}` | Get an account | 200, 401, 404 |
| GET | `/api/cuentas?page=0&size=10` | List accounts (paginated) | 200, 401 |
| POST | `/api/transferencias` | Make a transfer. Needs the `Idempotency-Key` header | 201, 400, 401, 404, 409, 422 |
| GET | `/api/transferencias/{id}` | Get a transfer | 200, 401, 404 |

### notificaciones-service (`localhost:8081`)

| Method | Path | Description |
|---|---|---|
| GET | `/api/notificaciones` | List the notifications |
| POST | `/api/notificaciones` | Create a notification by hand |

### Example

```bash
# Register a user and log in
curl -X POST localhost:8082/api/auth/register -H "Content-Type: application/json" \
  -d '{"username": "ana", "password": "secret123"}'
curl -X POST localhost:8082/api/auth/login -H "Content-Type: application/json" \
  -d '{"username": "ana", "password": "secret123"}'
# → {"token": "eyJhbGciOiJSUzI1NiJ9..."}

TOKEN=eyJhbGciOiJSUzI1NiJ9...

# Create two accounts
curl -X POST localhost:8080/api/cuentas -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"holder": "Ana", "initialBalance": 1000.00}'
curl -X POST localhost:8080/api/cuentas -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"holder": "Luis", "initialBalance": 50.00}'

# Transfer 100 from account 1 to account 2
curl -X POST localhost:8080/api/transferencias -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -H "Idempotency-Key: 7f9c2b1e-0001" \
  -d '{"sourceAccountId": 1, "targetAccountId": 2, "amount": 100.00, "transferType": "EXTERNAL"}'

# Account 1 now has 899.00 (100 + 1% external commission)
curl localhost:8080/api/cuentas/1 -H "Authorization: Bearer $TOKEN"

# A few seconds later, there is a notification for the transfer
curl localhost:8081/api/notificaciones
```

Response of the transfer (`201 Created`):

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

If you send the same request again with the same `Idempotency-Key`, you get this same transfer back and the money doesn't move again.

## Errors

Errors use the `ProblemDetail` format (RFC 9457), handled in one place with `@RestControllerAdvice`.

| Status | When |
|---|---|
| 400 | The request body is not valid |
| 401 | Wrong username or password, or a missing, invalid or expired token |
| 404 | The account or the transfer does not exist |
| 409 | The username already exists, or another operation changed the same account at the same time |
| 422 | Not enough balance (amount + commission) |

## Tests

To run the tests you need Java 21. Docker must be running, because the integration tests start a real PostgreSQL with Testcontainers. The commands are for Bash (on Windows, use Git Bash).

```bash
cd cuentas-service && ./mvnw verify
```

- **Unit tests** (JUnit 5 + Mockito): `AccountServiceTest`, `TransferServiceTest`, `AccountTest` and the commission strategies.
- **Web layer tests** (`@WebMvcTest`): `AccountControllerTest` and `TransferControllerTest`.
- **Integration tests** (`@SpringBootTest` + Testcontainers): `TransferIntegrationTest`. It includes:
  - the same `Idempotency-Key` sent twice moves the money only once
  - two threads debiting the same account at the same time: only one succeeds, and only one outbox event is saved

The tests use their own RSA keys in `application-test.yaml`. The real private key only lives in `.env`, which is not in the repository.

GitHub Actions runs `mvn verify` for the three services on every push and pull request.

## Design decisions

- **`BigDecimal` for money.** `double` can't store values like 0.1 exactly. Balances are `NUMERIC(19,2)` in the database.
- **Atomic transfers.** The whole transfer runs in one `@Transactional` method: both accounts change, or none of them.
- **Idempotency.** The client sends an `Idempotency-Key`. If the key was already used, the service returns the first transfer and does not move the money again. The column is also `unique` in the database, as a second line of defense.
- **Optimistic locking.** `Account` has a `@Version` field. If two operations update the same account at the same time, one of them fails and the API returns 409.
- **Strategy pattern for commissions.** `CommissionStrategy` has one implementation per transfer type: internal (no commission) and external (1%). Adding a new type means adding a new class, without changing `TransferService`.
- **Kafka instead of a REST call.** First, `cuentas-service` called `notificaciones-service` by REST, protected with Resilience4j (timeout, retry and circuit breaker). Then I replaced it with a Kafka event, so the transfer doesn't depend on the other service being up.
- **Outbox pattern.** A database transaction can't include a message sent to Kafka. So the transfer doesn't talk to Kafka: it saves an `OutboxEvent` in the same transaction. Both are saved, or none. A scheduled job (`@Scheduled`) reads the pending events every 5 seconds, sends them to Kafka in order, and marks each one as sent only after Kafka confirms it. If Kafka is down, the events wait in the table and are sent when it comes back. No event is lost.
- **Idempotent consumer.** The Outbox gives at-least-once delivery: if the app crashes right after Kafka confirms an event, that event is sent again. `notificaciones-service` ignores repeated events (`saveIfAbsent`), and `transfer_id` is also `unique` in its table.
- **A separate authentication service.** `auth-service` is the only service that knows the users and their passwords. Passwords are stored as BCrypt hashes, never in plain text. A failed login gives the same message whether the user exists or not, so nobody can find out which usernames exist.
- **JWT signed with RSA (RS256).** `auth-service` signs the tokens with a private key that only it has. `cuentas-service` checks them with the public key in a `JwtAuthenticationFilter`, which runs before every request. With a shared secret, any service that can check tokens could also create them; with a public key, it can only check them. The API is stateless: no sessions, every request brings its token. In production, the identity part would usually be a dedicated tool such as Keycloak.
- **Database migrations with Flyway.** The tables are created by versioned SQL scripts in `db/migration`, not by Hibernate. Hibernate only validates that the tables match the entities (`ddl-auto: validate`). The integration tests run the same scripts on a real PostgreSQL.
- **Database per service.** Each microservice has its own PostgreSQL database and its own Flyway migrations. No service reads the other's tables.
- **Spring profiles.** The `dev` profile has the local database settings. Credentials and keys come from environment variables, never from the code.
