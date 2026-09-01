# Mitire

Team time reporting: a Vaadin UI for entering and reviewing time reports, backed by a
Spring Boot / Spring Data JPA domain layer that also exposes a REST API for other
systems to submit or query time entries.

## Modules

- `backend` — domain entities, repositories, services, REST controllers (`/api/**`), Flyway migrations. Built as a plain Java library (no Spring Boot plugin, no main class).
- `ui` — the runnable Spring Boot application. Vaadin views call the backend services directly (in-process); Spring Security is configured with two chains: session/form login for the UI, stateless HTTP Basic for `/api/**`.

Both modules run in a single deployable Spring Boot application (`ui`'s `bootJar`).

## Requirements

- Java 21 (a `gradle/gradle-daemon-jvm.properties` file pins the Gradle daemon to 21 and
  will auto-provision it via the Foojay resolver if you don't have one — this overrides
  any `org.gradle.java.home` set in your global `~/.gradle/gradle.properties`)
- Docker (for local Postgres)

## Running locally

```bash
docker compose up -d       # Postgres on localhost:5433 (5432 is often taken by a local Postgres.app)
./gradlew :ui:bootRun
```

Then open http://localhost:8080 and log in with the seeded admin account:

- username: `admin`
- password: `admin123`

A default project (`INTERNAL`) is also seeded on first run. Change the seeded password
before using this anywhere but a local dev machine.

## REST API

All endpoints require HTTP Basic auth (same users as the UI).

- `GET /api/projects`, `POST /api/projects`
- `GET /api/users`
- `GET /api/time-entries?userId=&projectId=&from=&to=`, `POST /api/time-entries`

```bash
curl -u admin:admin123 http://localhost:8080/api/time-entries
```

## Tests

```bash
./gradlew test
```
