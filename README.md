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
- A local PostgreSQL instance running on `localhost:5432`, with a `mitire` role/database:

  ```sql
  CREATE ROLE mitire WITH LOGIN PASSWORD 'mitire';
  CREATE DATABASE mitire OWNER mitire;
  ```

  (Only needs to be done once. Flyway creates the schema on first run.)

  **If you're on Postgres.app (macOS):** its default `pg_hba.conf` uses `trust` auth for
  all local TCP connections, but Postgres.app itself blocks passwordless connections from
  processes it can't identify (a JVM launched via Gradle is typically one of these) —
  see [postgresapp.com/l/app-permissions](https://postgresapp.com/l/app-permissions/).
  No permission dialog appears for unidentified processes; Postgres.app's own docs
  recommend password auth as the fix. Add this above the catch-all `trust` lines in
  Postgres.app's `pg_hba.conf` (Postgres menu → path shown under "Server Settings",
  typically `~/Library/Application Support/Postgres/var-<version>/pg_hba.conf`), then
  reload with `psql -c "SELECT pg_reload_conf();"` (no restart needed):

  ```
  host    mitire          mitire          127.0.0.1/32            scram-sha-256
  host    mitire          mitire          ::1/128                 scram-sha-256
  ```

  This only affects connections to the `mitire` database as the `mitire` role — every
  other database/role on the instance keeps using its existing rules.

## Running locally

```bash
./gradlew :ui:bootRun
```

Then open http://localhost:8080 and log in with a seeded account:

- `admin` / `admin123` — role ADMIN, sees every project regardless of group, and is the
  only role that can manage projects, groups and users (menu entries for those only
  appear for admins).
- `member` / `member123` — role MEMBER, belongs to the seeded "Internal Team" group,
  which grants access to the seeded `INTERNAL` project.

Change the seeded passwords before using this anywhere but a local dev machine.

## Access model

Projects aren't assigned to users directly. Instead:

- A **group** holds a set of permitted projects.
- A **user** optionally belongs to one group and inherits its project access.
- **ADMIN** users bypass this entirely and can access every project.

This is enforced in `TimeEntryService`, not just hidden in the UI — submitting a time
entry (via the UI or directly through the REST API) for a project outside the user's
group throws `AccessDeniedException` (HTTP 403). Managing projects, groups, and users
is restricted to ADMIN via `@PreAuthorize` on the REST layer and `@RolesAllowed` on the
corresponding Vaadin views.

Clicking a project row on the Projects page opens `/projects/{id}`, a detail page with
three tabs: **Project details** (edit name/active status), **Time Entries** (every entry
logged against the project, across all users), and **Users** (everyone with access to
the project — group members plus all ADMINs).

Clicking a group row on the Groups page opens `/groups/{id}`, a detail page with three
tabs: **Group details** (rename), **Projects** (edit the group's permitted projects),
and **Users** (members of that group). Both tabs' Save buttons update the same
name+projects pair, since a group is one entity split across tabs purely for
presentation. The Groups list page itself is create-only now — editing happens on the
detail page.

## REST API

All endpoints require HTTP Basic auth (same users as the UI). Endpoints that create or
update projects, groups, or users require the ADMIN role.

- `GET /api/projects`, `GET /api/projects/{id}`, `POST /api/projects` (admin), `PUT /api/projects/{id}` (admin)
- `GET /api/groups`, `GET /api/groups/{id}`, `POST /api/groups` (admin), `PUT /api/groups/{id}` (admin)
- `GET /api/users` (optionally `?projectId=` for users with access to a project, or
  `?groupId=` for members of a group), `POST /api/users` (admin), `PUT /api/users/{id}` (admin)
- `PUT /api/users/{id}/password` (admin) — resets a non-ADMIN user's password; rejected
  with 403 if the target user is an ADMIN, even for another admin
- `GET /api/time-entries?userId=&projectId=&from=&to=`, `POST /api/time-entries`

```bash
curl -u admin:admin123 http://localhost:8080/api/time-entries
```

## Tests

```bash
./gradlew test
```
