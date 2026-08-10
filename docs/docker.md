# Running with Docker

Two ways to run it. Pick by whether you want multi-device sync.

## Option A — the whole stack, one command

Postgres, migrations, and the app. Use this if you want the Sync screen to work.

```bash
docker compose -f devops/compose.yaml up -d --build
```

Then open <http://localhost:3000>.

Compose sequences it for you: Postgres starts and must report healthy, the migration job runs to
completion, and only then does the web service start. Nothing to order by hand.

Check it came up:

```bash
docker compose -f devops/compose.yaml ps
```

Expected:

```
SERVICE    STATE     STATUS
postgres   running   Up (healthy)
web        running   Up (healthy)
```

`migrate` is absent from that list on purpose — it is a one-shot job that exits after applying
the schema. To see what it did:

```bash
docker compose -f devops/compose.yaml logs migrate
```

```
migrate-1  | ✓ 0001_init.sql
migrate-1  | Schema up to date (1 migration).
```

Stop it, keeping the data:

```bash
docker compose -f devops/compose.yaml down
```

Stop it and delete the database volume:

```bash
docker compose -f devops/compose.yaml down -v
```

## Option B — the app alone, no database

Attendance works entirely on the device, so the app is complete without a server. Skip Postgres:

```bash
docker build -f devops/Dockerfile -t attendance-web .
docker run --rm -p 3000:3000 attendance-web
```

The Sync screen then reports that sync is not enabled on this deployment — an honest state rather
than buttons that fail when pressed. Everything else behaves identically.

## Verifying it

```bash
curl http://localhost:3000/api/health          # {"status":"ok"}
curl http://localhost:3000/api/sync/status     # {"configured":true,"protocol":1}
```

`configured: false` means the container has no `DATABASE_URL`, which is expected for Option B.

Confirm it is not running as root:

```bash
docker compose -f devops/compose.yaml exec web id
# uid=1001(nextjs) gid=1001(nodejs) groups=1001(nodejs)
```

## What the image contains

A three-stage build on `node:22-alpine`, about 331 MB:

| Stage | Purpose |
|---|---|
| `deps` | `npm ci` from the root manifest and both workspace manifests |
| `builder` | `next build` with `BUILD_STANDALONE=1` |
| `migrator` | one-shot schema job; carries the backend source and database tooling |
| `runner` | the standalone server only — no source, no TypeScript, no build tools |

The runtime stage runs as an unprivileged user and has a healthcheck that probes `/api/health`
rather than the port, so a server that is listening but broken reports unhealthy.

Compose hardens the web service further: read-only root filesystem, all capabilities dropped,
and `no-new-privileges`.

## If the build fails with "Cannot find native binding"

The lockfile was generated on Windows or macOS. npm records only the platforms it resolved on, so
`npm ci` inside the Linux image installs no `@tailwindcss/oxide-linux-*`, `lightningcss-linux-*`,
or `@next/swc-linux-*` package and the CSS compile dies.

```bash
npm run check:lockfile   # tells you whether this is the problem
npm run lockfile:linux   # regenerates it correctly
```

Details in [`development.md`](development.md).

## Connecting a second device

With Option A running, open <http://localhost:3000/sync>, create a workspace, and copy the join
code. On another device on the same network, open `http://<your-machine-ip>:3000/sync` and enter
the code. Both devices then hold the same roster and reconcile automatically.

Two caveats for real use:

- The camera scanner needs a secure context. `localhost` counts; a plain-HTTP address on the LAN
  does not, so put TLS in front of the container before scanning from a phone.
- The join code grants full access to that workspace. Treat it like a key.
