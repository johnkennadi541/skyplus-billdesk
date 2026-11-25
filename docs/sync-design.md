# Cloud sync design (Supabase)

Status: design agreed. Not implemented. Schema SQL is written but has never been
executed against a real Postgres.

Decision: **(C) two-way sync** - multiple devices and outlets share one dataset.

## The shape of it

```
Compose UI  ->  UseCase  ->  Repository interface
                                   |            |
                              Room (local)   SupabaseRepository
                                   |            |
                            source of truth   push / pull
```

- **Room stays the source of truth.** A bill is written to Room first and the
  screen never waits for the network. The restaurant must keep billing with the
  wifi dead.
- **A pending queue** holds every row not yet acknowledged by the server.
- **Supabase** (Postgres + Auth, region `ap-south-1` Mumbai) is the shared copy.
- The UI never imports a Supabase type. One interface per area means the backend
  can be replaced by writing a new implementation.

## Why the ids are generated on the device

Every synced row carries a UUID v4 generated on the device, before it is sent.
If the network drops after the server saved a bill but before the app heard back,
the app retries with the same id and the server ignores the duplicate.

Without this, a flaky connection produces two bills for one table, and the
restaurant has to explain it to a customer. This is the single most important
decision in the whole sync design.

## Conflict rules

One rule per entity, in `sync/ConflictPolicy.kt`, unit tested:

| Entity | Rule | Why |
|---|---|---|
| Bill, bill item, stock movement | Append-only, idempotent | Money and stock are events, not editable rows |
| Bill void | Monotonic - first void wins, never un-void | Two tills must not both refund the same bill |
| Menu item, category, settings | Last write wins by `updatedAt` | The last person who edited it meant it |
| Inventory quantity | Server-derived from movements | Two tills each subtracting the same usage is the classic stock bug |
| Membership | Server-derived | Nobody grants themselves admin from a device |

Equal timestamps keep the existing row: a tie is not a reason to overwrite.

## What syncs

| Syncs | Does not sync |
|---|---|
| Bills and bill items | The cashier's PIN (stays on the device) |
| Menu and categories | Session tokens (never logged) |
| Inventory items and movements | Device settings that are device-specific |
| Outlet settings | |

## Security model

- Row level security on every table, scoped by outlet.
- `is_member(outlet_id)` for reads, `is_admin(outlet_id)` for writes.
- A bill can be inserted by any member and updated only by an admin, only once,
  only to void it - enforced in the database, not only in the app.
- The database refuses to delete a bill, and refuses to change an amount on an
  existing bill. Triggers do this, so a bug in the app cannot rewrite history.
- The app's own permission matrix is a convenience for the UI. The database rules
  are the real boundary.

## Offline behaviour

| Situation | What happens |
|---|---|
| No internet | Billing works. Rows queue in Room with `synced = false`. |
| Internet returns | Queue drains oldest first, in the background, with backoff. |
| Server rejects a row | It stays in the queue, flagged, and is shown to the admin. Never silently dropped. |
| Two devices edit the same menu item | Newest `updatedAt` wins; the loser is logged in `audit_log`. |
| Two devices void the same bill | First void wins; the second is rejected and reported. |
| Token expired while offline | The device keeps working locally; sync resumes after the next sign-in. |

## Honest cost

This is the largest piece of the project - realistically four to six weeks on top
of the billing app, not days:

- Room entities, DAOs and migrations for the full schema
- The pending queue, retry with backoff, and a visible sync status
- Supabase Auth wiring, deep-link OAuth, session refresh
- Row level security policies and their tests
- Conflict tests: two devices, offline, edited the same thing
- A restore path: what happens when a device is lost or replaced

## Sequencing

Choosing (C) does not change Steps 2 to 4 - you cannot sync tables that do not
exist yet. The order stays:

1. Step 2 - login, billing engine, CI
2. Step 3 - Cashier and Admin screens on Room
3. Step 4 - KOT, print, v0.1.0 release, offline-only
4. Step 5 - inventory screens
5. Step 6 - Supabase Auth (real token verification)
6. Step 7 - two-way sync with the queue and conflict rules

v0.1 ships without sync. That is a feature, not a shortcut: a working offline till
beats a broken connected one.
