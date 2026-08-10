/*
 * Service worker: makes the application itself available offline.
 *
 * The data was always local — IndexedDB does not care about the network — but the JavaScript
 * that renders it still had to be fetched, so a cold start with no connection showed nothing.
 * For an app whose whole point is taking attendance at a gate, that is the difference between
 * "works offline" being true and being a slogan.
 *
 * Runtime caching rather than a build-time precache manifest: asset filenames are content-hashed
 * and change every build, and a hand-maintained list of them would rot on the first deploy.
 * The cost is that the very first visit must be online, which is unavoidable anyway.
 */

const VERSION = "v1";
const SHELL_CACHE = `attendance-shell-${VERSION}`;
const ASSET_CACHE = `attendance-assets-${VERSION}`;

/** Every screen, so visiting one online makes them all available offline. */
const ROUTES = ["/", "/scan", "/sections", "/schedules", "/reports", "/data", "/sync"];

self.addEventListener("install", (event) => {
  event.waitUntil(
    (async () => {
      const cache = await caches.open(SHELL_CACHE);
      // Individually, so one unreachable route cannot fail the whole install.
      await Promise.allSettled(ROUTES.map((route) => cache.add(route)));
      await self.skipWaiting();
    })(),
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    (async () => {
      const keys = await caches.keys();
      await Promise.all(
        keys
          .filter((key) => key !== SHELL_CACHE && key !== ASSET_CACHE)
          .map((key) => caches.delete(key)),
      );
      await self.clients.claim();
    })(),
  );
});

self.addEventListener("fetch", (event) => {
  const request = event.request;
  if (request.method !== "GET") return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  // Sync and health must never be served from a cache: a stale cursor would silently skip
  // changes, and a cached "ok" would hide an outage.
  if (url.pathname.startsWith("/api/")) return;

  if (request.mode === "navigate") {
    event.respondWith(networkFirst(request, SHELL_CACHE));
    return;
  }

  if (
    url.pathname.startsWith("/_next/static/") ||
    /\.(?:js|css|woff2?|png|svg|ico|webmanifest)$/.test(url.pathname)
  ) {
    event.respondWith(cacheFirst(request, ASSET_CACHE));
  }
});

/**
 * Fresh HTML when the network answers, the last good copy when it does not. A navigation is the
 * one request where being a few minutes stale beats showing a browser error page.
 */
async function networkFirst(request, cacheName) {
  const cache = await caches.open(cacheName);
  try {
    const response = await fetch(request);
    if (response.ok) cache.put(request, response.clone());
    return response;
  } catch {
    const cached = (await cache.match(request)) ?? (await cache.match("/"));
    if (cached) return cached;
    return new Response("Offline, and this page has not been opened before.", {
      status: 503,
      headers: { "content-type": "text/plain; charset=utf-8" },
    });
  }
}

/** Build output is content-hashed, so a cached copy at a given URL can never be wrong. */
async function cacheFirst(request, cacheName) {
  const cache = await caches.open(cacheName);
  const cached = await cache.match(request);
  if (cached) return cached;

  const response = await fetch(request);
  if (response.ok) cache.put(request, response.clone());
  return response;
}
