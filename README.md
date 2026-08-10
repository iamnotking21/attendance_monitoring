# Attendance Monitoring

QR-code attendance for schools. Scan a student ID and the record lands in the right schedule with
the right status — present, late, or absent — with no double counting.

**Live demo: https://web-mu-sooty-57.vercel.app** — it opens with a seeded roster of two
sections and four weeks of history, and one schedule whose attendance window is always open, so
the scanner does something real the moment you try it. No sign-up, nothing to configure.

Originally an Android app written in 2019 (Java, SQLite, ZXing). Rebuilt as a local-first web
application that keeps the original's defining property: the data never leaves the device that
recorded it.

## Repository layout

```
web/             Next.js 16 web application — the current product
mobile-android/  The original Android app, preserved as reference. Not built by CI.
devops/          Dockerfile and compose
docs/            Security audit, performance measurements
.claude/         Specialist agent definitions, domain reference, and the ship pipeline
```

## The domain

Five entities: `Section`, `Student`, `Schedule`, `AttendanceRecord`, `SchoolDay`.

Each schedule owns two consecutive daily windows — a **present** window and a **late** window:

```
       present.start        present.end / late.start          late.end
             │                        │                          │
  before ────▶──────── present ───────▶───────── late ───────────▶──── closed
                                                                        │
                                                          absentee sweep runs
```

A scan is graded by whichever window is open when it arrives. Scans outside both windows record
nothing at all. When the late window closes, every student in the section without a record for
that schedule and date is swept in as absent.

Three invariants carry the whole design:

- **One record per (student, schedule, date).** Enforced by a unique compound index in IndexedDB,
  not by application code — otherwise a double-tapped badge races the absentee sweep.
- **Records are append-only.** Correcting a mistake writes a new record; history is never edited.
- **Deletes are soft.** Removing a section, student, or schedule takes it out of the UI and
  leaves past reports accurate.

Full rules: [`.claude/skills/attendance-domain/SKILL.md`](.claude/skills/attendance-domain/SKILL.md)

## Architecture

```
app/         Next.js routes — composition and wiring only
components/  Presentational and interactive UI
features/    Feature modules: hooks plus feature-scoped components
domain/      Pure TypeScript. Entities, Zod schemas, business rules.
             Imports nothing from React, Next.js, or any storage library.
lib/         Infrastructure: Dexie client, repositories, services, exporters
```

Dependencies point inward only. Because `domain/` is pure, the attendance rules are unit-tested
in isolation — the window state machine, duplicate suppression, and the absentee sweep each have
tests that fail if the rule changes.

## Running it

```bash
cd web && npm ci && npm run dev
```

| Command | What it does |
|---|---|
| `npm run dev` | development server |
| `npm run build` | production build |
| `npm run typecheck` | TypeScript, no emit |
| `npm run lint` | ESLint |
| `npm run test:run` | 139 unit and integration tests |
| `npm run test:e2e` | 16 Playwright journeys, desktop and mobile |

### Docker

```bash
docker build -f devops/Dockerfile -t attendance-web .
docker run --rm -p 3000:3000 attendance-web
```

Multi-stage build on `node:22-alpine`, running as a non-root user, with a healthcheck that
probes the app rather than the port. `devops/compose.yaml` adds a read-only root filesystem and
drops all capabilities.

## Quality

| | |
|---|---|
| Unit and integration | 139 tests, Vitest with fake-indexeddb |
| End to end | 16 Playwright journeys across Chromium desktop and a Pixel 5 profile |
| Dependency advisories | 0, `npm audit` including dev dependencies |
| Security headers | CSP with `default-src 'none'`, HSTS, `frame-ancestors 'none'`, camera-only permissions policy |
| First load | 299 kB JS compressed, then 6–8 kB per subsequent route |

Details: [`docs/security-audit.md`](docs/security-audit.md) and
[`docs/performance.md`](docs/performance.md).

Before any commit or deploy, [`.claude/skills/ship-pipeline`](.claude/skills/ship-pipeline/SKILL.md)
defines seven ordered gates and which specialist agent owns each failure.

## Why local-first

The original app kept everything in SQLite on the phone. The web rebuild keeps that shape rather
than adding a server, and the reason is the data: a roster of minors' names and their daily
movements. With no backend there is no database to breach, no session to steal, no third party to
trust, and no hosting bill — the app works offline and costs nothing to run. The trade is real
and stated plainly in the UI: data lives in one browser, so the Data screen makes backup, restore,
and erasure first-class operations.

## Legacy app

`mobile-android/` is the original project — Gradle 5.4.1, Java, SQLite, ZXing. It is kept as
reference for how the rules were originally expressed and is not part of the build.

## License

MIT
