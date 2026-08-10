import Dexie, { type EntityTable } from "dexie";

import type {
  AttendanceRecord,
  Schedule,
  SchoolDay,
  Section,
  Student,
} from "@/domain/model";

/**
 * All data lives in the browser, exactly as the original Android app kept it on the device.
 * Nothing leaves the machine, which is the right default for a roster of minors' names: there
 * is no server to breach and no third party to trust.
 */
export class AttendanceDatabase extends Dexie {
  sections!: EntityTable<Section, "id">;
  students!: EntityTable<Student, "id">;
  schedules!: EntityTable<Schedule, "id">;
  records!: EntityTable<AttendanceRecord, "id">;
  schoolDays!: EntityTable<SchoolDay, "date">;

  constructor(name = "attendance_monitoring") {
    super(name);

    this.version(1).stores({
      sections: "++id, name, archived",
      students: "++id, sectionId, studentNumber, archived, [sectionId+archived]",
      schedules: "++id, sectionId, archived, [sectionId+archived]",
      // The compound unique index is the real guarantee behind "one record per student, per
      // schedule, per day". Enforcing it in application code alone would lose the race between
      // a double-tap scan and the absentee sweep.
      records:
        "++id, date, sectionId, scheduleId, studentNumber, &[studentNumber+scheduleId+date], [sectionId+date], [scheduleId+date]",
      schoolDays: "&date",
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
