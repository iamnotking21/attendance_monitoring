"use client";

import { useEffect } from "react";

import { openDay } from "@/lib/services/attendance";
import { seedDemoData } from "@/lib/services/seed";

/**
 * Runs once per page load, in the browser only.
 *
 * Two jobs, in order: put demo data in front of a first-time visitor so the app is not six
 * empty screens, then register today as a school day and settle any schedule whose late window
 * closed while nobody had the app open. The original Android app did the same work from a
 * broadcast receiver on launch.
 */
export function AppBootstrap() {
  useEffect(() => {
    let cancelled = false;

    void (async () => {
      try {
        await seedDemoData();
        if (cancelled) return;
        await openDay();
      } catch (error) {
        // Storage can be unavailable outright — private windows, blocked cookies, a full disk.
        // The app still renders; the screens that need data say so themselves.
        console.error("Startup could not reach local storage:", error);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  return null;
}
