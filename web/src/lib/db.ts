import Dexie, { type EntityTable } from "dexie";

import type {
  AttendanceRecord,
  Schedule,
  SchoolDay,
  Section,
  Student,
} from "@/domain/model";

/**
 * Local storage entry for anything that is about this device rather than about the school —
 * the sync cursor, the workspace credentials, the device identifier. Deliberately not synced.
 */
export interface MetaEntry {
  key: string;
  value: unknown;
}

/**
 * The device's own copy of everything, and the source of truth while offline.
 *
 * Named `_v2` because the primary keys changed from auto-incrementing integers to UUIDs, which
 * IndexedDB cannot alter in place. `migrateLegacyDatabase` moves any v1 data across on startup.
 */
export class AttendanceDatabase extends Dexie {
  sections!: EntityTable<Section, "id">;
  students!: EntityTable<Student, "id">;
  schedules!: EntityTable<Schedule, "id">;
  records!: EntityTable<AttendanceRecord, "id">;
  schoolDays!: EntityTable<SchoolDay, "date">;
  meta!: EntityTable<MetaEntry, "key">;

  constructor(name = "attendance_monitoring_v2") {
    super(name);

    this.version(1).stores({
      // `id` without `++`: the application mints a UUID, storage never assigns one.
      sections: "id, name, archived, updatedAt",
      students: "id, sectionId, studentNumber, archived, updatedAt, [sectionId+archived]",
      schedules: "id, sectionId, archived, updatedAt, [sectionId+archived]",
      // The compound unique index is the real guarantee behind "one record per student, per
      // schedule, per day". Enforcing it in application code alone would lose the race between
      // a double-tap scan and the absentee sweep — and, now, between two devices syncing.
      records:
        "id, date, sectionId, scheduleId, studentNumber, recordedAt, &[studentNumber+scheduleId+date], [sectionId+date], [scheduleId+date]",
      schoolDays: "&date",
      meta: "key",
    });
  }
}

let instance: AttendanceDatabase | null = null;

/**
 * Lazily created so that importing a repository during server rendering never touches
 * IndexedDB, which does not exist in Node.
 */
export function db(): AttendanceDatabase {
  instance ??= new AttendanceDatabase();
  return instance;
}

/** Test seam: swap in a database backed by fake-indexeddb. */
export function __setDatabaseForTests(next: AttendanceDatabase | null): void {
  instance = next;
}

export function isQuotaError(error: unknown): boolean {
  return (
    error instanceof Error &&
    (error.name === "QuotaExceededError" || error.name === "NotEnoughSpaceError")
  );
}

/* ------------------------------------------------------------------ metadata */

export async function readMeta<T>(key: string): Promise<T | undefined> {
  const entry = await db().meta.get(key);
  return entry?.value as T | undefined;
}

export async function writeMeta(key: string, value: unknown): Promise<void> {
  await db().meta.put({ key, value });
}

export async function clearMeta(key: string): Promise<void> {
  await db().meta.delete(key);
}
