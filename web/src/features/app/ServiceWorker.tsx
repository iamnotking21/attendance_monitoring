"use client";

import { useEffect } from "react";

/**
 * Registers the service worker that makes the app itself work offline.
 *
 * Registered after load rather than during it, so fetching and installing the worker never
 * competes with the first paint.
 */
export function ServiceWorker() {
  useEffect(() => {
    if (typeof navigator === "undefined" || !("serviceWorker" in navigator)) return;
    // A worker registered from a dev server would cache development assets and then serve them
    // over a later production build.
    if (process.env.NODE_ENV !== "production") return;

    const register = () => {
      void navigator.serviceWorker.register("/sw.js").catch((error) => {
        // Not fatal: without it the app simply behaves as it did before, needing the network to
        // load but not to work.
        console.warn("Offline support could not be enabled:", error);
      });
    };

    if (document.readyState === "complete") register();
    else window.addEventListener("load", register, { once: true });

    return () => window.removeEventListener("load", register);
  }, []);

  return null;
}
