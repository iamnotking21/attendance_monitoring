# Sync

The app works with no server. Sync is an optional layer that keeps several devices holding the
same data — a phone at the gate, a tablet in the office, a laptop for reports.

## Shape

```
   Device A                    Server                     Device B
  ┌────────────┐          ┌──────────────┐          ┌────────────┐
  │ IndexedDB  │ ──push──▶│  Postgres    │◀──push── │ IndexedDB  │
  │ (truth)    │ ◀──pull──│  (relay)     │ ──pull──▶│ (truth)    │
  └────────────┘          └──────────────┘          └────────────┘
        ▲                                                  ▲
    every screen reads here                     every screen reads here
```

The device's own database is the source of truth for everything it renders. The server is a
relay: it holds a copy so other devices can catch up, and never sits between a screen and the
data it displays. That is what makes offline the normal case rather than a degraded one — with
no network there is simply nothing to reconcile.

## Enrolment

There are no user accounts, deliberately. Attendance is taken by whoever is holding the phone at
the gate; a login screen there buys nothing and guarantees a shared password on a sticky note.

- **Create a workspace** — mints a join code and a token for that device.
- **Join** — any device presenting the code receives *its own* token.

Tokens are per device, so a lost phone can be revoked alone. The server stores SHA-256 hashes of
both the code and the tokens; a database leak yields nothing usable. Comparison is constant-time.

SHA-256 rather than Argon2 or bcrypt: those exist to slow down guessing of low-entropy human
passwords. A token is 256 random bits and a join code is ~59 bits, so there is nothing to guess
offline, and a deliberately slow hash on every sync request would cost latency for no security.

Anyone with the join code has full access to the workspace. The UI says so where the code is
shown.

## The cursor

Pull is driven by a server-assigned sequence number, not a timestamp:

```sql
CREATE SEQUENCE sync_seq;
-- every replicated table:
server_seq bigint NOT NULL DEFAULT nextval('sync_seq')
```

One sequence shared by all five tables. Per-table sequences would make a single cursor
meaningless — "everything after 40" would point somewhere different in each table.

Timestamps cannot serve as a cursor because device clocks disagree, sometimes by minutes and
occasionally by years. A row written while a phone's clock was wrong would be permanently skipped
by every other device. A sequence assigned by the database is monotonic by construction.

Every upsert sets `server_seq = nextval('sync_seq')`, so an edit is as visible to the cursor as an
insert.

## Conflict resolution

Last-write-wins per row, keyed by the writer's `updatedAt`, with the row id as tiebreak:

```ts
export function incomingWins(incoming, existing) {
  if (incoming.updatedAt !== existing.updatedAt) return incoming.updatedAt > existing.updatedAt;
  return incoming.id > existing.id;
}
```

The tiebreak matters more than it looks. Timestamps are coarse and a bulk edit can produce the
same millisecond twice; without a deterministic decision two replicas could disagree forever.
The rule is total and antisymmetric, so both devices and the server reach the same answer without
consulting each other. The same function runs on both sides.

This is not a CRDT and does not merge field by field. If two teachers rename one section while
both offline, one name wins whole. For a roster edited by a handful of people that is the honest
trade: an operation log would double the storage and the complexity to resolve a conflict that is
rare and cheap to correct by hand.

**Attendance records never conflict.** They are append-only and deduplicated by
`(workspace, studentNumber, scheduleId, date)`, enforced by a unique index in Postgres and by the
matching index in IndexedDB. Two devices scanning the same badge offline each mint their own row
id, and the natural key recognises them as one event. The first writer wins; a scan is a fact
about a moment, not a value to be overwritten.

## What gets pushed

Everything whose `updatedAt` is later than a stored watermark — a query, rather than a dirty flag
or an outbox table that could drift out of step with the data it describes.

The cost is a little re-sending: a row pulled from another device can fall after the watermark
and be pushed straight back. The server recognises it as identical and applies nothing, so the
effect is wasted bytes rather than a wrong result.

The watermark advances only after every batch in a push has landed. If the connection drops
halfway, the whole set is offered again next time; the server deduplicates, so re-sending is
cheap and losing a change is not possible.

## Offline

Two separate things have to work offline, and only one of them was free:

- **The data** was always local. IndexedDB does not care about the network.
- **The application code** was not. Before the service worker in `web/public/sw.js`, a cold start
  with no connection showed nothing at all — which was found by killing the server and reloading,
  not by reading the code.

The worker caches the seven routes and the content-hashed assets at runtime, so after one online
visit the app opens and works with the server switched off. Verified by stopping the server,
reloading, recording a scan, and reading a four-week report — then restarting it and watching
that scan arrive on the server.

`/api/*` is never cached. A stale cursor would silently skip changes, and a cached health
response would hide an outage.

## Running it locally

```bash
npm run db:up        # Postgres in Docker
npm run db:migrate   # apply migrations
DATABASE_URL="postgres://attendance:attendance-local-only@127.0.0.1:55432/attendance" npm run dev
```

Without `DATABASE_URL` the app runs exactly as before and the Sync screen says sync is not enabled
on this deployment — an honest state rather than buttons that fail when pressed.

## Endpoints

| Route | Purpose | Auth | Rate limit |
|---|---|---|---|
| `POST /api/workspace` | create a workspace, enrol this device | none | 5 per hour per caller |
| `POST /api/workspace/join` | exchange a join code for a device token | none | 10 per 15 min per caller |
| `POST /api/sync/pull` | changes after a cursor | bearer token | change-set caps |
| `POST /api/sync/push` | submit local changes | bearer token | 2000 rows, 8 MB |
| `GET /api/sync/status` | is sync configured here | none | — |

The workspace is always taken from the token, never from the request body — taking it from the
body would let any authenticated device read another school's roster. There is a test for it.

Rate limiting is a fixed window held in Postgres, because serverless instances share no memory;
an in-process counter would reset on every cold start and bound nothing.

## Deploying with sync

Set `DATABASE_URL` in the hosting environment and run the migrations once. Any Postgres works;
Neon's free tier is enough for a school. Use the pooled connection string — the client is
configured with `max: 1` and `prepare: false`, which is what transaction-mode poolers require.

Without that variable the deployment is offline-only, which is a perfectly good state for a
portfolio demo and costs nothing to run.
