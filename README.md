# Mitire

Team time reporting: a Vaadin UI for entering and reviewing time reports, backed by a
Spring Boot / Spring Data JPA domain layer that also exposes a REST API for other
systems to submit or query time entries.

## Modules

- `backend` — domain entities, repositories, services, REST controllers (`/api/**`), Flyway migrations. Built as a plain Java library (no Spring Boot plugin, no main class).
- `ui` — the runnable Spring Boot application. Vaadin views call the backend services directly (in-process); Spring Security is configured with two chains: session/form login for the UI, stateless HTTP Basic for `/api/**`.

Both modules run in a single deployable Spring Boot application (`ui`'s `bootJar`).

The app icon lives at `backend/src/main/resources/static/mitire-icon.png`, served by
Spring's default static resource handling at `/mitire-icon.png`. It's used as the
browser-tab favicon (registered via `Application.configurePage()`, which Vaadin's
security layer automatically detects and permits for unauthenticated requests — no
manual security config needed) and as the logo shown to the left of the drawer toggle
in `MainLayout`.

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
- A **user** can belong to any number of groups (many-to-many) and inherits the union
  of all their groups' project access.
- **ADMIN** users bypass this entirely and can access every project.

This is enforced in `TimeEntryService`, not just hidden in the UI — submitting a time
entry (via the UI or directly through the REST API) for a project outside all of the
user's groups throws `AccessDeniedException` (HTTP 403). `UserService.findAccessibleProjects()`
computes the same union-of-groups policy (or "everything" for ADMIN) for every
"accessible projects" list shown in the UI. Managing projects, groups, and users is
restricted to ADMIN via `@PreAuthorize` on the REST layer and `@RolesAllowed` on the
corresponding Vaadin views.

Clicking a project row on the Projects page opens `/projects/{id}`, a detail page with
four tabs: **Project details** (edit name/active status), **Time Entries** (every entry
logged against the project, across all users), **Users** (everyone with access to the
project — group members plus all ADMINs; clicking a row there opens that user's detail
page), and **Groups** (every group that grants access to this project, a picker to link
an additional existing group, and a trashcan icon per row to unlink one — with
confirmation — the reciprocal of the Projects tab on a group's page).

Clicking a group row on the Groups page opens `/groups/{id}`, a detail page with three
tabs: **Group details** (rename), **Projects** (the group's permitted projects —
Code/Name/Active columns, click through to a project's detail page, a picker to link an
additional existing project, and a trashcan icon per row to unlink one — with
confirmation), and **Users** (members of that group, likewise click-through to their
detail page, a picker to link an additional existing user, and a trashcan icon per row
to remove one from the group — with confirmation). The Groups list page is create-only
— editing happens on the detail page.

Clicking a user row on the Users page opens `/users/{id}`, a detail page with four
tabs: **User details** (edit profile, role, groups — a multi-select, since a user can
belong to any number of groups — enabled status, and — for non-ADMIN users — reset the
password), **Time recordings** (every time entry logged by that user, across all
projects), **Groups** (every group the user belongs to — click through to a group's
detail page, a picker to link an additional existing group without leaving the tab, and
a trashcan icon per row to remove that membership — with confirmation), and **Projects**
(every project the user can currently access — the
union of all their groups' projects, or everything for ADMIN; click through to a
project's detail page). The Users list page is likewise create-only, with the same
multi-select for initial group assignment.

The three detail pages fully cross-link every user/group/project relationship, in both
directions: a project's "Users"/"Groups" tabs and a group's "Users"/"Projects" tabs
link to those entities' pages, and a user's "Groups"/"Projects" tabs link back. Every
relationship tab that shows a link also has a picker to add another one, backed by the
same handful of `addX`-style service methods regardless of which side initiates the
link.

## Time entries

Clicking a row on the home page (`/`, your own time entries) opens `/time-entries/{id}`
to edit it: Hours and Description are the only editable fields (project/date/status are
shown read-only — creating a new entry for a different project/date is a separate
action from the home page's form). **Save** persists and returns to `/`; **Cancel**
returns without saving; **Delete** asks for confirmation first, then deletes and
returns to `/`. Only the entry's own owner can view/edit/delete it — ADMIN bypasses
this, same as the rest of the access model — enforced in `TimeEntryService`, not just
hidden in the UI, so `GET`/`PUT`/`DELETE /api/time-entries/{id}` reject a non-owner,
non-admin caller with 403.

## My Profile

The user icon button next to "Log out" (any role, not just ADMIN) opens `/profile`,
where you can see your own username, role, groups and enabled status (all read-only —
role and group membership are admin-managed), edit your full name/email, and change
your own password given the current one. This is a separate, self-service path from
the admin-only `UserService.updatePassword()` used on the Users pages — it has no
ADMIN-target restriction, since changing *your own* password is always allowed
regardless of role (unlike an admin resetting *someone else's*).

## REST API

All endpoints require HTTP Basic auth (same users as the UI). Endpoints that create or
update projects, groups, or users require the ADMIN role.

- `GET /api/projects`, `GET /api/projects/{id}`, `POST /api/projects` (admin), `PUT /api/projects/{id}` (admin)
- `GET /api/groups` (optionally `?projectId=` for groups granting access to a project),
  `GET /api/groups/{id}`, `POST /api/groups` (admin), `PUT /api/groups/{id}` (admin)
- `PUT /api/groups/{id}/projects/{projectId}` (admin) — links an existing project to
  the group (a no-op, not an error, if already linked)
- `DELETE /api/groups/{id}/projects/{projectId}` (admin) — unlinks the project from the
  group (a no-op, not an error, if not linked)
- `GET /api/users` (optionally `?projectId=` for users with access to a project, or
  `?groupId=` for members of a group), `GET /api/users/{id}`, `POST /api/users`
  (admin), `PUT /api/users/{id}` (admin)
- `PUT /api/users/{id}/groups/{groupId}` (admin) — links an existing group to the user
  (a no-op, not an error, if already linked)
- `DELETE /api/users/{id}/groups/{groupId}` (admin) — removes the user from the group
  (a no-op, not an error, if not a member)
- `PUT /api/users/{id}/password` (admin) — resets a non-ADMIN user's password; rejected
  with 403 if the target user is an ADMIN, even for another admin
- `GET /api/users/me`, `PUT /api/users/me` (update own full name/email), `PUT
  /api/users/me/password` (change own password given the current one) — any
  authenticated user, no ADMIN role required; these act on the caller's own account
  only, resolved from the request's authentication
- `GET /api/time-entries?userId=&projectId=&from=&to=`, `POST /api/time-entries`
- `GET /api/time-entries/{id}`, `PUT /api/time-entries/{id}` (hours/description only),
  `DELETE /api/time-entries/{id}` — all three restricted to the entry's own owner or
  an ADMIN (403 otherwise)

Every `@Valid @RequestBody` failure across the API returns a proper `400` with an
`{"error": "..."}` body via an explicit `@ExceptionHandler(MethodArgumentNotValidException.class)`
in `ApiExceptionHandler` — without it, Spring's default handling calls `sendError(400)`,
which triggers Boot's `/error` forward and gets re-caught by the Vaadin security chain,
silently turning it into a `403`. Same root cause, and same fix, as the `AccessDeniedException`
handler above it.

```bash
curl -u admin:admin123 http://localhost:8080/api/time-entries
```

## Tests

```bash
./gradlew test
```
