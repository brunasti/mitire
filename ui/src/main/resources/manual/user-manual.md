# MiTiRe User Manual

MiTiRe is a team time reporting application. Everyone logs the hours they worked
against a project; projects have an approval workflow that tracks each time entry
from submission to its final state.

This manual is organized by role:

- **Every user** — logging and managing your own time entries
- **Approver** — the person designated to review a project's entries
- **Project Owner** — the person who designs and maintains a project's approval workflow
- **Administrator** — full management of users, groups, projects and workflows

Your role (or roles — they can overlap) determines which of these sections apply to
you. Anyone can be a project's Approver or Owner regardless of their system role
(ADMIN/MEMBER); Owner and Approver are per-project responsibilities, not separate
login roles.

---

<a id="sec-index"></a>

## Index

- <a href="/manual#sec-every-user" router-ignore>1. Every user: logging your time reports</a>
  - <a href="/manual#sec-logging-in" router-ignore>Logging in</a>
  - <a href="/manual#sec-adding-entry" router-ignore>Adding a time entry</a>
  - <a href="/manual#sec-viewing-editing" router-ignore>Viewing and editing your entries</a>
  - <a href="/manual#sec-your-profile" router-ignore>Your profile</a>
- <a href="/manual#sec-approver" router-ignore>2. Approver: reviewing a project's time entries</a>
  - <a href="/manual#sec-approver-projects" router-ignore>Finding out which projects you approve</a>
  - <a href="/manual#sec-approver-role" router-ignore>What being the Approver means today</a>
- <a href="/manual#sec-owner" router-ignore>3. Project Owner: designing the approval workflow</a>
  - <a href="/manual#sec-owner-projects" router-ignore>Finding your projects</a>
  - <a href="/manual#sec-workflow-model" router-ignore>Understanding the workflow model</a>
  - <a href="/manual#sec-workflow-example" router-ignore>Building a workflow from scratch — worked example</a>
  - <a href="/manual#sec-status-detail" router-ignore>Viewing more detail on a single status</a>
- <a href="/manual#sec-admin" router-ignore>4. Administrator: full system management</a>
  - <a href="/manual#sec-access-model" router-ignore>Users, Groups and Projects — the access model</a>
  - <a href="/manual#sec-setup-project" router-ignore>Setting up a project</a>
  - <a href="/manual#sec-manage-users" router-ignore>Managing users</a>
  - <a href="/manual#sec-move-entry" router-ignore>Moving a time entry through its workflow</a>
  - <a href="/manual#sec-everything-else" router-ignore>Everything else</a>

**Quick links:** <a href="/manual#sec-adding-entry" router-ignore>Add a time entry</a> &middot;
<a href="/manual#sec-viewing-editing" router-ignore>Edit or delete an entry</a> &middot;
<a href="/manual#sec-your-profile" router-ignore>Change your password</a> &middot;
<a href="/manual#sec-workflow-example" router-ignore>Build a workflow</a> &middot;
<a href="/manual#sec-setup-project" router-ignore>Set up a project</a> &middot;
<a href="/manual#sec-manage-users" router-ignore>Manage users</a>

---

<a id="sec-every-user"></a>

## 1. Every user: logging your time reports

This section applies to everyone, regardless of any other responsibility you may have.

<a id="sec-logging-in"></a>

### Logging in

Open the application and sign in with your username and password. You'll land on the
home page (`/`), titled "Time Report System", with two tabs: **Add Time Entry** and
**Time Entries**.

<a id="sec-adding-entry"></a>

### Adding a time entry

On the **Add Time Entry** tab:

1. **Project** — pick from the dropdown. Only projects you currently have access to,
   and that are active, are listed. (Access comes from the groups you belong to; if a
   project you expect to see is missing, ask your administrator to check your group
   membership.)
2. **Date** — defaults to today, but can be changed. If the project has a start and/or
   end date configured, your work date must fall inside that range.
3. **Hours** — between 0.25 and 24, in quarter-hour steps.
4. **Description** — optional free text describing the work.
5. Click **Submit**.

Two rules are enforced no matter what:

- Your **total hours for a single calendar day cannot exceed 24**, added up across
  *all* your entries that day — even if they're logged against different projects.
  If you already logged 20 hours today on Project A, you can only log up to 4 more
  today, whether that's on Project A or any other project.
- The entry's date must respect the project's start/end date, if the project has
  them set.

If either rule is violated, you'll see an error message and the entry won't be saved.

<a id="sec-viewing-editing"></a>

### Viewing and editing your entries

The **Time Entries** tab lists your own entries — Date, User (yourself), Project,
Hours, Description and Status — sortable by Date, User or Project.

Click a row to open it (`/time-entries/{id}`). From there:

- **Hours** and **Description** are always editable by you.
- **Date** and **Project** cannot be changed once an entry is created — if you logged
  the wrong project or date, delete the entry and create a new one instead.
- **Status** is shown as read-only text for you — only an Administrator can move an
  entry to a different status (see the Approver and Administrator sections below for
  how that workflow works).
- **Save** keeps your changes and returns to the home page.
- **Cancel** discards your changes and returns to the home page.
- **Delete** asks for confirmation, then removes the entry permanently.

You can only view, edit or delete your **own** entries. Attempting to reach someone
else's entry (e.g. by guessing its URL) is blocked.

<a id="sec-your-profile"></a>

### Your profile

Click the person icon in the top bar (next to "Log out") to open **My Profile**
(`/profile`). Here you can:

- See your username, role, groups and enabled status (these are read-only — an
  administrator manages them).
- Edit your full name and email address, then click **Save profile**.
- Change your own password: enter your current password, then the new one twice, and
  click **Change password**.

<a href="/manual#sec-index" router-ignore>↑ Back to index</a>

---

<a id="sec-approver"></a>

## 2. Approver: reviewing a project's time entries

A project can have one designated **Approver** — a user responsible for reviewing
that project's time entries. Any user with access to the project (or an
Administrator) can be set as its Approver; this is configured by an Administrator on
the project's detail page.

<a id="sec-approver-projects"></a>

### Finding out which projects you approve

Open **My Profile** (`/profile`) and, if you're an Administrator, follow "View full
user details" to your own user page, which has an **Approver** tab listing every
project you're the designated approver for. (If you're not an Administrator, ask
your administrator to confirm which projects list you as Approver — you can also
recognize it from a project's **Users** tab, where your row shows "Yes" in the
**Approver** column.)

<a id="sec-approver-role"></a>

### What being the Approver means today

Being flagged as a project's Approver identifies you, to everyone with visibility
into the project, as the person responsible for reviewing its time entries — it
appears on the project's Users tab and is discoverable from your own profile.

At present, actually **moving** a time entry from one status to the next (for
example, from "Submitted" to "Approved") is still performed by an Administrator from
the entry's detail page — the Approver designation does not yet grant you that
button yourself. In practice, this means: review the project's time entries (via the
project detail page, if you have access, or by asking an Administrator for a report),
and coordinate with an Administrator to advance entries through the workflow that the
project's Owner has defined (see the next section for how that workflow is built).

<a href="/manual#sec-index" router-ignore>↑ Back to index</a>

---

<a id="sec-owner"></a>

## 3. Project Owner: designing the approval workflow

A project can have one designated **Owner** — the person responsible for defining
*how* that project's time entries move from submission to approval. Like the
Approver, the Owner is a per-project responsibility assigned by an Administrator on
the project's detail page; it doesn't require the ADMIN system role.

**Only an Administrator or the project's Owner can edit that project's workflow.** A
regular member with ordinary access to the project can see the workflow but cannot
change it.

<a id="sec-owner-projects"></a>

### Finding your projects

Once you've been made the Owner of one or more projects, a **"My Projects"** entry
appears in the left-hand navigation menu for you automatically. Open it to see the
list of projects you own. Click a project to open its **Project workflow** page
(`/project-workflow/{id}`), which shows the project's name and everything you need to
manage its workflow — you don't need (or have) access to the full administrative
project page.

<a id="sec-workflow-model"></a>

### Understanding the workflow model

A project's approval workflow is made of two things:

1. **Statuses** — the named stages a time entry can be in (e.g. "Submitted",
   "In review", "Approved", "Rejected"). Each status has:
   - A **name** and optional **description**.
   - An **order** (its position in the list — mostly for display; the actual allowed
     path between statuses is defined separately, see below).
   - An **active** flag — inactive statuses can no longer be assigned to *new*
     transitions, but an entry already sitting in an inactive status keeps showing it.
   - Exactly one status per project is the **starting status** — every new time entry
     created in this project begins there.
2. **Dependencies** (the workflow graph) — for each status, which *other* statuses of
   the same project can be reached from it next. This is what actually restricts how
   an entry can move: an Administrator changing an entry's status can only pick the
   entry's current status or one of its direct dependencies — not just any active
   status in the project.

<a id="sec-workflow-example"></a>

### Building a workflow from scratch — worked example

Suppose you want: **Submitted → In review → Approved**, with **Submitted → In
review → Rejected** also possible, and no way to go anywhere from Approved or
Rejected. Every new project starts pre-seeded with `SUBMITTED → APPROVED →
REJECTED` (with `SUBMITTED` as the starting status), so you'll typically be adjusting
that default rather than starting completely empty. Here's how, from the
**Project workflow** page:

1. **Add the statuses you need.** Use the "Add" form (name + optional description) to
   create any status that doesn't already exist — in this example, add "In review".
   New statuses are created active, and never as the starting status automatically.
2. **Check the starting status.** Exactly one status has the star icon marking it as
   the starting point for new time entries — by default this is "Submitted". Click
   the outlined star button next to a status to make it the starting one instead, if
   needed (this automatically un-marks whichever status held it before).
3. **Define the dependencies.** Click a status row (or its pencil/edit icon) to open
   its edit dialog, which also lets you manage its dependencies — the statuses
   reachable *from* it:
   - From **Submitted**, add "In review" as a dependency (and remove "Approved" and
     "Rejected" directly from it, if you no longer want entries to skip review).
   - From **In review**, add both "Approved" and "Rejected" as dependencies.
   - Leave **Approved** and **Rejected** with no dependencies, since nothing should
     follow them.
4. **Reorder if you like.** The up/down arrows next to each status change its display
   order — this is cosmetic only and doesn't affect which transitions are allowed.
5. **Delete what you don't need.** A status can be deleted (with confirmation) as
   long as it isn't the current starting status and no time entry currently uses it —
   remove or reassign those first.

Once this is set up, when an Administrator opens a time entry currently in
"Submitted", the status dropdown will only offer "Submitted" (staying put) or "In
review" — not "Approved" or "Rejected" directly, since those aren't direct
dependencies of "Submitted" anymore.

<a id="sec-status-detail"></a>

### Viewing more detail on a single status

For finer inspection, each status also has its own detail page reachable from an
Administrator's view of the project (`/statuses/{id}`), showing the same
dependency management plus the reverse view — every status that can reach *this* one
(its "parent" statuses). As Owner, you manage everything through the simpler
**Project workflow** page described above; you don't need this admin-only page.

<a href="/manual#sec-index" router-ignore>↑ Back to index</a>

---

<a id="sec-admin"></a>

## 4. Administrator: full system management

Administrators can access every project regardless of group membership, and are the
only role that can manage projects, groups and users. The left-hand menu shows
**Projects**, **Groups** and **Users** entries only when you're logged in as an
Administrator.

<a id="sec-access-model"></a>

### Users, Groups and Projects — the access model

Projects aren't assigned to users directly:

- A **Group** holds a set of projects it grants access to.
- A **User** belongs to any number of groups, and gets the union of all their groups'
  project access.
- Administrators bypass this and can always access every project.

From **Users** (`/users`), **Groups** (`/groups`) and **Projects** (`/projects`) you
can create new ones and open any existing one to edit it. Every detail page
cross-links the others — for example, a project's **Users** and **Groups** tabs, a
group's **Users** and **Projects** tabs, and a user's **Groups** and **Projects**
tabs — each with a picker to link another one and a trash icon to unlink, with
confirmation.

<a id="sec-setup-project"></a>

### Setting up a project

1. Create the project from **Projects → Add Project**, with an optional start/end
   date.
2. Link one or more **Groups** to it (from the project's Groups tab, or the other way
   round from a group's Projects tab) so the right users can access it.
3. On the project's **Project details** tab, optionally assign an **Approver** and an
   **Owner** — both must be chosen from users who already have access to the project
   (group members or other Administrators).
4. New projects come pre-seeded with a simple `Submitted → Approved`/`Rejected`
   workflow. As Administrator you can adjust it yourself from the project's
   **Workflow** tab (identical capabilities to what an Owner has from their
   **Project workflow** page — see the Owner section above for the full workflow
   walkthrough), or leave it to the project's Owner.

<a id="sec-manage-users"></a>

### Managing users

From a user's detail page (`/users/{id}`) you can edit their full name, email, role,
enabled status, and group memberships, and — for non-Administrator users — reset
their password. You can also see every time entry that user has ever logged (**Time
recordings** tab), every group they belong to, and every project they can access.

<a id="sec-move-entry"></a>

### Moving a time entry through its workflow

Only an Administrator can change a time entry's status, from the entry's own detail
page (`/time-entries/{id}`). The Status dropdown only offers the entry's current
status plus whatever the project's Owner (or another Administrator) has defined as
its direct next steps — see "Building a workflow from scratch" above for how those
options are determined. If a status has no further dependencies, the dropdown will
only contain that one status.

<a id="sec-everything-else"></a>

### Everything else

Anything a normal user, Approver or Owner can do, an Administrator can do too, with
unrestricted access to every project. The sections above still apply to you in those
capacities — this section only covers what's exclusive to the Administrator role.

<a href="/manual#sec-index" router-ignore>↑ Back to index</a>
