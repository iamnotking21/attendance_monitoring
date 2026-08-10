# Security audit — web application

Date: 2026-08-10 · Scope: `web/` · Method: static review, dependency audit, and live header
inspection against the production container.

> **Updated 2026-08-10** after the sync server was added. The original assessment below covered
> an application with no server at all. The sync layer introduces a real network surface, audited
> in [Sync server](#sync-server-added-2026-08-10) at the end of this document.

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
| Route parameter `[sectionId]` | `idSchema` (UUID), then `notFound()` | any path segment that is not a UUID |
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

---

## Sync server (added 2026-08-10)

Optional, and off unless `DATABASE_URL` is set. When off, the endpoints answer 503 and the app is
exactly the local-only application audited above.

### Threat model, revised

The server holds the same data the device does — names of minors and their daily movements — for
every workspace that enrols. That makes three things matter that did not before: **tenant
isolation** (one school must never see another's roster), **credential handling** (there are now
credentials at all), and **abuse of unauthenticated endpoints**.

### Findings

| # | Severity | Finding | Status |
|---|---|---|---|
| 4 | MEDIUM | A join code grants full workspace access, and stays valid | Accepted, disclosed in the UI |
| 5 | LOW | Data is stored unencrypted at rest, beyond what the host provides | Accepted |
| 6 | INFO | No transport-level protection is enforced by the app | Handled by the platform |

Nothing rated HIGH or CRITICAL.

#### 4. Join codes are bearer secrets — MEDIUM, accepted

Anyone holding the code can enrol a device and read everything. There is no approval step and no
expiry. This is the deliberate consequence of having no accounts: attendance is taken by whoever
holds the phone at the gate, and a login there produces a shared password on a sticky note rather
than security.

Mitigations: the code is ~59 bits from a 30-character alphabet chosen to survive being read
aloud; joining is rate limited to 10 attempts per 15 minutes per caller, which makes an online
sweep hopeless; each device receives its own token, so a lost phone is revoked alone; and the
Sync screen states plainly that anyone with the code gets full access.

Residual risk: a leaked code is a full compromise of one workspace until it is rotated. Rotation
is not yet exposed in the UI — the next thing to add.

#### 5. Unencrypted at rest — LOW, accepted

Rows are stored in plain Postgres. Application-level encryption would require a key the server
must also hold to serve pulls, which relocates the problem rather than solving it. Managed
Postgres providers encrypt volumes at rest; that is the appropriate layer.

#### 6. Transport — INFO

The app sets `Strict-Transport-Security` and `upgrade-insecure-requests`, and Vercel serves
HTTPS only. Self-hosting behind plain HTTP would send tokens in the clear; the deployment notes
say to terminate TLS in front of the container.

### Verified

| Check | Evidence |
|---|---|
| Tenant isolation | Workspace id comes from the token, never the request body. Test: *"keeps one workspace's rows out of another's pull"*. |
| Credentials at rest | Only SHA-256 hashes stored; plaintext returned once and never again. |
| Timing attacks | `timingSafeEqual` on every secret comparison. |
| Auth on every sync route | `requireWorkspace` before any handler body; 401 verified live. |
| Input validation | Zod on every request; row counts capped at 2000 per push, body at 8 MB. Malformed input verified to return 400 with no stack trace. |
| Error disclosure | Handlers map known errors to stable codes and log the rest; no driver text or stack reaches a caller. |
| Rate limiting | Fixed window in Postgres, since serverless instances share no memory. Tested for both the limit and per-caller isolation. |
| Injection | Drizzle parameterises everything; the two raw SQL statements use bound parameters, and the one interpolated fragment is an integer interval derived from a constant, never from user input. |
| Idempotency | Re-pushing a batch after a failed response creates nothing twice — tested. |
| Rate-limit keys | Caller IP is used to form a key and never stored as a column value. |

`npm audit` across both workspaces: **0 vulnerabilities**.

### Residual risk

A leaked join code compromises one workspace's roster. That is the deliberate cost of a design
with no accounts, and it is bounded to a single school by tenant isolation. The proportionate
next step is a rotate-join-code action in the UI, alongside the per-device revocation that
already exists in the data model.
