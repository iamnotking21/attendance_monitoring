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
web/             Next.js 16 application
backend/         Sync server: Postgres schema, workspace auth, replication
mobile-android/  Android app — Kotlin, Jetpack Compose, Room
legacy-android/  The original 2019 Java app. Reference only; it cannot be built today.
devops/          Dockerfile and compose files
docs/            Sync architecture, security audit, performance, development notes
.claude/         Specialist agent definitions, domain reference, and the ship pipeline
```

Three clients, one set of rules. The attendance domain — the present/late window state machine,
duplicate suppression, the absentee sweep — exists twice, in TypeScript under `web/src/domain`
and in Kotlin under `mobile-android/app/src/main/java/ph/attendance/domain`, with both test
suites asserting the same boundaries. They meet at the sync protocol in `backend/`.

An npm workspace repo: install once from the root and it covers both packages.

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

## Offline and multi-device

The app works with no server at all — that is the normal case, not a fallback. Every screen reads
and writes the device's own IndexedDB, and a service worker caches the application itself, so
after one online visit it opens and works with the network switched off. Verified by killing the
server, reloading, recording a scan, and reading a four-week report.

Connecting a workspace adds replication on top. One device creates a workspace and gets a join
code; others enrol with it and receive their own tokens. Changes are pushed and pulled against a
Postgres relay; the device's copy stays the source of truth throughout.

Conflicts resolve by last-write-wins per row with a deterministic tiebreak, so every replica
reaches the same answer without coordinating. Attendance records never conflict at all: they are
append-only and deduplicated by (student, schedule, date), enforced by a unique index on both
sides. Two phones scanning the same badge offline produce one record.

Sync is optional. With no `DATABASE_URL` configured the deployment is offline-only and says so
plainly, rather than showing buttons that fail when pressed.

Full design, including why the pull cursor is a database sequence rather than a timestamp:
[`docs/sync.md`](docs/sync.md).

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
npm install && npm run dev
```

| Command | What it does |
|---|---|
| `npm run dev` | development server |
| `npm run build` | production build |
| `npm run typecheck` | TypeScript, no emit |
| `npm run lint` | ESLint |
| `npm test` | 153 web tests plus 21 backend tests |
| `npm run db:up` | local Postgres, for developing sync |
| `npm run test:e2e` | 16 Playwright journeys, desktop and mobile |

### Docker

The whole stack — Postgres, migrations, and the app — in one command:

```bash
npm run docker:up
```

Then open <http://localhost:3000>. Compose waits for Postgres to report healthy, runs the
migration job to completion, and only then starts the app.

App only, no database, no sync:

```bash
docker build -f devops/Dockerfile -t attendance-web .
docker run --rm -p 3000:3000 attendance-web
```

Multi-stage build on `node:22-alpine` (331 MB), running as a non-root user, with a healthcheck
that probes the app rather than the port. Compose adds a read-only root filesystem, drops all
capabilities, and sets `no-new-privileges`.

Step by step, including troubleshooting: [`docs/docker.md`](docs/docker.md).

## Quality

| | |
|---|---|
| Unit and integration | 153 web tests (Vitest, fake-indexeddb) plus 21 backend tests against real Postgres |
| End to end | 16 Playwright journeys across Chromium desktop and a Pixel 5 profile |
| Dependency advisories | 0 across both workspaces, dev dependencies included |
| Security headers | CSP with `default-src 'none'`, HSTS, `frame-ancestors 'none'`, camera-only permissions policy |
| First load | 299 kB JS compressed, then 6–8 kB per subsequent route |

Details: [`docs/security-audit.md`](docs/security-audit.md), [`docs/performance.md`](docs/performance.md),
[`docs/sync.md`](docs/sync.md), [`docs/mobile-android.md`](docs/mobile-android.md),
[`docs/docker.md`](docs/docker.md), and [`docs/development.md`](docs/development.md).

Before any commit or deploy, [`.claude/skills/ship-pipeline`](.claude/skills/ship-pipeline/SKILL.md)
defines seven ordered gates and which specialist agent owns each failure.

## Why local-first

The original app kept everything in SQLite on the phone. The web rebuild keeps that shape, and
the reason is the data: a roster of minors' names and their daily movements. With the device as
the source of truth, the app works in a corridor with no signal, costs nothing to run, and has no
session to steal.

Sync was added on top rather than underneath. It is optional, and turning it off returns the app
to exactly the local-only behaviour above — no screen depends on it. What it costs is stated
plainly in the UI: a workspace join code grants full access to that school's data, and anyone
holding it can enrol a device.

The trade that remains is storage: data lives in one browser unless a workspace is connected, so
the Data screen makes backup, restore, and erasure first-class operations.

## Android

`mobile-android/` is a Kotlin and Jetpack Compose app: Room for storage, CameraX with ML Kit for
scanning, and the same sync protocol the web client speaks. Its `domain/` package is the Kotlin
twin of `web/src/domain`, with 56 unit tests asserting the same rules.

```bash
cd mobile-android && ./gradlew assembleDebug testDebugUnitTest lintDebug
```

Details, including why the version matrix is deliberately not the newest:
[`docs/mobile-android.md`](docs/mobile-android.md).

## Legacy app

`legacy-android/` is the original 2019 project — Gradle 5.4.1, Java, SQLite, ZXing. Kept as
reference, not part of the build, and it cannot be built today: it resolves dependencies from
JCenter, which was sunset in 2021, and every alarm it schedules would crash on Android 12 or later
for want of a `PendingIntent` mutability flag. The full assessment, including two security
findings in the original code, is in [`docs/android-legacy.md`](docs/android-legacy.md).

## License

MIT
