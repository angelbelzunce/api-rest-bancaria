# Banking REST API

[![CI](https://github.com/angelbelzunce/api-rest-bancaria/actions/workflows/ci.yaml/badge.svg)](https://github.com/angelbelzunce/api-rest-bancaria/actions/workflows/ci.yaml)

[Versión en español](README.es.md)

A small banking backend built with two Spring Boot microservices. I built it to practice the things a real banking project needs: exact money handling, atomic transfers, idempotency, concurrency control, testing, Docker and CI.

> **Out of scope:** this is a practice project, not a real bank. There is no authentication, notifications are only kept in memory, and the database schema is created by Hibernate (no migrations).

## Services

| Service | Port | What it does |
|---|---|---|
| `cuentas-service` | 8080 | Accounts and transfers. Stores data in PostgreSQL and publishes an event to Kafka after each transfer. |
| `notificaciones-service` | 8081 | Listens to the transfer events from Kafka and stores a notification for each one (in memory). |

```
client ──HTTP──> cuentas-service ──> PostgreSQL
                       │
                       └──event──> Kafka ──> notificaciones-service
```

## Tech stack

Java 21, Spring Boot 4, Maven, Spring Web, Spring Data JPA, Jakarta Validation, PostgreSQL 17, Apache Kafka, JUnit 5, Mockito, Testcontainers, Docker, Docker Compose and GitHub Actions.

## How to run it

You need Java 21 and Docker. The commands are for Bash (on Windows, use Git Bash).

1. Create a `.env` file in the root folder with the database credentials:

   ```
   POSTGRES_USER=your_user
   POSTGRES_PASSWORD=your_password
   ```

2. Build the jars. The Dockerfiles copy the jar from `target/`:

   ```bash
   cd cuentas-service && ./mvnw clean package -DskipTests && cd ..
   cd notificaciones-service && ./mvnw clean package -DskipTests && cd ..
   ```

3. Start everything:

   ```bash
   docker compose up --build
   ```

This starts PostgreSQL, Kafka and both services.

## Endpoints

### cuentas-service (`localhost:8080`)

| Method | Path | Description | Responses |
|---|---|---|---|
| POST | `/api/cuentas` | Create an account | 201, 400 |
| GET | `/api/cuentas/{id}` | Get an account | 200, 404 |
| GET | `/api/cuentas?page=0&size=10` | List accounts (paginated) | 200 |
| POST | `/api/transferencias` | Make a transfer. Needs the `Idempotency-Key` header | 201, 400, 404, 409, 422 |
| GET | `/api/transferencias/{id}` | Get a transfer | 200, 404 |

### notificaciones-service (`localhost:8081`)

| Method | Path | Description |
|---|---|---|
| GET | `/api/notificaciones` | List the notifications |
| POST | `/api/notificaciones` | Create a notification by hand |

### Example

```bash
# Create two accounts
curl -X POST localhost:8080/api/cuentas -H "Content-Type: application/json" \
  -d '{"holder": "Ana", "initialBalance": 1000.00}'
curl -X POST localhost:8080/api/cuentas -H "Content-Type: application/json" \
  -d '{"holder": "Luis", "initialBalance": 50.00}'

# Transfer 100 from account 1 to account 2
curl -X POST localhost:8080/api/transferencias -H "Content-Type: application/json" \
  -H "Idempotency-Key: 7f9c2b1e-0001" \
  -d '{"sourceAccountId": 1, "targetAccountId": 2, "amount": 100.00, "transferType": "EXTERNAL"}'

# Account 1 now has 899.00 (100 + 1% external commission)
curl localhost:8080/api/cuentas/1

# There is a notification for the transfer
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
| 404 | The account or the transfer does not exist |
| 409 | Another operation changed the same account at the same time |
| 422 | Not enough balance (amount + commission) |

## Tests

Docker must be running, because the integration tests start a real PostgreSQL with Testcontainers.

```bash
cd cuentas-service && ./mvnw verify
```

- **Unit tests** (JUnit 5 + Mockito): `AccountServiceTest`, `TransferServiceTest`, `AccountTest` and the commission strategies.
- **Web layer tests** (`@WebMvcTest`): `AccountControllerTest`.
- **Integration tests** (`@SpringBootTest` + Testcontainers): `TransferIntegrationTest`. It includes:
  - the same `Idempotency-Key` sent twice moves the money only once
  - two threads debiting the same account at the same time: only one succeeds

GitHub Actions runs `mvn verify` for both services on every push and pull request.

## Design decisions

- **`BigDecimal` for money.** `double` can't store values like 0.1 exactly. Balances are `NUMERIC(19,2)` in the database.
- **Atomic transfers.** The whole transfer runs in one `@Transactional` method: both accounts change, or none of them.
- **Idempotency.** The client sends an `Idempotency-Key`. If the key was already used, the service returns the first transfer and does not move the money again. The column is also `unique` in the database, as a second line of defense.
- **Optimistic locking.** `Account` has a `@Version` field. If two operations update the same account at the same time, one of them fails and the API returns 409.
- **Strategy pattern for commissions.** `CommissionStrategy` has one implementation per transfer type: internal (no commission) and external (1%). Adding a new type means adding a new class, without changing `TransferService`.
- **Kafka instead of a REST call.** First, `cuentas-service` called `notificaciones-service` by REST, protected with Resilience4j (timeout, retry and circuit breaker). Then I replaced it with a Kafka event, so the transfer doesn't depend on the other service being up. If Kafka is down, the transfer still works and the error is only logged.
- **Spring profiles.** The `dev` profile has the local database settings. Credentials come from environment variables, never from the code.
