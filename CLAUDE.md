# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Multi-tenant event registration application for JUG (Java User Group) events. Built on Quarkus 3.x / Java 21 / PostgreSQL. Designed to be embedded in an `<iframe>` on JUG websites.

Live URL pattern: `https://registration.ijug.eu`

## Common Commands

```bash
# Start in dev mode (spins up PostgreSQL, Mailpit, Keycloak via DevServices automatically)
./mvnw compile quarkus:dev

# Build
./mvnw clean package

# Run all tests (uses Testcontainers for PostgreSQL)
./mvnw test

# Run a specific test class
./mvnw test -Dtest=RegistrationAndDeletionFunctionalTest

# Native build
./mvnw clean package -Pnative
```

### Dev Mode URLs
- Registration form: http://localhost:8080/registration/test?eventId=2026-12-31&opensBeforeInMonths=8
- Admin UI: http://localhost:8080/admin/test/events (credentials: `alice` / `alice`)
- Mailpit UI: http://localhost:8080/q/dev-ui/quarkus-mailpit/mailpit-ui
- Keycloak dev server: http://localhost:8081

## Architecture

### Multi-Tenancy
The app serves multiple JUGs from a single deployment. The tenant is always the first path segment after the resource type (e.g., `/registration/{tenant}`, `/admin/{tenant}/events`).

- **`TenantAccessFilter`** — JAX-RS `ContainerRequestFilter` that reads `{tenant}` from the path, enforces that the authenticated user's OIDC roles include that tenant name, then sets the resolved tenant on `TenantContext`.
- **`TenantContext`** — `@RequestScoped` CDI bean carrying the current `tenantId` and lazy-loading the `Tenant` entity.
- **Hibernate DISCRIMINATOR multi-tenancy** — `Registration`, `Content`, and `Tenant` entities use `@TenantId` so all DB queries are automatically scoped. `CurrentTenantResolver` feeds the tenant value from `TenantContext`.

Known tenants (seeded in `V3__tenant.sql`): `test`, `jugda`, `cyberland`.

New tenants are created at runtime by **`TenantProvisioningService`** (`GET/POST /admin/{tenant}/tenants`,
role `admin`), which clones the `test` tenant: the `tenant` row and all its `content` rows are copied, only
id and name come from the form. It uses **native SQL on purpose** — `Tenant` and `Content` carry `@TenantId`,
so every JPA query is filtered to the *current* request's tenant, which is exactly the isolation this one
operation has to cross. Keycloak is not touched: a new tenant is unreachable in the admin UI until someone
creates the client role of the same name by hand.

### Endpoints

| Path | Auth | Description |
|---|---|---|
| `GET /registration/{tenant}` | anonymous | Show registration form |
| `POST /registration/{tenant}` | anonymous | Submit registration |
| `GET/POST /registration/{tenant}/delete` | anonymous | Self-service deregistration |
| `DELETE /registration/{tenant}/delete?id=` | anonymous | Delete one registration by UUID (used by the admin UI and the mail link) |
| `GET /registration/{tenant}/ical/{eventId}` | anonymous | Download `.ics` calendar file |
| `GET /webinar/{tenant}/{eventId}` | anonymous | Webinar landing page (today-only in prod) |
| `GET /admin/{tenant}/events` | OIDC | Admin overview of all events |
| `GET /admin/{tenant}/events/{eventId}` | OIDC | List registrations for one event (HTML, or JSON with `Accept: application/json`) |
| `GET/POST /admin/{tenant}/data` | OIDC | View and edit the tenant's master data |
| `GET/POST /admin/{tenant}/content` | OIDC | View and edit the tenant's help texts (`Content`) |
| `GET/POST /admin/{tenant}/tenants` | OIDC + role `admin` | Create a new JUG by cloning the `test` tenant |
| `GET /admin/{tenant}/logout` | OIDC | RP-initiated logout, returns to `/admin/{tenant}` |
| `GET /admin/{tenant}/logs` | OIDC + role `admin` | Tail the server log |
| `PUT /admin/{tenant}/events/{eventId}/data` | OIDC | Update event metadata |
| `PUT /admin/{tenant}/events/{eventId}/message` | OIDC | Send bulk email to participants |

### Key Components

- **`RegistrationResource`** — handles registration form display and submission; decides between `registration`, `closed`, and `not_yet_open` templates based on deadline / `opensBeforeInMonths`.
- **`RegistrationService`** — persists a registration (upsert on eventId+email), triggers confirmation email.
- **`EventService`** — fetches events from an external JSON URL (per tenant, see `Tenant.events`), cached with Caffeine (`events` cache, 5-min TTL).
- **`EmailService`** — sends confirmation, waitlist-to-attendee, and bulk emails via Quarkus Mailer + Qute templates.
- **`CleanupJob`** — scheduled job that purges expired registrations (based on `ttl` epoch seconds, set to 1 week after the event).
- **`Content`** — tenant-scoped key/value texts stored in the `content` table; injected into templates via `Content.asMap()` as `helptext`.
- **`ContentKey`** — enum of the help-text keys the templates actually read. It is the single source of truth for the
  admin "Texte" form (`AdminContentResource` / `ContentService`): only these keys are rendered and saved, unknown
  form fields are dropped. `ContentKeyTemplateTest` scans the templates for `helptext["..."]` and fails in both
  directions — a lookup with no enum constant (nobody can fill it) and an enum constant no template renders
  (editing it does nothing).

### Data Model
- `Registration` — one row per participant per event; `ttl` auto-expires ~1 week post-event.
- `Tenant` — configuration per JUG: name, website, privacy/imprint URLs, logo, reply-to address, events JSON URL.
- `EventData` — mutable per-event key/value metadata, editable by admins.
- `Content` — tenant-scoped UI help texts (name field hint, email hint, video recording notice, etc.).

### Templates
Qute HTML templates in `src/main/resources/templates/`. Mail templates are in `templates/mail/`. All templates share `template.html` as the base layout via Qute includes.

**Page scripts belong in the `{#scripts}` block, not in `{#body}`.** `template.html` loads jQuery and Bootstrap at the end of `<body>` and then offers a `{#insert scripts}{/}` slot. A `<script>` inside `{#body}` is rendered before the library tags and therefore runs before `$` exists -- with no visible error, the page just stays dead.

**Inline JS with object literals needs a Qute raw block `{|` ... `|}`.** Qute parses
`{` as the start of an expression as soon as an identifier follows directly. A JS object literal
like `{type: 'x', height: h}` is therefore read as an expression and the page dies at runtime
with `No namespace resolver found for [type]`. The existing inline JS in `admin/list.html` only
works because every `{` there happens to be followed by a line break -- coincidence, not
protection. The height reporter in `template.html` is wrapped in `{|` ... `|}` accordingly.

`async`/`defer` are deliberately not set on the library tags: `defer` has no effect on inline scripts, so a deferred jQuery tag would still be overtaken by the slot scripts. Anyone who wants `defer` has to wrap every slot script in `DOMContentLoaded`.

### Database Migrations
Flyway migrations in `src/main/resources/db/migration/`. Run automatically at startup. Schema is managed solely via Flyway (`quarkus.hibernate-orm.schema-management.strategy=none`).

### Participant Mails via CDI Events (asynchronous)

No participant mail is sent inside the JTA transaction any more. The services only do DB work and
fire a CDI event from `de.jugda.registration.event` at the end:

- `RegistrationService.handleRegistration()` -> `RegistrationConfirmed`
- `DeleteService.processWaitlist()` -> `WaitlistPromoted`

Both are records `(tenantId, baseUrl, RegistrationDto)` and implement the sealed interface
`RegistrationEvent`. `EmailService` hangs exactly two observers off it -- **not one per event type,
but one on the supertype**, because CDI resolves observers through the runtime class and its
supertypes:

1. `relayAfterCommit(@Observes(during = AFTER_SUCCESS) RegistrationEvent)` -- runs only after the
   commit, so no mail goes out for a rolled-back transaction, and re-fires the same event via
   `fireAsync()`. `fireAsync` only reaches `@ObservesAsync` methods, so there is no loop.
2. `onRegistrationEvent(@ObservesAsync RegistrationEvent)` -- renders and sends the mail on a worker
   thread. Which template and which subject is decided by a `switch` over the sealed interface: a new
   event type therefore breaks compilation instead of silently sending no mail.

**Important for the async observer:** there is no HTTP request active there. `UriInfo` is therefore
unusable -- the base URL travels in the event payload. The request *context*, on the other hand, is
present: ArC activates it around every observer notification (`EventImpl.Notifier.notify`, as long as
`quarkus.arc.strict-compatibility` is off -- the default). So it does not have to be activated by
hand; the only thing that must be set is `TenantContext.tenantId` from the payload, otherwise
`CurrentTenantResolver`, `EventService` and `Tenant.findById()` come up empty. (There used to be a
manual `Arc.container().requestContext().activate()` here; a probe showed the context is already
active and the block never ran.) `@ActivateRequestContext` would be ineffective anyway, because
interceptors do not apply to observer methods.

Exceptions from asynchronous observers end up in a `CompletionStage` nobody inspects -- which is why
`onRegistrationEvent()` logs by itself. Consequence: a failed send no longer rolls the transaction
back, and the user sees the thank-you page regardless.

So that this does not stay invisible, `registration.confirmationSentAt` carries the state:

- `null` means *mail still pending* -- set when a registration is created or updated
  (`RegistrationService.handleRegistration`) and when someone moves up from the waitlist
  (`DeleteService.processWaitlist`).
- The timestamp is only set after a send that threw no exception, via
  `RegistrationService.markConfirmationSent(...)`. That has to be a **separate bean call**, otherwise
  the `@Transactional` interceptor does not apply (self-invocation), and there is no transaction on
  the worker thread anyway.
- `admin/list.html` shows an icon per row: a green envelope with a timestamp, otherwise a red one.
  A brief red right after a registration is normal -- the send is asynchronous.

Migration `V7` backfills existing rows with `created`: back then the send hung inside the transaction,
so a persisted row implies a delivered mail. Without the backfill every old registration would have
turned red after the deploy.

Test pitfall: assertions that read the DB need `pollInSameThread()`. Otherwise Awaitility polls on a
thread of its own, and `CurrentTenantResolver` is `@RequestScoped` -- the query dies with *no tenant
identifier specified*. Mailbox assertions are unaffected (`MockMailbox` is a singleton).

The bulk mail (`EmailService.sendBulkEmail`, admin UI) still runs synchronously in the request -- it
has no transaction problem and the admin wants to see the result right away. It issues *one*
`mailer.send(Mail...)` call per chunk of 50 (built in `AdminEventsResource.sendMessage`), which the
mailer processes as a batch. The chunk must therefore not be flattened -- otherwise every mail goes
out individually and blocking. The Qute `Fmt` of the bulk mail is deliberately **not** cached: Qute's
template cache is unbounded, and bulk mail texts are free-form admin input.

Tests: because the mails go out asynchronously, they have not necessarily arrived by the time the HTTP
response is back. `FunctionalTestBase.awaitTotalMails(n)` / `awaitMailsTo(mail, n)` poll for them with
Awaitility (`untilAsserted`) instead of asserting directly -- that way a timeout reports the number of
mails actually counted (`expected: 2 but was: 1`) and not merely that the wait expired.
The `MockMailbox` lives in `FunctionalTestBase`; both test classes use it.

### Tenant-owned CSS

`Tenant.css` (column `tenant.css`, migration `V8`) holds an optional stylesheet that the JUGs maintain
in the admin area under *JUG Data*, to match the iframe-embedded pages to their own website.

- **Only the participant pages get it, never the admin area.** A broken stylesheet must not break the
  very form used to repair it. Implemented as **opt-in**: `template.html` has an `{#insert styles}{/}`
  slot in `<head>` (behind Bootstrap, so the tenant's own rules win), and the eight participant
  templates fill it with `{#styles}{#include tenantstyle.html/}{/styles}`. Opt-out would be shorter but
  would silently colour any admin page added later.
- It is supplied by **`TenantStyle`**, a `@Named("tenantStyle") @RequestScoped` bean like `CurrentUser`,
  reachable as `inject:tenantStyle.present` / `inject:tenantStyle.css`. Not template data, because
  `tenant` means two different things across the participant templates -- the bare id string in
  `registration.html`/`thanks.html`, the `Tenant` entity in the webinar pages -- and `delete.html` gets
  no tenant at all. The bean loads the entity lazily during rendering; that works, the request context
  and the Hibernate session are still around at that point.
- `getCss()` deliberately returns a `RawString`: the CSS lands unescaped inside a `<style>` element.
  The only sequence that can break out of it is a literal `</style` (whitespace after the slash does
  *not* close the element). `AdminTenantResource.post` rejects it on save
  (`TenantStyle.closesTheStyleElement`), and `TenantStyle.load()` strips it again at render time, so a
  row that reached the database some other way cannot inject markup either.
- `admin/data.html` now renders its values from a `TenantForm` instead of straight off the entity --
  that is what lets a rejected POST hand the entered data back. `AdminTenantResource.post` stays
  `@Transactional` as a whole; a separate `save()` method would be self-invocation and the interceptor
  would not fire.
- `TenantProvisioningService` copies the column along -- its column list is maintained by hand.

### iframe Auto-Height

`template.html` ends with a height reporter: if the page is embedded
(`window.parent !== window`), a `ResizeObserver` reports the content height to the embedding
page via `postMessage` as `{type: 'ijug-registration:height', height: <px>}`. The message type
is a **public contract** with the JUG websites (snippet in `docs/handbuch.adoc`) -- renaming it
breaks their integration silently. A test in `RegistrationAndDeletionFunctionalTest` pins it
down.

`document.body` is measured deliberately, not `documentElement`: the document height grows to at
least the viewport height, which would keep the iframe from ever shrinking again after it grew
(feedback loop).

### CORS
`quarkus.http.cors.methods` in `application.properties` has to list every method the frontend uses -- currently `GET,POST,PUT,DELETE,OPTIONS`. The CORS filter runs **before** authentication and routing: a missing method is rejected with a **403 without a body**, which looks like a permission problem but is not one. Until 2026-08-19 `PUT` and `DELETE` were missing here, which made every writing admin function (saving event data, bulk mail, deleting a registration) fail silently in the browser.

### Authentication (OIDC / Keycloak)
- Production Keycloak: `https://id.ijug.eu/realms/ijug`
- Client ID: `registration`
- Roles are sourced from the access token at `resource_access/registration/roles`
- A role named exactly like the tenant ID grants admin access to that tenant
- `admin/menu.html` binds `{#let nav=activeNav.or('')}` around the nav list. Qute renders strictly: a page
  that simply omits `activeNav` from its template data would fail with a 500, not a blank highlight. The
  binding lets the operator pages, which highlight no nav entry, leave it out.
- **Every link in `admin/menu.html` is absolute (`/admin/{tenant.id}/...`), not relative.** The menu is
  included by pages at two path depths: `/admin/{tenant}/events` and `/admin/{tenant}/events/{eventId}`
  (`admin/list.html`). On the detail page a relative `./events` resolved against `/admin/{tenant}/events/`
  and ended up at `/admin/{tenant}/events/events` -- a 404 instead of navigation. The remaining admin
  templates were absolute already.
  `AdminFunctionalTest.testMenuLinksAreAbsoluteOnTheEventDetailPage` pins this down.
- The two operator-only pages (*Neue JUG*, *Server-Logs*) live in the user dropdown at the bottom of
  `admin/menu.html`, wrapped in `{#if inject:currentUser.admin}`; the nav list above holds only the pages every
  orga team uses.
  `CurrentUser` is a `@Named @RequestScoped` bean; Qute resolves `inject:` namespace expressions against
  `@Named` beans and validates them at build time, which keeps the flag out of every single resource method.
  The endpoints stay guarded by `@RolesAllowed` — the hidden link is a courtesy, not a permission check.
- Logout runs through `AdminLogoutResource` (`/admin/{tenant}/logout`), **not** through
  `quarkus.oidc.logout.path`: the built-in logout offers a single static `post-logout-path`, while the
  landing page has to carry the tenant. The resource assembles the RP-initiated logout request itself
  (`id_token_hint` + `post_logout_redirect_uri`) and clears the local session via `OidcSession.logout()`.
  The post-logout URI must be a valid post-logout redirect URI on the Keycloak client — the dev realm
  allows `*`, the production client at id.ijug.eu has to permit `https://registration.ijug.eu/admin/*`.
- Dev mode uses a local Keycloak DevService with `ijug-realm.json`

### Production Deployment
Deployed via Docker Compose (`docker-compose.yml`). Image: `ghcr.io/ijug-ev/registration:latest`. Built and pushed by GitHub Actions on pushes to `main` (tagged `latest`) or `v*` tags (tagged with version).
