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

### Data Model
- `Registration` — one row per participant per event; `ttl` auto-expires ~1 week post-event.
- `Tenant` — configuration per JUG: name, website, privacy/imprint URLs, logo, reply-to address, events JSON URL.
- `EventData` — mutable per-event key/value metadata, editable by admins.
- `Content` — tenant-scoped UI help texts (name field hint, email hint, video recording notice, etc.).

### Templates
Qute HTML templates in `src/main/resources/templates/`. Mail templates are in `templates/mail/`. All templates share `template.html` as the base layout via Qute includes.

**Seiten-Scripts gehoeren in den `{#scripts}`-Block, nicht in `{#body}`.** `template.html` laedt jQuery und Bootstrap am Ende von `<body>` und stellt danach einen `{#insert scripts}{/}`-Slot bereit. Ein `<script>` innerhalb von `{#body}` wird vor den Lib-Tags gerendert und laeuft damit, bevor `$` existiert -- ohne sichtbaren Fehler, die Seite bleibt einfach tot.

`async`/`defer` sind auf den Lib-Tags bewusst nicht gesetzt: `defer` wirkt nicht auf Inline-Scripts, ein deferter jQuery-Tag wuerde die Slot-Scripts also weiterhin ueberholen. Wer `defer` will, muss alle Slot-Scripts in `DOMContentLoaded` kapseln.

### Database Migrations
Flyway migrations in `src/main/resources/db/migration/`. Run automatically at startup. Schema is managed solely via Flyway (`quarkus.hibernate-orm.schema-management.strategy=none`).

### Teilnehmer-Mails per CDI-Event (asynchron)

Kein Teilnehmer-Mailversand haengt mehr in der JTA-Transaktion. Die Services machen nur noch DB-Arbeit
und feuern am Ende ein CDI-Event aus `de.jugda.registration.event`:

- `RegistrationService.handleRegistration()` -> `RegistrationConfirmed`
- `DeleteService.processWaitlist()` -> `WaitlistPromoted`

Beide sind Records `(tenantId, baseUrl, RegistrationDto)` und implementieren das sealed Interface
`RegistrationEvent`. In `EmailService` haengen daran genau zwei Observer -- **nicht pro Event-Typ,
sondern einmal auf dem Supertyp**, weil CDI Observer ueber die Laufzeitklasse und deren Supertypen
aufloest:

1. `relayAfterCommit(@Observes(during = AFTER_SUCCESS) RegistrationEvent)` -- laeuft erst nach dem
   Commit, damit keine Mail fuer eine zurueckgerollte Transaktion rausgeht, und feuert dasselbe Event
   via `fireAsync()` erneut. `fireAsync` erreicht nur `@ObservesAsync`-Methoden, also keine Schleife.
2. `onRegistrationEvent(@ObservesAsync RegistrationEvent)` -- rendert und versendet die Mail auf einem
   Worker-Thread. Welches Template und welcher Betreff, entscheidet ein `switch` ueber das sealed
   Interface: ein neuer Event-Typ bricht damit die Kompilierung, statt still keine Mail zu schicken.

**Wichtig fuer den Async-Observer:** Dort ist kein HTTP-Request aktiv. `UriInfo` ist deshalb nicht
benutzbar -- die Base-URL steckt im Event-Payload. Der Request-*Kontext* dagegen ist da: ArC aktiviert
ihn um jede Observer-Notification herum (`EventImpl.Notifier.notify`, solange
`quarkus.arc.strict-compatibility` aus ist -- Default). Selbst aktivieren muss man ihn also nicht,
gesetzt werden muss nur `TenantContext.tenantId` aus dem Payload, sonst laufen `CurrentTenantResolver`,
`EventService` und `Tenant.findById()` ins Leere. (Frueher stand hier ein manuelles
`Arc.container().requestContext().activate()`; eine Probe hat gezeigt, dass der Kontext bereits aktiv
ist und der Block nie lief.) `@ActivateRequestContext` waere ohnehin wirkungslos, weil Interceptoren
auf Observer-Methoden nicht greifen.

Exceptions asynchroner Observer landen in einem `CompletionStage`, den niemand auswertet -- deshalb
loggt `onRegistrationEvent()` selbst. Folge: Ein fehlgeschlagener Mailversand rollt die Transaktion
nicht mehr zurueck, der Nutzer sieht trotzdem die Danke-Seite.

Die Rundmail (`EmailService.sendBulkEmail`, Admin-UI) laeuft weiterhin synchron im Request -- sie hat
kein Transaktions-Problem und der Admin will das Ergebnis sofort sehen. Sie verschickt pro 50er-Chunk
(gebildet in `AdminEventsResource.sendMessage`) *einen* `mailer.send(Mail...)`-Aufruf, den der Mailer
gebuendelt abarbeitet. Der Chunk darf also nicht flachgeklopft werden -- sonst geht jede Mail einzeln
und blockierend raus. Das Qute-`Fmt` der Rundmail wird bewusst **nicht** gecacht: Qutes Template-Cache
ist unbegrenzt, und die Rundmail-Texte sind freier Admin-Text.

Tests: Weil die Mails asynchron rausgehen, sind sie beim Eintreffen der HTTP-Response noch nicht
zwingend da. `FunctionalTestBase.awaitTotalMails(n)` / `awaitMailsTo(mail, n)` pollen per Awaitility
(`untilAsserted`) darauf, statt direkt zu assertieren -- ein Timeout meldet dadurch die tatsaechlich
gezaehlten Mails (`expected: 2 but was: 1`) und nicht nur, dass die Wartezeit abgelaufen ist.
Die `MockMailbox` haengt in `FunctionalTestBase`, beide Test-Klassen nutzen sie.

### CORS
`quarkus.http.cors.methods` in `application.properties` muss alle Methoden enthalten, die das Frontend nutzt -- aktuell `GET,POST,PUT,DELETE,OPTIONS`. Der CORS-Filter laeuft **vor** Authentifizierung und Routing: eine fehlende Methode wird mit einem **403 ohne Body** abgewiesen, was wie ein Rechteproblem aussieht, aber keines ist. Bis 2026-08-19 fehlten hier `PUT` und `DELETE`, wodurch saemtliche schreibenden Admin-Funktionen (Event-Daten speichern, Rundmail, Anmeldung loeschen) im Browser stumm fehlschlugen.

### Authentication (OIDC / Keycloak)
- Production Keycloak: `https://id.ijug.eu/realms/ijug`
- Client ID: `registration`
- Roles are sourced from the access token at `resource_access/registration/roles`
- A role named exactly like the tenant ID grants admin access to that tenant
- Dev mode uses a local Keycloak DevService with `ijug-realm.json`

### Production Deployment
Deployed via Docker Compose (`docker-compose.yml`). Image: `ghcr.io/ijug-ev/registration:latest`. Built and pushed by GitHub Actions on pushes to `main` (tagged `latest`) or `v*` tags (tagged with version).
