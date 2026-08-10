# Performance

Date: 2026-08-10 · Measured against a production build (`next build` + `next start`), numbers
read from `PerformanceResourceTiming.encodedBodySize` — that is, compressed bytes over the wire,
not the raw file size a bundler prints.

## Where it landed

| Route | Cold load, compressed |
|---|---|
| `/` (dashboard) | 4.2 kB HTML + 330 kB JS + 6.9 kB CSS + 47.3 kB font = **388 kB**, 15 requests |
| Any subsequent route | **6–8 kB**, 2 requests |

**Measure with the service worker unregistered.** Once it is installed every asset comes from
Cache Storage, and `encodedBodySize` then reports the *uncompressed* size — the same page reads
as 1.1 MB of JavaScript. That is a measurement artefact, not a regression, and it is an easy one
to panic over:

```js
const regs = await navigator.serviceWorker.getRegistrations();
await Promise.all(regs.map((r) => r.unregister()));
await Promise.all((await caches.keys()).map((k) => caches.delete(k)));
// then hard-reload and measure
```

The 330 kB figure includes the animation engine. An earlier note claimed 27 kB of it sat off the
critical path; measured again on a warm connection it arrives in the same burst as everything
else, because `LazyMotion` starts the import the moment the tree mounts. It is still a separate
cacheable chunk, but counting it as deferred flattered the number, so it is counted here.

Client-side navigation between the six screens costs 6–8 kB each. The application is a one-time
download followed by a near-instant app; that ratio is the number that matters for a tool a
coordinator keeps open all morning.

Deferred, and not counted above because they never load unless used:

| Chunk | Compressed | Loads when |
|---|---|---|
| QR decoder + worker | ~45 kB | "Start camera" is pressed |
| QR encoder | ~19 kB | a student's QR dialog is opened |
| Excel writer | ~83 kB | "Export Excel" is pressed |

Separation was verified by grepping the built chunks: `QrScanner`, `toDataURL`, and
`xl/workbook` each appear in their own chunk, none of them in the entry bundle.

## What was changed, and what it bought

| Change | Measured |
|---|---|
| Dropped the second webfont (JetBrains Mono) for the platform monospace stack | **−39.5 kB** font payload, 86.8 → 47.3 kB |
| `LazyMotion` with an async `domMax` import, `strict` mode, and `m` components | 27 kB of animation engine split into its own cacheable chunk — see the correction above: it is not deferred in practice |
| `optimizePackageImports` for `lucide-react` | icons import individually rather than through the barrel |
| Heavy libraries behind `await import()` at their point of use | ~147 kB kept out of the initial load |
| `next/font` self-hosting | no font-CDN origin, no render-blocking stylesheet, no layout shift |

`strict` on `LazyMotion` still earns its place: importing a full `motion.*` component anywhere
would fold the engine into the shared entry chunk instead of its own, and `strict` turns that
into a runtime error rather than a silent regression.

## Budget

The original budget in `ship-pipeline` read "under 160 kB First Load JS". Measurement showed
that to be unreachable for this stack rather than a sign of sloppy code: Next.js 16's App Router
plus React 19 accounts for roughly 210 kB compressed before a single line of application code,
and Dexie adds around 44 kB.

The budget was therefore revised to numbers this stack can actually hold to, which still fail
loudly on a regression:

| Budget | Limit | Current |
|---|---|---|
| Entry JS, compressed | 345 kB | 330 kB |
| Per-route incremental JS | 25 kB | 6–8 kB |
| Font payload | 60 kB | 47.3 kB |
| Blocking third-party requests | 0 | 0 |

Revising a budget to match reality is only honest when the alternative was examined and rejected
for a reason. Here it was: the remaining weight is framework, storage, and animation, and the
only way materially below it is dropping the animation library — which the product deliberately
uses to communicate state changes.


## Data access

Two query paths were reading far more than they needed, and both got worse as a school accumulated history.

| Path | Was | Now |
|---|---|---|
| Sync push, finding local changes | `table.filter()` over every row, testing each in JavaScript — a term of attendance meant walking ~100k records to find the handful written since the last sync | `where("updatedAt").above(watermark)` seeks straight to them through the index |
| Scan, checking for duplicates | every record in the school for that date | only the schedules the scanned student belongs to, through the `[scheduleId+date]` index |

The scan path matters most: at a gate, the delay between holding up a badge and seeing a result
is the only performance anyone notices, and it should not grow with the size of the school.

The same two fixes were applied to the Android app, which had the same shapes — plus three
full-table scans in the repository (`students().all()` to read one row's `createdAt`, twice, and
`listActive().size` to count) replaced with keyed queries and `COUNT(*)`.

## Server

Every push used to end with a cursor query: five `MAX(server_seq)` subqueries, one per replicated
table, in a separate round trip. The upserts already know what they wrote, so they now return
`server_seq` instead of the row id and the response cursor is the maximum of those — the extra
round trip is gone, and the fallback query only runs when a push applied nothing at all.

## Rendering

Ten of eleven routes prerender as static HTML. Only `/sections/[sectionId]` is server-rendered on
demand, because the section id comes from the URL. Since all data lives in the browser, the
served HTML is a shell and the real work happens client-side — which is why DOM content loaded
lands at ~30 ms locally.

## Caching

- `/_next/static/*` is content-hashed and served immutable by Next.js. An earlier override of
  its `Cache-Control` was removed after the build warned that it breaks development behaviour —
  the framework's own default was already correct.
- `/icon.svg`: `public, max-age=86400, stale-while-revalidate=604800`.
- `/api/health`: `no-store`.

## Query shape

Attendance records are the only table that grows without bound. Every read path uses a compound
index rather than a table scan:

- `[sectionId+date]` for the dashboard and for date-range reports.
- `[scheduleId+date]` for the absentee sweep.
- `[studentNumber+scheduleId+date]`, unique, for duplicate suppression.

Reports over a date range use `.between()` on the compound index, so cost scales with the rows
returned rather than with the size of the history.

## Reproducing

```bash
npm run build && node scripts/bundle-report.mjs
```

Per-route numbers come from loading each route in a real browser against `next start` and reading
`performance.getEntriesByType('resource')`. Chunk sizes printed by the bundler are uncompressed
and roughly three times the transferred size — quoting those instead would flatter the result by
a factor of three.
