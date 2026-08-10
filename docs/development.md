# Development

## Layout

An npm workspace repo. Install once from the root; it covers both packages.

```
web/       Next.js application (attendance-monitoring-web)
backend/   Sync server: Postgres schema, auth, replication (@attendance/sync)
devops/    Dockerfile, compose files
docs/      Architecture, audits, this file
```

`web` depends on `@attendance/sync` as a workspace package and consumes its TypeScript source
directly, which is why `next.config.ts` lists it under `transpilePackages`.

## Commands

Run from the repository root.

| Command | What it does |
|---|---|
| `npm install` | install every workspace |
| `npm run dev` | development server |
| `npm run build` | production build |
| `npm run typecheck` | TypeScript across both packages |
| `npm run lint` | ESLint |
| `npm test` | unit and integration tests, both packages |
| `npm run test:e2e` | Playwright, desktop and mobile |
| `npm run db:up` / `db:down` | local Postgres in Docker |
| `npm run db:migrate` | apply SQL migrations |

## Sync locally

```bash
npm run db:up
npm run db:migrate
DATABASE_URL="postgres://attendance:attendance-local-only@127.0.0.1:55432/attendance" npm run dev
```

Without `DATABASE_URL` everything still runs; the Sync screen reports that sync is not enabled.
The backend integration tests need the database up — they connect to the same URL by default.

## Regenerating the lockfile

**The lockfile must be generated on Linux, from the manifests alone.**

npm records only the platforms present in the tree it resolves. A lockfile written on Windows
names no `@tailwindcss/oxide-linux-*`, `lightningcss-linux-*`, or `@next/swc-linux-*` package, so
`npm ci` in the Docker image produces a tree that cannot compile CSS and the build fails with
`Cannot find native binding`. Running `npm dedupe` on Windows strips those entries out again,
which is exactly how this broke.

Regenerate with a clean tree inside Linux:

```bash
npm run lockfile:linux
```

Copying the manifests into an empty directory is the part that matters: resolving inside the
mounted repo would reuse the existing `node_modules` and reproduce the same platform-specific
result.

Verify before committing:

```bash
npm run check:lockfile
```

That check is part of the ship pipeline, because this has broken the Docker build twice — once
from `npm dedupe`, once from a routine `npm install` on Windows. A paragraph in a document was
not enough; the guard fails the build with the exact command needed to fix it.

Installing on Windows afterwards is fine and normally leaves the Linux entries in place. `npm
dedupe` is the one command that reliably destroys them — avoid it in this repo.

## Docker

```bash
docker build -f devops/Dockerfile -t attendance-web .
docker run --rm -p 3000:3000 attendance-web
```

With sync:

```bash
docker run --rm -p 3000:3000 -e DATABASE_URL="postgres://..." attendance-web
```

The image is a three-stage build on `node:22-alpine`, runs as a non-root user, and serves the
standalone output. In a workspace build that output keeps the repository's directory shape, so
the entry point is `web/server.js` rather than `server.js`.

## Before committing

`.claude/skills/ship-pipeline` defines the ordered gates and which specialist agent owns each
failure. The short version: typecheck, lint, tests, security scan, build with the performance
budget, runtime and responsive check in a browser, container, then deploy.
