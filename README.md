# QueuePilot Incident API

A Spring Boot service for tracking operational incidents through a controlled lifecycle:
create, acknowledge, assign, start, resolve, close. Every transition is validated against an
explicit state machine, every change appends an immutable event to an audit trail, and
incident creation publishes a Kafka event for downstream consumers.

Built to practise the parts of backend work that a CRUD tutorial tends to skip: illegal
state transitions, duplicate suppression, structured error responses, and an audit log that
is appended to rather than overwritten.

## Stack

Java 21 · Spring Boot 3.3.5 · Spring Data JPA · Spring Kafka · Bean Validation · PostgreSQL · Lombok · Gradle

## Domain model

| Entity | Purpose |
|---|---|
| `Incident` | The tracked incident: title, description, severity, status, source, optional dedupe key, assignee, owning service, lifecycle timestamps. |
| `ServiceEntity` | The service an incident belongs to, with owning team, tier, SLA minutes and runbook URL. |
| `User` | Assignable person with a role (`ADMIN`, `SRE`, `ENGINEER`, `VIEWER`) and team. |
| `IncidentEvent` | Append-only audit record: what happened, which actor type caused it, and when. |

Enums: `Severity` (CRITICAL/HIGH/MEDIUM/LOW), `IncidentStatus`, `IncidentSource`
(MANUAL/DATADOG/PROMETHEUS/SECURITY/SYSTEM), `EventType`, `ServiceTier`, `Role`, `ActorType`.

## Status machine

`validateStatusTransition` in `IncidentService` is the single gate. A transition not on this
list throws `InvalidStateException` and the request returns 400 rather than silently
corrupting the incident's state.

```
OPEN ──────────► ACKNOWLEDGED ──────────► IN_PROGRESS ──────────► RESOLVED ──────► CLOSED
                      │                                              ▲
                      └──────────────────────────────────────────────┘
```

Assignment is refused separately: an incident that is already `RESOLVED` or `CLOSED` cannot
be assigned to anyone.

## Endpoints

Base path `/api/incidents`.

| Method | Path | Behaviour |
|---|---|---|
| `POST` | `/` | Create an incident. Requires `serviceId`, `title`, `severity`, `source`. Optional `dedupeKey` and `description`. |
| `GET` | `/` | List all incidents. |
| `GET` | `/{id}` | Fetch one incident. |
| `PATCH` | `/{id}/acknowledge` | `OPEN` → `ACKNOWLEDGED`, stamps `acknowledgedAt`. |
| `PATCH` | `/{id}/start` | `ACKNOWLEDGED` → `IN_PROGRESS`. |
| `PATCH` | `/{id}/assign` | Assign to a user by `userId`. |
| `PATCH` | `/{id}/resolve` | → `RESOLVED`, stamps `resolvedAt`, requires `resolutionSummary`. |
| `PATCH` | `/{id}/close` | `RESOLVED` → `CLOSED`. |

### Deduplication

If a request carries a `dedupeKey` that already exists, creation is rejected with **409
Conflict** instead of producing a second incident. The column is uniquely constrained, so
the check is backed by the database rather than by the service alone. This is the usual
alert-storm case: one failing service firing the same alert repeatedly should open one
incident, not two hundred.

### Audit trail

Every mutating operation writes an `IncidentEvent` (`CREATED`, `ACKNOWLEDGED`, `ASSIGNED`,
`STATUS_CHANGED`, `RESOLVED`) alongside the state change, inside the same transaction.
Events are only ever inserted. Nothing rewrites history, so the sequence of what happened
to an incident stays reconstructable.

### Error responses

`GlobalExceptionHandler` (`@RestControllerAdvice`) maps exceptions to a consistent
`ApiErrorResponse` body of `{status, error, message, timestamp}`:

| Exception | Status |
|---|---|
| `ResourceNotFoundException` | 404 Not Found |
| `DuplicateResourceException` | 409 Conflict |
| `InvalidStateException` | 400 Bad Request |
| `MethodArgumentNotValidException` | 400, field validation messages joined |
| `HttpMessageNotReadableException` | 400, malformed JSON |
| `ConstraintViolationException` | 400 |
| `Exception` | 500 |

### Kafka

Creating an incident publishes an `IncidentCreatedEvent` (incident id, service id and name,
severity, source, dedupe key, created timestamp) to the **`incident-created`** topic, keyed
by incident id so all events for one incident land on the same partition.
`KafkaProducerConfig` builds a dedicated `KafkaTemplate` with a `StringSerializer` key and a
`JsonSerializer` value, layered over Spring Boot's own Kafka properties.

## Running locally

Requires JDK 21, a PostgreSQL database, and a Kafka broker.

`src/main/resources/application.properties` ships with only the application name and
`server.port=3021`, so connection settings have to be supplied before the app will start.
Add them there or pass them as `--` arguments / environment variables:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/queuepilot
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update

spring.kafka.bootstrap-servers=localhost:9092
```

Then:

```bash
./gradlew bootRun      # starts on http://localhost:3021
./gradlew build        # compile + run tests
```

`spring-boot-starter-security` is on the classpath with no `SecurityFilterChain` configured,
so Spring Security's defaults apply: endpoints sit behind HTTP Basic with a generated
password printed to the console at startup.

## Known limitations

These are real and worth naming rather than discovering later:

- **Tests are a stub.** The only test is the generated `contextLoads`, and it needs the
  datasource and broker configuration above to pass. There is no coverage of the status
  machine or the dedupe path, which are the first things that should be tested.
- **No authentication model.** Security is unconfigured defaults; there is no login, and
  `actorId` on every audit event is written as `null` with `ActorType.SYSTEM`, because
  there is no authenticated principal to attribute actions to.
- **Kafka publish is a dual write.** `publishIncidentCreated` is called inside the same
  `@Transactional` method that saves the incident. If the broker is unreachable the
  transaction rolls back; if the publish succeeds and the transaction later fails, the event
  is already out. The fix is a transactional outbox, which is not implemented here.
- **Redis is declared, not used.** `spring-boot-starter-data-redis` is a dependency with no
  code behind it yet. It was added for planned priority-queue work.
- **Listing is unpaginated.** `GET /api/incidents` returns every row.
