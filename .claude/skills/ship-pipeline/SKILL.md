---
name: ship-pipeline
description: The synchronized quality gate for this repository — run it before any commit, Docker build, or Vercel deploy. Defines gate order, pass criteria, and which specialist agent owns each failure. Trigger on "ship it", "is this ready", "run the pipeline", or before deploying.
---

# Ship pipeline

Gates run **in order**. A failed gate stops the pipeline; it is not deferred. Each gate names
the agent that owns its failures.

## Gate 0 — Static integrity

```bash
npm run typecheck && npm run lint
```

Pass: zero TypeScript errors, zero ESLint errors. Warnings are read and either fixed or
justified in writing. Owner: `architecture-refactorer`.

## Gate 1 — Correctness

```bash
npm run test -- --run
```

Pass: all unit and integration tests green. Domain rules — window state machine, duplicate-scan
suppression, absentee sweep, date-range reporting — must each have a test that fails when the
rule is broken. Owner: `qa-docker-engineer`.

## Gate 1b — Lockfile portability

```bash
npm run check:lockfile
```

Pass: the lockfile names the Linux native bindings. npm records only the platform it resolved on,
so a lockfile written on Windows makes `npm ci` in the container build a tree that cannot compile
CSS. Regenerate with `npm run lockfile:linux`. Owner: `qa-docker-engineer`.

## Gate 2 — Security

```bash
npm audit --omit=dev
git grep -nIE "(api[_-]?key|secret|password|token)\s*[:=]\s*['\"][^'\"]{8,}"
```

Pass: no committed secrets, no unvalidated external input reaching storage, security headers
present in `next.config.ts`, no exploitable advisory. Owner: `security-auditor`.

## Gate 3 — Build and performance budget

```bash
npm run build
```

Pass: build succeeds, and the measured budget holds. Budgets are in **compressed transfer bytes**
(`PerformanceResourceTiming.encodedBodySize`), not the uncompressed sizes a bundler prints —
those run about three times larger and would flatter every result:

| Budget | Limit |
|---|---|
| Entry JS on first load | 345 kB |
| Per-route incremental JS | 25 kB |
| Font payload | 60 kB |
| Blocking third-party requests | 0 |

Camera, QR, spreadsheet, and animation-engine code must each be confirmed absent from the entry
bundle. Rationale for these specific numbers, and what the stack costs before application code:
`docs/performance.md`. Owner: `performance-optimizer`.

## Gate 4 — Runtime and responsive

Start the app, then at 375 / 768 / 1280 px: visit every route, confirm zero console errors,
confirm no horizontal body scroll, exercise one real interaction per route. Owner:
`ui-ux-engineer`.

## Gate 5 — Container

```bash
docker build -f devops/Dockerfile -t attendance-web .
docker run --rm -p 3000:3000 attendance-web
```

Pass: image builds, container runs as non-root, health endpoint returns 200, one real page
renders. Report image size. Owner: `qa-docker-engineer`.

## Gate 6 — Deploy

Preview deploy → verify live URL in a browser → promote to production. Requires explicit user
confirmation; production deployment is outward-facing. Owner: `deploy-engineer`.

## Synchronization rules

- Gates 0–3 may be prepared in parallel but must be **reported** in order. A green Gate 3 on top
  of a red Gate 1 is meaningless.
- Any fix applied at gate *n* re-runs gates `0..n`. Fixes invalidate earlier evidence.
- Evidence is real command output. "Should pass" fails the gate.
- If a gate cannot run, it is a failure, not a skip. Say which gate and why.
