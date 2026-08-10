# Security audit — web application

Date: 2026-08-10 · Scope: `web/` · Method: static review, dependency audit, and live header
inspection against the production container.

## Threat model

The application has no server-side state, no accounts, no cookies, and no outbound requests. It
holds one thing worth protecting: a roster of schoolchildren's names and attendance history,
stored in the operator's own browser. That shapes the whole assessment — there is no database to
breach and no session to steal, so the realistic attacks are **hostile input reaching storage**
(a printed QR code, an edited backup file), **injection into the DOM**, and **data escaping into
an export that something else executes**.

## Findings

| # | Severity | Finding | Status |
|---|---|---|---|
| 1 | MEDIUM | `script-src` requires `'unsafe-inline'` | Accepted, mitigated — see below |
| 2 | LOW | Personal data is readable by any script on the origin | Accepted, inherent to the design |
| 3 | INFO | The camera is requested only on explicit user action | By design |

Nothing rated HIGH or CRITICAL was found.

### 1. `script-src 'unsafe-inline'` — MEDIUM, accepted

`next.config.ts:16` ships `script-src 'self' 'unsafe-inline'` in production. Next.js emits inline
bootstrap and streaming scripts; removing the allowance requires a per-request nonce from a
proxy, which forces every page out of static rendering.

The allowance is only dangerous with an XSS foothold, so the mitigation is to close that off
instead:

- Every stored value is rendered as text through JSX. There is no `dangerouslySetInnerHTML`,
  `innerHTML`, `eval`, `new Function`, or `document.write` anywhere in `web/src` (verified).
- Student numbers — the one field an outsider controls, by printing a QR code — are constrained
  to `^[A-Za-z0-9][A-Za-z0-9._-]*$` at `src/domain/primitives.ts:44` before any lookup runs. A
  payload of `<script>alert(1)</script>` is rejected as malformed and never reaches storage;
  this is covered by both a unit test and an end-to-end test.
- Free-text fields strip Unicode control and format characters, then enforce a length cap.
- `default-src 'none'` means anything not explicitly allowed is refused, and `connect-src 'self'`
  means an injected script has nowhere to send what it steals.
- Development additionally allows `'unsafe-eval'`, which React's development build needs. It is
  scoped by `NODE_ENV` and verified absent from the production container's response.

Residual risk: an attacker who achieves script execution through a compromised dependency could
read local attendance data. `connect-src 'self'` denies them an exfiltration channel over
fetch, XHR, or WebSocket.

### 2. Personal data readable on the origin — LOW, inherent

Names, student numbers, and attendance history sit in IndexedDB unencrypted. Encrypting them
would mean holding a key somewhere the same scripts can read, which moves the problem without
solving it. The controls that do apply:

- QR codes encode the student number alone — no name, no section, no date of birth
  (`src/features/students/QrCodeDialog.tsx`). A badge found on the floor tells a stranger nothing.
- `Permissions-Policy` denies microphone, geolocation, payment, and USB outright, and limits the
  camera to this origin.
- `frame-ancestors 'none'` plus `X-Frame-Options: DENY` prevent clickjacking.
- `Cross-Origin-Opener-Policy` and `Cross-Origin-Resource-Policy` are set to `same-origin`.
- The Data screen lets the operator erase everything, and the browser's own "clear site data"
  works as expected.

### 3. Camera access — INFO

`getUserMedia` is called only from the "Start camera" button, never on page load. The decoder and
its worker are dynamically imported at the same moment, so a visitor who never opens the scanner
never downloads them. Denied permission produces a specific, actionable message and the typed
fallback stays available.

## Verified clean

| Check | Command | Result |
|---|---|---|
| Committed secrets | `git grep -nIE "(api[_-]?key\|secret\|password\|token)..."` | no matches |
| DOM injection sinks | `git grep -nE "dangerouslySetInnerHTML\|innerHTML\s*=\|eval\("` | no matches |
| Client-exposed env vars | `git grep -n NEXT_PUBLIC` | none defined |
| Dependency advisories | `npm audit --omit=dev` | **0 vulnerabilities** |
| Dependency advisories, dev included | `npm audit` | **0 vulnerabilities** |
| Ignored secret files | `.gitignore`, `.dockerignore` | `.env*`, `*.pem`, `*.key`, `*.jks`, `local.properties` |

## Input validation

Every boundary parses before it trusts. Zod schemas live in `src/domain/`, and repositories call
them rather than accepting typed objects on faith:

| Input | Schema | Rejects |
|---|---|---|
| QR payload / typed student number | `studentNumberSchema` | markup, URLs, formulas, SQL, over-length, empty |
| Route parameter `[sectionId]` | `Number.isInteger` guard, then `notFound()` | any non-numeric path segment |
| Section, student, schedule forms | `sectionInputSchema`, `studentInputSchema`, `scheduleInputSchema` | blank names, invalid times, overlapping windows |
| Imported backup file | `backupSchema` | wrong format, future version, malformed rows, 64 MB size cap, per-array element caps |
| Dates and times | `isoDateSchema`, `time24Schema` | well-formatted dates that do not exist, such as `2023-02-29` |

## CSV and spreadsheet injection

A student named `=cmd|'/c calc'!A1` would become executable content the moment a coordinator
opened the export. `src/domain/spreadsheet.ts` prefixes any cell beginning with `=`, `+`, `-`,
`@`, tab, or carriage return with an apostrophe, and applies it to both the CSV and the `.xlsx`
writer. Covered by tests in `tests/domain/spreadsheet.test.ts`.

## CORS

There is no CORS configuration, and that is the correct state: the application makes no
cross-origin requests and exposes no API for others to call. `connect-src 'self'` enforces it
from the browser side. The single route handler, `/api/health`, returns `{"status":"ok"}` and
deliberately reveals nothing about versions or dependencies.

## Live header verification

Captured from the production container (`curl -sI`):

```
Content-Security-Policy: default-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self'
  'unsafe-inline'; font-src 'self'; img-src 'self' data: blob:; worker-src 'self' blob:;
  connect-src 'self'; manifest-src 'self'; media-src 'self' blob:; form-action 'self';
  base-uri 'none'; object-src 'none'; frame-ancestors 'none'; upgrade-insecure-requests
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
X-Frame-Options: DENY
Cross-Origin-Opener-Policy: same-origin
Cross-Origin-Resource-Policy: same-origin
Strict-Transport-Security: max-age=63072000; includeSubDomains; preload
Permissions-Policy: camera=(self), microphone=(), geolocation=(), payment=(), usb=(),
  interest-cohort=()
```

`X-Powered-By` is suppressed. Note the absence of `'unsafe-eval'` — that allowance exists only
in development.

## Container

- Runs as `uid=1001(nextjs)`, verified with `docker exec attendance-test id`.
- `.dockerignore` excludes `.env*`, keys, keystores, `local.properties`, and `.git`.
- `compose.yaml` adds `read_only: true`, `cap_drop: ALL`, and `no-new-privileges`.
- `npm ci --ignore-scripts` in the dependency stage, so no package lifecycle script runs during
  the build.

## Residual risk

The realistic remaining exposure is a supply-chain compromise in a runtime dependency, which
`'unsafe-inline'` would not stop. Its blast radius is bounded by `connect-src 'self'` — the data
can be read but has no route off the machine over the network — and by the fact that no
credential, token, or server-side resource exists to steal. The proportionate mitigation is
keeping dependencies current and re-running `npm audit` as a release gate, which
`.claude/skills/ship-pipeline` requires at Gate 2.
