# Mini-Banco

[![CI](https://github.com/angelbelzunce/mini-banco/actions/workflows/ci.yaml/badge.svg)](https://github.com/angelbelzunce/mini-banco/actions/workflows/ci.yaml)

[English version](README.md)

Un backend bancario pequeño hecho con dos microservicios en Spring Boot. Lo construí para practicar lo que necesita un proyecto bancario real: dinero exacto, transferencias atómicas, idempotencia, control de concurrencia, tests, Docker e integración continua.

> **Fuera de alcance:** es un proyecto de práctica, no un banco real. No tiene autenticación, las notificaciones solo se guardan en memoria y el esquema de la base de datos lo crea Hibernate (sin migraciones).

## Servicios

| Servicio | Puerto | Qué hace |
|---|---|---|
| `cuentas-service` | 8080 | Cuentas y transferencias. Guarda los datos en PostgreSQL y publica un evento en Kafka después de cada transferencia. |
| `notificaciones-service` | 8081 | Escucha los eventos de transferencia de Kafka y guarda una notificación por cada uno (en memoria). |

```
cliente ──HTTP──> cuentas-service ──> PostgreSQL
                        │
                        └──evento──> Kafka ──> notificaciones-service
```

## Tecnologías

Java 21, Spring Boot 4, Maven, Spring Web, Spring Data JPA, Jakarta Validation, PostgreSQL 17, Apache Kafka, JUnit 5, Mockito, Testcontainers, Docker, Docker Compose y GitHub Actions.

## Cómo arrancarlo

Necesitas Java 21 y Docker. Los comandos son para Bash (en Windows, usa Git Bash).

1. Crea un archivo `.env` en la carpeta raíz con las credenciales de la base de datos:

   ```
   POSTGRES_USER=tu_usuario
   POSTGRES_PASSWORD=tu_contraseña
   ```

2. Genera los jar. Los Dockerfile copian el jar de `target/`:

   ```bash
   cd cuentas-service && ./mvnw clean package -DskipTests && cd ..
   cd notificaciones-service && ./mvnw clean package -DskipTests && cd ..
   ```

3. Arranca todo:

   ```bash
   docker compose up --build
   ```

Esto levanta PostgreSQL, Kafka y los dos servicios.

## Endpoints

### cuentas-service (`localhost:8080`)

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| POST | `/api/cuentas` | Crear una cuenta | 201, 400 |
| GET | `/api/cuentas/{id}` | Ver una cuenta | 200, 404 |
| GET | `/api/cuentas?page=0&size=10` | Listar cuentas (paginado) | 200 |
| POST | `/api/transferencias` | Hacer una transferencia. Necesita la cabecera `Idempotency-Key` | 201, 400, 404, 409, 422 |
| GET | `/api/transferencias/{id}` | Ver una transferencia | 200, 404 |

### notificaciones-service (`localhost:8081`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/notificaciones` | Listar las notificaciones |
| POST | `/api/notificaciones` | Crear una notificación a mano |

### Ejemplo

```bash
# Crear dos cuentas
curl -X POST localhost:8080/api/cuentas -H "Content-Type: application/json" \
  -d '{"holder": "Ana", "initialBalance": 1000.00}'
curl -X POST localhost:8080/api/cuentas -H "Content-Type: application/json" \
  -d '{"holder": "Luis", "initialBalance": 50.00}'

# Transferir 100 de la cuenta 1 a la cuenta 2
curl -X POST localhost:8080/api/transferencias -H "Content-Type: application/json" \
  -H "Idempotency-Key: 7f9c2b1e-0001" \
  -d '{"sourceAccountId": 1, "targetAccountId": 2, "amount": 100.00, "transferType": "EXTERNAL"}'

# La cuenta 1 tiene ahora 899.00 (100 + 1 % de comisión externa)
curl localhost:8080/api/cuentas/1

# Hay una notificación de la transferencia
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
| 404 | La cuenta o la transferencia no existe |
| 409 | Otra operación ha modificado la misma cuenta a la vez |
| 422 | Saldo insuficiente (importe + comisión) |

## Tests

Docker tiene que estar arrancado, porque los tests de integración levantan un PostgreSQL real con Testcontainers.

```bash
cd cuentas-service && ./mvnw verify
```

- **Unitarios** (JUnit 5 + Mockito): `AccountServiceTest`, `TransferServiceTest`, `AccountTest` y las estrategias de comisión.
- **Capa web** (`@WebMvcTest`): `AccountControllerTest`.
- **Integración** (`@SpringBootTest` + Testcontainers): `TransferIntegrationTest`. Incluye:
  - la misma `Idempotency-Key` enviada dos veces mueve el dinero una sola vez
  - dos hilos cargando la misma cuenta a la vez: solo uno lo consigue

GitHub Actions ejecuta `mvn verify` en los dos servicios en cada push y cada pull request.

## Decisiones de diseño

- **`BigDecimal` para el dinero.** `double` no puede guardar valores como 0.1 de forma exacta. Los saldos son `NUMERIC(19,2)` en la base de datos.
- **Transferencias atómicas.** Toda la transferencia va en un único método `@Transactional`: o cambian las dos cuentas, o ninguna.
- **Idempotencia.** El cliente envía una `Idempotency-Key`. Si la clave ya se usó, el servicio devuelve la primera transferencia y no vuelve a mover el dinero. Además, la columna es `unique` en la base de datos, como segunda barrera.
- **Bloqueo optimista.** `Account` tiene un campo `@Version`. Si dos operaciones modifican la misma cuenta a la vez, una de ellas falla y la API devuelve 409.
- **Patrón Strategy para las comisiones.** `CommissionStrategy` tiene una implementación por tipo de transferencia: interna (sin comisión) y externa (1 %). Añadir un tipo nuevo es añadir una clase nueva, sin tocar `TransferService`.
- **Kafka en lugar de una llamada REST.** Al principio, `cuentas-service` llamaba a `notificaciones-service` por REST, protegido con Resilience4j (timeout, reintentos y circuit breaker). Después lo cambié por un evento de Kafka, para que la transferencia no dependa de que el otro servicio esté levantado. Si Kafka está caído, la transferencia funciona igual y el error solo se registra en el log.
- **Perfiles de Spring.** El perfil `dev` tiene la configuración de la base de datos local. Las credenciales vienen de variables de entorno, nunca del código.
