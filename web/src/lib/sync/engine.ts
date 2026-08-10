import {
  changeSetSize,
  emptyChangeSet,
  incomingWins,
  type ChangeSet,
} from "@attendance/sync/protocol";

import { now } from "@/domain/primitives";
import { db } from "@/lib/db";
import {
  pullChanges,
  pushChanges,
  SyncError,
  type SyncFailure,
} from "@/lib/sync/api";
import {
  readConnection,
  readCursor,
  readPushWatermark,
  writeCursor,
  writeLastSyncedAt,
  writePushWatermark,
} from "@/lib/sync/state";

/**
 * Replication, from the device's side.
 *
 * Push first, then pull. Doing it in that order means a change made on this device is on the
 * server before anything can arrive to compete with it, so the last-write-wins comparison runs
 * against complete information rather than deciding a conflict that had not fully happened yet.
 *
 * Nothing here is required for the app to work. Every screen reads from IndexedDB, and sync is a
 * background reconciliation on top — which is why an aeroplane, a dead Wi-Fi point, or a
 * deployment with no database configured all degrade to "works exactly as before, on this
 * device".
 */

export interface SyncOutcome {
  pushed: number;
  pulled: number;
  cursor: number;
  at: string;
}

export type SyncResult =
  | { ok: true; outcome: SyncOutcome }
  | { ok: false; failure: SyncFailure };

/** Guards against a manual tap and the background timer overlapping. */
let inFlight: Promise<SyncResult> | null = null;

export function syncNow(): Promise<SyncResult> {
  inFlight ??= runSync().finally(() => {
    inFlight = null;
  });
  return inFlight;
}

async function runSync(): Promise<SyncResult> {
  const connection = await readConnection();
  if (!connection) {
    return {
      ok: false,
      failure: { kind: "unauthorized", message: "This device is not connected to a workspace." },
    };
  }

  try {
    const startedAt = now();
    const pushed = await pushLocalChanges(connection.token, startedAt);
    const pulled = await pullRemoteChanges(connection.token);

    const at = now();
    await writeLastSyncedAt(at);

    return { ok: true, outcome: { pushed, pulled: pulled.applied, cursor: pulled.cursor, at } };
  } catch (error) {
    if (error instanceof SyncError) return { ok: false, failure: error.failure };
    return {
      ok: false,
      failure: {
        kind: "rejected",
        message: error instanceof Error ? error.message : "Sync failed.",
      },
    };
  }
}

/* --------------------------------------------------------------------- push */

const PUSH_BATCH = 500;

async function pushLocalChanges(token: string, startedAt: string): Promise<number> {
  const watermark = await readPushWatermark();
  const database = db();

  const [sections, students, schedules, records, schoolDays] = await Promise.all([
    database.sections.filter((row) => row.updatedAt > watermark).toArray(),
    database.students.filter((row) => row.updatedAt > watermark).toArray(),
    database.schedules.filter((row) => row.updatedAt > watermark).toArray(),
    database.records.filter((row) => row.recordedAt > watermark).toArray(),
    database.schoolDays.filter((row) => row.firstSeenAt > watermark).toArray(),
  ]);

  const outgoing: ChangeSet = { sections, students, schedules, records, schoolDays };
  const total = changeSetSize(outgoing);
  if (total === 0) {
    await writePushWatermark(startedAt);
    return 0;
  }

  let sent = 0;
  for (const batch of splitIntoBatches(outgoing, PUSH_BATCH)) {
    await pushChanges(token, batch);
    sent += changeSetSize(batch);
  }

  // Advanced only after every batch landed. If the connection drops halfway, the watermark stays
  // where it was and the whole set is offered again next time — the server deduplicates, so
  // re-sending is cheap and losing a change is not possible.
  await writePushWatermark(startedAt);
  return sent;
}

/** Splits a change set into batches small enough for one request. */
function splitIntoBatches(changes: ChangeSet, size: number): ChangeSet[] {
  const batches: ChangeSet[] = [];
  const keys = ["sections", "students", "schedules", "records", "schoolDays"] as const;

  let current = emptyChangeSet();
  let count = 0;

  for (const key of keys) {
    for (const row of changes[key]) {
      (current[key] as unknown[]).push(row);
      count += 1;
      if (count === size) {
        batches.push(current);
        current = emptyChangeSet();
        count = 0;
      }
    }
  }

  if (count > 0) batches.push(current);
  return batches;
}

/* --------------------------------------------------------------------- pull */

async function pullRemoteChanges(token: string): Promise<{ applied: number; cursor: number }> {
  let cursor = await readCursor();
  let applied = 0;

  // Bounded so a corrupt cursor cannot spin forever; 200 pages of 500 rows is far more than any
  // school will ever have.
  for (let page = 0; page < 200; page += 1) {
    const response = await pullChanges(token, cursor, 500);
    applied += await applyChanges(response.changes);

    cursor = response.cursor;
    await writeCursor(cursor);

    if (!response.hasMore) break;
  }

  return { applied, cursor };
}

/**
 * Merges a server change set into local storage.
 *
 * Every row is compared against what is already here, and an older edit is dropped rather than
 * applied. The comparison is the same rule the server uses, so both sides converge on the same
 * answer without needing to agree in advance about who is authoritative.
 */
export async function applyChanges(changes: ChangeSet): Promise<number> {
  const database = db();
  let applied = 0;

  await database.transaction(
    "rw",
    [
      database.sections,
      database.students,
      database.schedules,
      database.records,
      database.schoolDays,
    ],
    async () => {
      for (const incoming of changes.sections) {
        const existing = await database.sections.get(incoming.id);
        if (!existing || incomingWins(incoming, existing)) {
          await database.sections.put(incoming);
          applied += 1;
        }
      }

      for (const incoming of changes.schedules) {
        const existing = await database.schedules.get(incoming.id);
        if (!existing || incomingWins(incoming, existing)) {
          await database.schedules.put(incoming);
          applied += 1;
        }
      }

      for (const incoming of changes.students) {
        const existing = await database.students.get(incoming.id);
        if (!existing || incomingWins(incoming, existing)) {
          await database.students.put(incoming);
          applied += 1;
        }
      }

      for (const incoming of changes.records) {
        // Records are append-only, and the unique index on (studentNumber, scheduleId, date) is
        // what stops a record this device already has from arriving again under a different id.
        const clash = await database.records
          .where("[studentNumber+scheduleId+date]")
          .equals([incoming.studentNumber, incoming.scheduleId, incoming.date])
          .first();
        if (clash) continue;

        await database.records.put(incoming);
        applied += 1;
      }

      for (const incoming of changes.schoolDays) {
        const existing = await database.schoolDays.get(incoming.date);
        if (existing) continue;
        await database.schoolDays.put(incoming);
        applied += 1;
      }
    },
  );

  return applied;
}

/* ------------------------------------------------------------- connectivity */

export function isOnline(): boolean {
  return typeof navigator === "undefined" ? true : navigator.onLine;
}

/**
 * Syncs on reconnect and on a slow timer.
 *
 * The timer is a fallback, not the mechanism: the `online` event covers the case that actually
 * matters — walking back into Wi-Fi — and a five-minute tick catches everything else without
 * hammering a free-tier database.
 */
export function startBackgroundSync(onResult?: (result: SyncResult) => void): () => void {
  if (typeof window === "undefined") return () => undefined;

  const INTERVAL_MS = 5 * 60 * 1000;

  const attempt = async () => {
    if (!isOnline()) return;
    if (!(await readConnection())) return;
    const result = await syncNow();
    onResult?.(result);
  };

  void attempt();

  const timer = window.setInterval(() => void attempt(), INTERVAL_MS);
  const onOnline = () => void attempt();
  window.addEventListener("online", onOnline);

  return () => {
    window.clearInterval(timer);
    window.removeEventListener("online", onOnline);
  };
}
