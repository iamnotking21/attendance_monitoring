import { newId, now } from "@/domain/primitives";
import { db } from "@/lib/db";

const LEGACY_DATABASE = "attendance_monitoring";
const MIGRATION_FLAG = "migrated:legacy-numeric-ids";

/**
 * Moves data from the first release, which used auto-incrementing integer primary keys, into the
 * UUID-keyed database.
 *
 * IndexedDB cannot change a primary key in place, so this reads the old database, rewrites every
 * identifier and every reference to one, and writes the result into the new one. Foreign keys are
 * remapped through a lookup built during the pass, so a student still points at the right section
 * afterwards. Attendance records reference schedules by id and students by student number, and
 * the number is stable, so only `scheduleId` and `sectionId` need rewriting.
 *
 * Runs once. The old database is deleted only after the new one has been written, so an
 * interruption leaves the original intact.
 */
export async function migrateLegacyDatabase(): Promise<boolean> {
  if (typeof indexedDB === "undefined") return false;
  if (await db().meta.get(MIGRATION_FLAG)) return false;

  const legacy = await openLegacyDatabase();
  if (!legacy) {
    await db().meta.put({ key: MIGRATION_FLAG, value: now() });
    return false;
  }

  try {
    const [sections, students, schedules, records, schoolDays] = await Promise.all([
      readAll(legacy, "sections"),
      readAll(legacy, "students"),
      readAll(legacy, "schedules"),
      readAll(legacy, "records"),
      readAll(legacy, "schoolDays"),
    ]);

    if (sections.length === 0 && students.length === 0 && records.length === 0) {
      legacy.close();
      await deleteDatabase(LEGACY_DATABASE);
      await db().meta.put({ key: MIGRATION_FLAG, value: now() });
      return false;
    }

    const sectionIds = new Map<number, string>();
    const scheduleIds = new Map<number, string>();
    const timestamp = now();

    const nextSections = sections.map((row) => {
      const id = newId();
      sectionIds.set(row.id as number, id);
      return {
        id,
        name: String(row.name ?? "Untitled section"),
        archived: Boolean(row.archived),
        createdAt: asTimestamp(row.createdAt, timestamp),
        updatedAt: timestamp,
      };
    });

    const nextSchedules = schedules.map((row) => {
      const id = newId();
      scheduleIds.set(row.id as number, id);
      return {
        id,
        sectionId: sectionIds.get(row.sectionId as number) ?? newId(),
        title: String(row.title ?? "Untitled"),
        venue: String(row.venue ?? ""),
        present: row.present as { start: string; end: string },
        late: row.late as { start: string; end: string },
        archived: Boolean(row.archived),
        createdAt: asTimestamp(row.createdAt, timestamp),
        updatedAt: timestamp,
      };
    });

    const nextStudents = students.map((row) => ({
      id: newId(),
      sectionId: sectionIds.get(row.sectionId as number) ?? newId(),
      studentNumber: String(row.studentNumber),
      lastName: String(row.lastName ?? ""),
      firstName: String(row.firstName ?? ""),
      middleName: String(row.middleName ?? ""),
      gender: row.gender === "female" ? ("female" as const) : ("male" as const),
      archived: Boolean(row.archived),
      createdAt: asTimestamp(row.createdAt, timestamp),
      updatedAt: timestamp,
    }));

    const nextRecords = records
      .filter((row) => scheduleIds.has(row.scheduleId as number))
      .map((row) => ({
        id: newId(),
        scheduleId: scheduleIds.get(row.scheduleId as number)!,
        sectionId: sectionIds.get(row.sectionId as number) ?? newId(),
        studentNumber: String(row.studentNumber),
        date: String(row.date),
        status: row.status as "present" | "late" | "absent",
        scheduleTitle: String(row.scheduleTitle ?? ""),
        recordedAt: asTimestamp(row.recordedAt, timestamp),
      }));

    const nextSchoolDays = schoolDays.map((row) => ({
      date: String(row.date),
      firstSeenAt: asTimestamp(row.firstSeenAt, timestamp),
    }));

    const database = db();
    await database.transaction(
      "rw",
      [
        database.sections,
        database.students,
        database.schedules,
        database.records,
        database.schoolDays,
        database.meta,
      ],
      async () => {
        await database.sections.bulkPut(nextSections);
        await database.schedules.bulkPut(nextSchedules);
        await database.students.bulkPut(nextStudents);
        await database.records.bulkPut(nextRecords);
        await database.schoolDays.bulkPut(nextSchoolDays);
        await database.meta.put({ key: MIGRATION_FLAG, value: timestamp });
      },
    );

    legacy.close();
    await deleteDatabase(LEGACY_DATABASE);
    return true;
  } catch (error) {
    legacy.close();
    // Leave the old database in place and the flag unset: a failed migration must be
    // retryable, and losing a term's attendance to a half-finished copy is unacceptable.
    console.error("Could not migrate the previous local database:", error);
    return false;
  }
}

function openLegacyDatabase(): Promise<IDBDatabase | null> {
  return new Promise((resolve) => {
    let existed = true;
    const request = indexedDB.open(LEGACY_DATABASE);

    request.onupgradeneeded = () => {
      // Opening a database that does not exist creates it. This fires only in that case.
      existed = false;
    };
    request.onsuccess = () => {
      const database = request.result;
      if (!existed || database.objectStoreNames.length === 0) {
        database.close();
        void deleteDatabase(LEGACY_DATABASE);
        resolve(null);
        return;
      }
      resolve(database);
    };
    request.onerror = () => resolve(null);
    request.onblocked = () => resolve(null);
  });
}

function readAll(
  database: IDBDatabase,
  store: string,
): Promise<Record<string, unknown>[]> {
  return new Promise((resolve) => {
    if (!database.objectStoreNames.contains(store)) {
      resolve([]);
      return;
    }
    const request = database.transaction(store, "readonly").objectStore(store).getAll();
    request.onsuccess = () => resolve(request.result as Record<string, unknown>[]);
    request.onerror = () => resolve([]);
  });
}

function deleteDatabase(name: string): Promise<void> {
  return new Promise((resolve) => {
    const request = indexedDB.deleteDatabase(name);
    request.onsuccess = request.onerror = request.onblocked = () => resolve();
  });
}

function asTimestamp(value: unknown, fallback: string): string {
  if (typeof value !== "string") return fallback;
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? fallback : new Date(parsed).toISOString();
}
