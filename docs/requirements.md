# MiTiRe — Prompts to Build This System From Scratch

This document reconstructs the sequence of prompts that would be needed to take an
empty repository to the MiTiRe application as it exists today. It is not a
transcript — the real development history (in [`mitire-prompts.md`](mitire-prompts.md),
[`mitire-prompts-20260901.txt`](mitire-prompts-20260901.txt) and
[`mitire-prompts-20260902.txt`](mitire-prompts-20260902.txt)) is full of
back-and-forth: bug reports, one-off corrections, questions the assistant asked back,
and small UI nudges given one at a time. This document distills that history into the
smaller, logically ordered set of prompts that would get you to the same result,
grouped by the part of the system each one builds. Pure process noise (greetings,
"OK", requests to save the prompt log itself) is left out; everything that shaped the
running application is kept. Order and details were cross-checked against the git
history and the current `README.md`, which occasionally corrected the raw prompt log
(e.g. the workflow-status feature needed an unlogged foundational step before the
prompts about it make sense — see §4).

Each numbered item below is phrased the way it would actually need to be asked —
these are prompts, not a spec written after the fact.

## 1. Project bootstrap

1. Build an application, based on Java and Spring Boot, to manage time reports for a
   team of people working on different projects. It should have a frontend and a
   backend communicating via REST APIs: Vaadin for the UI, Gradle as the build tool,
   a REST API other modules could also call. Initialize it as a local git repository.
2. Change the Java package from `com.mitire` to `it.brunasti.mitire`.
3. Run the application against a local PostgreSQL instance on port 5432 instead of
   Docker (and, when it fails to authenticate: resolve the Postgres.app "trust"
   authentication rejection).
4. Fix the "Denied access to view ... due to parent layout access rules" navigation
   error on startup — the base layout and login view need to be reachable
   unauthenticated.

## 2. Users, groups, projects, and access control

5. As an ADMIN, add a menu entry to manage users, where a user can belong to a group,
   a group can be granted access to multiple projects, and a user inherits project
   access from the groups they belong to.
6. Let an ADMIN edit the password of a non-ADMIN user.
7. Give the project list a detail page (`/projects/<id>`) with three tabs: "Project
   details", "Time Entries", "Users".
8. Give the group list a detail page (`/groups/<id>`) with three tabs: "Group
   details", "Projects", "Users".
9. Give the user list a detail page (`/users/<id>`) with four tabs: "User details",
   "Time recordings", "Groups", "Projects".
10. Add navigation from a project's/group's Users tab to each user's detail page, and
    the reverse navigation back (user → project, user → group).
11. Render a group's Projects tab the same way the user's Projects tab is rendered,
    then remove the "Accessible projects" dropdown from it.
12. Change the User–Group relationship from many-to-one to many-to-many — a user can
    belong to more than one group.
13. Add the application's icon as both the favicon and the navbar logo (top-left,
    next to the drawer toggle).
14. On the user detail page's Groups tab, let an admin link an existing group. Do the
    reverse on the group detail page (link an existing user), and the same again
    between group and project.
15. Add a "Groups" tab to the project detail page, reciprocal to the group's
    "Projects" tab.
16. Next to "Log out", add a user-icon button that opens a self-service page where
    the logged-in user can view their own details and change their own password.
17. On the group, project, and user detail pages, let each row in a linked list
    (projects/users/groups) be unlinked via a trashcan icon, with a confirmation
    prompt.
18. Next to the "← Back to …" link on the project/group/user detail pages, show the
    name of the entity itself.
19. Add a VIEWER role, alongside ADMIN and MEMBER, that can only see projects, not
    edit anything.
20. Add a role attribute to Group: a user's *effective* role on a project is the most
    privileged of (a) their own intrinsic role and (b) the role of any group they
    belong to that has access to that project — e.g. an intrinsically VIEWER user who
    is in an ADMIN-role group with access to project P is effectively ADMIN on P.

## 3. Time entries

21. On the home page, let a user log time entries and list the ones they've already
    logged; let them edit an entry (hours, description) via an edit page with Save /
    Cancel / Delete (Delete needs confirmation), returning to the list afterward.
22. Separate the "Add Time Entry" form from the list of entries on the home page (and
    tune their order/position).
23. Exclude inactive projects from the "Add Time Entry" project dropdown.
24. Validate time entries on create/edit: no more than 24 hours per day across a
    user's existing entries, and the entry date must fall within the project's
    start/end dates when those are set.
25. Fix that 24-hour cap: it must be scoped per *person* per day, not per
    project per day — someone logging hours against two different projects on the
    same day still can't exceed 24 hours total.
26. Make validation warnings a more visible colored popup (e.g. orange), not a plain
    message.
27. Make the entry Description field a multi-line text area.
28. Record a `created_at` timestamp on every time entry.
29. Let a user attach notes to a time entry — each note records who wrote it, when,
    and a free-text body (as a text area).
30. Rename the "Time Entries" menu item to "My Time Entries"; add an admin-only "All
    Time Entries" page listing every user's entries.
31. Add filters to every time-entry list: by project, by user, and by time period
    (last/selected week, last/selected month, a single day, or a from–to range).
32. Give a time entry its own detail page (`/time-entries/<id>`) showing who created
    it (linked to their profile) and which project it belongs to (linked to the
    project), reachable by clicking a row in any time-entry list.
33. Add the entry owner's user name as a column on every time-entry list.

## 4. Project workflow / status engine

34. Rename every table and class that currently says "entity" (`time_entity`,
    `project_entity`, …) to "entry" (`time_entry`, `project_entry_status`, …).
35. Replace the time entry's fixed status field with a per-project, admin-managed
    list of named statuses: add, rename/edit, delete (blocked if any entry is using
    the status), reorder, and mark exactly one as the project's "starting" status at
    a time. Seed every new project with a default `SUBMITTED` (starting) →
    `APPROVED` → `REJECTED` set. A new time entry always starts at its project's
    current starting status. For now, only ADMIN can move an entry to a different
    status.
36. Give a project an optional single **Approver** — a dropdown on the Project
    details tab, with a clear option, whose candidates are limited to users who
    already have access to the project. Add the reverse view: an "Approver" tab on
    the user detail page listing every project that user approves.
37. Add a special user to the project, defined as the **Owner**, who can edit the
    project's workflow — even an ADMIN can, but a normal member of the project
    cannot. Assign the Owner the same way as the Approver (a dropdown on Project
    details, same candidate restriction), with a reverse "Owner" tab on the user
    detail page.
38. Now build the workflow itself: a relation table linking a `project_entry_status`
    to its "parent" status(es), so an entry in one status can only move into another
    if the target is a direct child of its current one. Give each status a detail
    page with its own fields, a read-only list of parent statuses that can reach it
    (each linked), and a "Depending statuses" tab listing the statuses directly
    reachable from it, with a picker to link another of the project's statuses and a
    trashcan icon (with confirmation) to unlink one.
39. Since the project detail page is ADMIN-only, give the Owner their own way in:
    a "My Projects" page (in the drawer nav) listing the projects the current user
    owns, linking to an owner-facing `/project-workflow/<id>` page — the same layout
    as the project detail page, but read-only except for the Workflow tab, where the
    owner can click a status to edit it or manage its dependencies. Rename the
    "Statuses" tab to "Workflow" everywhere.
40. Show a "Next statuses" column on the Workflow tab's status list.
41. Add a `time_entry_transition` audit table recording, on every status change: the
    old status, the new status, the timestamp, and who made the change. Show it as a
    list at the bottom of the time entry detail page.
42. On the project detail page's Users tab, add columns showing which user (if any)
    is that project's Approver and which is its Owner.
43. On the time entry detail page, restrict the (still ADMIN-only) status dropdown
    to the entry's current status plus whatever statuses are directly reachable from
    it via the workflow graph, instead of every active status in the project.

## 5. Workflow diagram

44. Render a project's status workflow as a diagram (statuses as boxes, one arrow per
    allowed transition), shown next to the Workflow tab.
45. Give the diagram its own tab that uses the full available space, since the inline
    version is too cramped.
46. Fix the arrow direction (they were pointing the wrong way); auto-trim excess
    whitespace around the diagram and keep it centered.
47. Reduce the margin/border space around the diagram further.
48. Comment the layering algorithm so it's clear how a status's column is derived,
    and cap the number of levels at the number of statuses so a malformed workflow
    graph can't loop forever.
49. Guarantee the starting status always renders as the leftmost box — including
    when a transition cycles back into it (e.g. a "resubmit" transition) — without
    letting that cycle push it, or anything reachable from it, further right.
50. Fix the case where a cycle *not* involving the starting status (e.g. a chain that
    loops back on itself several statuses later) inflates the column numbering with
    many empty, unused columns, leaving the starting status visually stranded far
    from the rest of the diagram.
51. When two or more transitions would otherwise overlap on the same line (typical
    once a workflow has both a "skip a step" transition and a cycle), stagger the
    status columns diagonally and route each transition as a right-angle
    (horizontal-then-vertical) connector instead of a curve, so every transition is
    visually distinct.
52. Route the "backward" transitions (the cycle edges) so they leave from the
    *bottom* of the source status and arrive at the *bottom* of the target status,
    via a lane below the rest of the diagram — so a workflow cycle reads visually
    differently from a normal forward transition.

## 6. Cross-cutting UI polish

53. Give the Projects/Groups/Users list pages a page title next to the "Mitire"
    brand, split each into a "List" tab and an "Add new" tab, and make every column
    sortable (in particular Name).
54. Add a user column to the Groups list (mirroring the one on Projects).
55. Replace the raw "Could not navigate to 'x'" error with a proper access-denied
    page.
56. Turn the top-left icon into a button that navigates home.
57. Show the logged-in user's name next to their profile icon in the top bar.
58. Rename the brand from "Mitire" to "MiTiRe".
59. Switch the application font from the default serif to a sans-serif (Tahoma).
60. Narrow the columns in list views that hold short values (order numbers, boolean
    flags like Active/Starting) instead of sizing every column equally.
61. Show the MiTiRe icon on the login page too.

## 7. Documentation and local dev tooling

62. Write a start/stop/restart shell script for running the app locally against
    Postgres, in the style of an existing example script (build the jar, track a pid
    file, tail the log).
63. Whenever port 8080 is already in use, restart the existing process automatically
    instead of asking first; after verifying a change works, leave the app running
    rather than stopping it.
64. Write a Markdown user manual covering four audiences — the regular user filling
    in their own time reports, a project's approver, a project's owner (including how
    to set up a workflow), and the global admin — with a linked table of contents.
    Link it from the left-hand menu as the last item, separated from the rest by a
    divider, opening in a new page. Make sure its section links resolve against the
    manual's own page rather than the application root.

---

*Derived from the actual prompt history in this `docs/` directory
(72 logged prompts through 2026-09-02, continued in `mitire-prompts.md` through
2026-09-22), cross-checked against the git history and the current `README.md`/source
tree. Items 35 and 36 in §4 fill a gap in the logged history: the git log shows the
per-project status list and Approver assignment were built (`483323c`, `829ed93`)
before the OWNER/workflow-graph work the log does capture, but no distinct prompt for
that step survived — its wording here is inferred from the resulting code, not quoted.*
