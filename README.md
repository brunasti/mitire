# MiTiRe

Team time reporting: a Vaadin UI for entering and reviewing time reports, backed by a
Spring Boot / Spring Data JPA domain layer that also exposes a REST API for other
systems to submit or query time entries.

## Modules

- `backend` — domain entities, repositories, services, REST controllers (`/api/**`), Flyway migrations. Built as a plain Java library (no Spring Boot plugin, no main class).
- `ui` — the runnable Spring Boot application. Vaadin views call the backend services directly (in-process); Spring Security is configured with two chains: session/form login for the UI, stateless HTTP Basic for `/api/**`.

Both modules run in a single deployable Spring Boot application (`ui`'s `bootJar`).

Every validation/error message across the UI (required fields, rejected updates,
access-denied exceptions caught in a view, etc.) is shown via
`it.brunasti.mitire.ui.util.Notifications.showError(message)` rather than a plain
`Notification.show(...)`: a solid dark-orange popup, top-center, held open for 6
seconds — deliberately more assertive than Vaadin's default pale bottom-left toast, so
it can't be mistaken for a routine (success) message or missed entirely.

A logged-in user who navigates to a page their role doesn't grant access to (e.g. a
MEMBER opening `/users`) sees a styled "Access denied" page inside the normal app
layout, instead of Vaadin's default raw "Could not navigate to '...'" message.
`AccessDeniedView` implements `HasErrorParameter<AccessDeniedException>`, which
Vaadin's routing automatically prefers over its own built-in handler for that
exception type (any application-provided handler for an exception class takes
priority over the framework's default, per `@DefaultErrorHandler`'s own contract).

The app icon lives at `backend/src/main/resources/static/mitire-icon.png`, served by
Spring's default static resource handling at `/mitire-icon.png`. It's used as the
browser-tab favicon (registered via `Application.configurePage()`, which Vaadin's
security layer automatically detects and permits for unauthenticated requests — no
manual security config needed) and as the logo shown to the left of the drawer toggle
in `MainLayout`, where it's wrapped in a button that navigates to the home page (`/`)
when clicked. The logged-in user's full name is shown to the left of the "My profile"
icon button. It's also shown next to the "MiTiRe" title on the login page
(`LoginView`), reusing the same already-anonymous-permitted `/mitire-icon.png` path.

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

The Projects page (`/projects`) shows a "Projects" heading and splits its content into
two tabs: **Projects** (the sortable list — Code, Name, Active, Start date and End date
columns can all be sorted) and **Add Project** (the creation form, which also takes an
optional start date and end date).

A project's start and end dates are both optional and otherwise unenforced (a project
stays usable for time entries regardless of today's date relative to them) — they're
informational only, aside from one check: `ProjectService` rejects an end date before
the start date with a `400`.

Clicking a project row on the Projects page opens `/projects/{id}`, showing the
project's name next to the "← Back to projects" link (kept live after a rename), with
five tabs: **Project details** (edit name/active status/start date/end date/approver —
see below), **Time Entries** (every entry logged against the project, across all
users, sorted by Date descending by default; clicking a row opens that entry's detail
page), **Users** (everyone with access to the project — group members plus all
ADMINs, with an **Approver** column and an **Owner** column, each showing "Yes" on
the row of the project's designated approver/owner (see below) and blank for everyone
else, live-updated the moment a new approver or owner is saved on the Project details
tab; clicking a row there opens that user's detail page), **Groups** (every group
that grants access to this project, a picker to
link an additional existing group, and a trashcan icon per row to unlink one — with
confirmation — the reciprocal of the Projects tab on a group's page), and **Statuses**
(the project's approval workflow — see below).

## Approval status

Each time entry carries a status describing where it is in that **project's**
approval flow. Unlike the fixed three-value status of earlier versions, the set of
valid statuses is itself data: the `project_entry_status` table holds an ordered
(`sequence`) list of named statuses **per project**, each with its own `active` flag,
`description`, and a `starting_status` flag, managed by ADMIN from the **Statuses**
tab on a project's detail page:

- **Add** (name + optional description; always created active, never starting)
- **Edit** (pencil icon) — a dialog to rename, change the description, or toggle
  active
- **Delete** (trashcan, with confirmation) — blocked with a `400` if the status is
  in use by any time entry, or if it's the project's starting status (set another
  status as starting first)
- **Reorder** with the up/down arrows (swaps `sequence` with the adjacent status)
- **Set as starting** (star icon) — exactly one status per project is the starting
  one at any time; setting a new one automatically un-sets the previous one
  (`ProjectEntryStatusService.setStarting()`)

Every project is seeded with `SUBMITTED` (starting) → `APPROVED` → `REJECTED`, all
active, when created (`ProjectService.create()` calls
`ProjectEntryStatusService.seedDefaultStatuses()`).

A new time entry always starts at its project's **starting** status (not merely the
lowest-`sequence` one — the two can now diverge once ADMIN reassigns starting to a
different status). Currently only ADMIN can move an entry to a different status —
done from the entry's detail page (`/time-entries/{id}`), where the Status field is
an editable dropdown of the entry's project's **active** statuses for ADMIN (an
already-assigned inactive status stays visible/selected even though new selections of
it are hidden), and read-only text for everyone else; `TimeEntryService.update()`
enforces the ADMIN-only rule server-side (403 for a non-admin attempting it) and
rejects a status that belongs to a different project (`400`) — statuses aren't
interchangeable across projects. (The per-project Approver below is a prerequisite
for eventually letting someone other than ADMIN drive this workflow — that
authorization change hasn't been wired in yet.)

### Workflow (which status can follow which)

The `project_entry_status_transition` table records, for a given status, which other
statuses of the *same project* can be reached from it — a directed edge from a
"parent" status to a "depending" (child) status. This is the workflow graph itself,
separate from the flat status list: a status can have any number of depending
statuses, and (via separate transition rows) could itself be reachable from more than
one parent, so it's a general graph, not necessarily a simple tree — nothing in
`ProjectEntryStatusService` assumes otherwise (no cycle detection, no single-parent
constraint), since none was asked for.

Clicking a status row on a project's **Statuses** tab now opens `/statuses/{id}`, with
two tabs:

- **Status details** — Order (read-only), Name, Description, Active, the starting
  indicator/action (a star, or a button to make this the starting status — the same
  action available from the Statuses tab's own list), and **Parent status(es)** — a
  read-only list of every status that can reach *this* one (the reverse of "Depending
  statuses" below), each a link to that parent's own detail page, or "None" if this
  status isn't reachable from any other; **Save** and **Delete** (with confirmation;
  same server-side rules as deleting from the list — blocked if it's the starting
  status or if any time entry references it)
- **Depending statuses** — every status directly reachable from this one, a picker to
  link another of the project's statuses (excluding itself and ones already linked),
  and a trashcan icon per row to unlink (with confirmation); clicking a row navigates
  to *that* status's own detail page, so the graph can be browsed depending-status by
  depending-status

A status's parent list is purely informational here — it's edited from the *parent's*
own "Depending statuses" tab (or the Owner's dependencies dialog), never from the
child's side, and since the graph allows a status to be reachable from more than one
parent (nothing enforces a single-parent tree), this list can show more than one
entry.

Deleting a status cascades at the database level to remove any transition row
involving it, on either side — no orphaned rows possible. Linking/unlinking is
restricted to ADMIN or the project's Owner (see "Owner" below) and is idempotent
(linking an already-linked pair, or unlinking a pair that isn't linked, succeeds as a
no-op, matching every other relationship in this app); linking rejects a self-loop or
a status from a different project (`400`/`404`).

### Approver

Each project optionally has one **Approver** — a single user, set from the
**Project details** tab's "Approver" dropdown, whose candidates are exactly the
project's own accessible users (the same set shown on the project's Users tab: group
members plus all ADMINs). `ProjectService.update()` rejects (`400`) an `approverId`
that isn't one of those users — an approver must actually have a stake in the
project. The dropdown has a clear button, since a project need not have an approver
assigned. The reverse view lives on the user's own page: `/users/{id}`'s **Approver**
tab lists every project that user is the designated approver of (clicking a row opens
that project's detail page) — backed by `GET /api/projects?approverId=`.

### Owner (and who may edit the workflow)

Each project optionally has one **Owner** — set the same way as the Approver (a
"Owner" dropdown on the **Project details** tab, candidates limited to users with
access to the project, `400` otherwise) and looked up the same way
(`GET /api/projects?ownerId=`, and mirrored in a **Owner** tab on `/users/{id}` next
to the Approver tab — every project that user owns, clicking a row opens that
project's detail page). The Owner isn't just informational: editing a
project's workflow — creating, renaming, deleting, or reordering its statuses,
changing which one is the starting status, or changing which statuses are reachable
from which — is restricted to **ADMIN or that project's Owner**; a plain member with
ordinary access to the project cannot, even though they can view everything else about
it. This is enforced in `ProjectEntryStatusService.requireWorkflowEditAccess()` on
every mutating method (not just in a controller annotation), so it applies equally
whether the call comes from the UI or directly through the REST API — a non-owner,
non-admin caller gets a `403` with `"Only the project owner or an ADMIN can edit the
workflow"`.

Because `/projects/{id}` (and everything under it, including the Statuses tab and the
`/statuses/{id}` workflow-graph page) is an ADMIN-only route, a non-admin Owner has no
way to reach it. Instead, **My Projects** (`/my-projects`, in the drawer nav for every
user) lists the projects the current user owns; clicking one opens
**Project workflow** (`/project-workflow/{id}`, its heading showing the project's
name) — a separate, minimal page with the same statuses list, add-status form, and
per-row actions (move up/down, edit, set-starting, delete) as the admin Statuses tab,
plus a "manage dependencies" dialog per status for editing its depending statuses in
place, so an Owner never needs to visit the ADMIN-only pages at all. Clicking a status
row itself (anywhere outside the action buttons) opens the same edit dialog as the
pencil icon, for convenience. `ProjectWorkflowView` is reachable by any authenticated
user at the route level, but checks in `setParameter()` that the caller is ADMIN or
the project's Owner and reroutes to the same "Access denied" page used elsewhere in
the app otherwise.

The Groups page (`/groups`) shows a "Groups" heading and splits its content into two
tabs: **Groups** (the sortable list — Name, Projects and Users columns can all be
sorted) and **Add Group** (the creation form).

Clicking a group row on the Groups page opens `/groups/{id}`, showing the group's name
next to the "← Back to groups" link (kept live after a rename), with three tabs:
**Group details** (rename), **Projects** (the group's permitted projects —
Code/Name/Active columns, click through to a project's detail page, a picker to link an
additional existing project, and a trashcan icon per row to unlink one — with
confirmation), and **Users** (members of that group, likewise click-through to their
detail page, a picker to link an additional existing user, and a trashcan icon per row
to remove one from the group — with confirmation). The Groups list page is create-only
— editing happens on the detail page.

The Users page (`/users`) shows a "Users" heading and splits its content into two
tabs: **Users** (the sortable list — Username, Full name, Email, Role, Groups and
Enabled columns can all be sorted) and **Add User** (the creation form).

Clicking a user row on the Users page opens `/users/{id}`, showing the user's full name
next to the "← Back to users" link (kept live after an edit), with four tabs: **User
details** (edit profile, role, groups — a multi-select, since a user can
belong to any number of groups — enabled status, and — for non-ADMIN users — reset the
password), **Time recordings** (every time entry logged by that user, across all
projects, with a User column alongside Date/Project for consistency with the other
Time Entries lists in the app), **Groups** (every group the user belongs to — click through to a group's
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

The home page (`/`) shows a "Time Report System" heading and splits its content into
two tabs: **Add Time Entry** (the creation form — its Project dropdown only lists your
accessible projects that are currently active) and **Time Entries** (your own entries,
with a User column alongside Date and Project, sortable by Date, User, and Project).

`TimeEntryService` enforces two rules on both creation and editing (a `400` with an
`{"error": "..."}` body otherwise, same as the other bean-validation failures above):
a user's total hours for a single day, across all their entries, can't exceed 24 (an
edit is checked against the day's other entries, excluding the one being edited); and
if the project has a start date and/or end date set, the entry's work date can't fall
outside that range (no check is done for whichever bound, or both, is left unset).

Clicking a row on the Time Entries tab (your own time entries) opens `/time-entries/{id}`
to edit it: Hours and Description are always editable; Status is an editable dropdown
for ADMIN only (see "Approval status" above) and read-only text for everyone else. The
dropdown's options are restricted to the entry's *current* status plus its direct
depending statuses per the project's workflow graph (`ProjectEntryStatusService.findChildren()`
— see "Workflow" above) — not every active status of the project — so ADMIN can only
move an entry one step along its defined path at a time; a status with no depending
statuses (nothing reachable from it) leaves the dropdown with just that one option. An
inactive status stays selectable only if it's the entry's current one (same rule as
before). Project and Date are always read-only (creating a new entry for a different
project/date is a separate action from the home page's form). **Save** persists and
returns to `/`; **Cancel**
returns without saving; **Delete** asks for confirmation first, then deletes and
returns to `/`. Only the entry's own owner can view/edit/delete it — ADMIN bypasses
this, same as the rest of the access model — enforced in `TimeEntryService`, not just
hidden in the UI, so `GET`/`PUT`/`DELETE /api/time-entries/{id}` reject a non-owner,
non-admin caller with 403. The User field is always a link: `/profile` when it's your
own entry, or `/users/{id}` (ADMIN-only) when an admin is viewing someone else's —
since `/profile` always shows the *caller's* own profile and can't target another
user. The Project field is a link to `/projects/{id}` only for an ADMIN viewer (the
only role that can reach that ADMIN-only page); for anyone else it's shown as plain
text, since making it "clickable" for them would only ever lead to the "Access
denied" page.

## My Profile

The user icon button next to "Log out" (any role, not just ADMIN) opens `/profile`,
where you can see your own username, role, groups and enabled status (all read-only —
role and group membership are admin-managed), edit your full name/email, and change
your own password given the current one. This is a separate, self-service path from
the admin-only `UserService.updatePassword()` used on the Users pages — it has no
ADMIN-target restriction, since changing *your own* password is always allowed
regardless of role (unlike an admin resetting *someone else's*). For an ADMIN viewer
only, a "View full user details" link appears right below the "My Profile" heading,
pointing to their own `/users/{id}` page (the fuller admin view, with its Time
recordings/Groups/Projects/Approver/Owner tabs); the link is omitted for everyone else
since `/users/{id}` is an ADMIN-only route and would otherwise be a dead end.

## User Manual

A role-oriented user manual — separate from this developer-facing README — lives at
`ui/src/main/resources/manual/user-manual.md` and is served in the app itself at
`/manual` (`ManualView`, `@PermitAll`, no `MainLayout` chrome — a standalone reading
page). It's converted from Markdown to HTML at request time with `commonmark-java`
and rendered via Vaadin's `Html` component; a `ManualViewSmokeTest` in the `ui`
module's test suite guards against the Markdown ever failing to render into valid
HTML. The drawer nav has a "User Manual" link (a plain `Anchor` with
`target="_blank"`, not a `RouterLink`, specifically so it opens in a new browser tab
rather than navigating away from whatever page you're on) available to every logged-in
user regardless of role. The manual itself is organized into four sections — every
user (logging time entries), Approver, Project Owner (with a full worked example of
building a workflow from scratch), and Administrator — matching the per-project
Approver/Owner responsibilities and system-wide ADMIN role described above.

## REST API

All endpoints require HTTP Basic auth (same users as the UI). Endpoints that create or
update projects, groups, or users require the ADMIN role.

- `GET /api/projects` (optionally `?approverId=` or `?ownerId=` for projects a user is
  the approver/owner of), `GET /api/projects/{id}`, `POST /api/projects` (admin),
  `PUT /api/projects/{id}` (admin) — the update body's `approverId`/`ownerId` must each
  belong to a user with access to the project (`400` otherwise)
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
- `GET /api/time-entries/{id}`, `PUT /api/time-entries/{id}` (hours/description always;
  an optional `statusId` too, but only ADMIN may set one — 403 for anyone else, 400 if
  it belongs to a different project than the entry's), `DELETE /api/time-entries/{id}`
  — all three restricted to the entry's own owner or an ADMIN (403 otherwise)
- `GET /api/projects/{projectId}/statuses` — any authenticated user
- `GET /api/projects/{projectId}/statuses/{statusId}` — any authenticated user
- `POST /api/projects/{projectId}/statuses` (ADMIN or the project's Owner — see
  "Owner" above) — body: name + optional description; appends at the end of the
  project's sequence, active, not starting; 400 on a duplicate name within the project
- `PUT /api/projects/{projectId}/statuses/{statusId}` (ADMIN or Owner) — body: name,
  description, active
- `DELETE /api/projects/{projectId}/statuses/{statusId}` (ADMIN or Owner) — 400 if
  it's the project's starting status, or if any time entry still references it
- `PUT /api/projects/{projectId}/statuses/{statusId}/move-up`,
  `PUT .../move-down` (ADMIN or Owner) — swaps `sequence` with the adjacent status;
  returns the project's updated status list
- `PUT /api/projects/{projectId}/statuses/{statusId}/set-starting` (ADMIN or Owner) —
  makes this the project's starting status, un-setting whichever status held it before
- `GET /api/projects/{projectId}/statuses/{statusId}/children` — any authenticated
  user; the statuses directly reachable from this one (the workflow graph)
- `GET /api/projects/{projectId}/statuses/{statusId}/parents` — any authenticated
  user; the statuses this one is directly reachable from (the reverse edge; can be
  more than one, since nothing enforces a single-parent tree)
- `PUT /api/projects/{projectId}/statuses/{statusId}/children/{childStatusId}`
  (ADMIN or Owner) — links `childStatusId` as reachable from `statusId` (a no-op, not
  an error, if already linked); 400 on a self-loop, 404 if either status doesn't
  belong to `projectId`
- `DELETE /api/projects/{projectId}/statuses/{statusId}/children/{childStatusId}`
  (ADMIN or Owner) — unlinks (a no-op, not an error, if not linked)

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
