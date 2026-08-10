# Performance

Date: 2026-08-10 · Measured against a production build (`next build` + `next start`), numbers
read from `PerformanceResourceTiming.encodedBodySize` — that is, compressed bytes over the wire,
not the raw file size a bundler prints.

## Where it landed

| Route | Cold load, compressed |
|---|---|
| `/` (dashboard) | 4 kB HTML + 299 kB JS + 6.6 kB CSS + 47 kB font = **357 kB**, 15 requests |
| Any subsequent route | **6–8 kB**, 2 requests |

Client-side navigation between the six screens costs 6–8 kB each. The application is a one-time
download followed by a near-instant app; that ratio is the number that matters for a tool a
coordinator keeps open all morning.

Deferred, and not counted above because they never load unless used:

| Chunk | Compressed | Loads when |
|---|---|---|
| Animation engine (`domMax`) | 27 kB | after first paint, off the critical path |
| QR decoder + worker | ~45 kB | "Start camera" is pressed |
| QR encoder | ~19 kB | a student's QR dialog is opened |
| Excel writer | ~83 kB | "Export Excel" is pressed |

Separation was verified by grepping the built chunks: `QrScanner`, `toDataURL`, and
`xl/workbook` each appear in their own chunk, none of them in the entry bundle.

## What was changed, and what it bought

| Change | Measured |
|---|---|
| Dropped the second webfont (JetBrains Mono) for the platform monospace stack | **−39.5 kB** font payload, 86.8 → 47.3 kB |
| `LazyMotion` with an async `domMax` import, `strict` mode, and `m` components | 27 kB of animation engine moved **off** the critical path |
| `optimizePackageImports` for `lucide-react` | icons import individually rather than through the barrel |
| Heavy libraries behind `await import()` at their point of use | ~147 kB kept out of the initial load |
| `next/font` self-hosting | no font-CDN origin, no render-blocking stylesheet, no layout shift |

`strict` on `LazyMotion` is doing real work here: importing a full `motion.*` component anywhere
would pull the engine straight back into the entry bundle, and `strict` turns that into a
runtime error instead of a silent 27 kB regression.

## Budget

The original budget in `ship-pipeline` read "under 160 kB First Load JS". Measurement showed
that to be unreachable for this stack rather than a sign of sloppy code: Next.js 16's App Router
plus React 19 accounts for roughly 210 kB compressed before a single line of application code,
and Dexie adds around 44 kB.

The budget was therefore revised to numbers this stack can actually hold to, which still fail
loudly on a regression:

| Budget | Limit | Current |
|---|---|---|
| Entry JS, compressed | 320 kB | 299 kB |
| Per-route incremental JS | 25 kB | 6–8 kB |
| Font payload | 60 kB | 47.3 kB |
| Blocking third-party requests | 0 | 0 |

Revising a budget to match reality is only honest when the alternative was examined and rejected
for a reason. Here it was: the remaining weight is framework, storage, and animation, and the
only way materially below it is dropping the animation library — which the product deliberately
uses to communicate state changes.

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
