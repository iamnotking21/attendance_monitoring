import { createBackup, parseBackupJson, type Backup } from "@/domain/backup";
import { db } from "@/lib/db";

export async function exportBackup(now: Date = new Date()): Promise<Backup> {
  const database = db();
  const [sections, students, schedules, records, schoolDays] = await Promise.all([
    database.sections.toArray(),
    database.students.toArray(),
    database.schedules.toArray(),
    database.records.toArray(),
    database.schoolDays.toArray(),
  ]);

  return createBackup({ sections, students, schedules, records, schoolDays }, now.toISOString());
}

export type RestoreResult =
  | { ok: true; counts: Record<keyof Backup["data"], number> }
  | { ok: false; error: string };

/**
 * Replaces the entire database with the contents of a backup.
 *
 * Everything happens inside one transaction, so a file that fails partway through leaves the
 * existing data untouched rather than half-wiped. The caller is responsible for confirming with
 * the user first — this is destructive by design.
 */
export async function restoreBackupFromJson(text: string): Promise<RestoreResult> {
  const parsed = parseBackupJson(text);
  if (!parsed.ok) return { ok: false, error: parsed.error };

  const { data } = parsed.backup;
  const database = db();

  try {
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
        await Promise.all([
          database.sections.clear(),
          database.students.clear(),
          database.schedules.clear(),
          database.records.clear(),
          database.schoolDays.clear(),
        ]);

        await database.sections.bulkAdd(data.sections);
        await database.students.bulkAdd(data.students);
        await database.schedules.bulkAdd(data.schedules);
        await database.records.bulkAdd(data.records);
        await database.schoolDays.bulkAdd(data.schoolDays);
      },
    );
  } catch (error) {
    return {
      ok: false,
      error:
        error instanceof Error
          ? `Restore failed, and your existing data was left unchanged: ${error.message}`
          : "Restore failed, and your existing data was left unchanged.",
    };
  }

  return {
    ok: true,
    counts: {
      sections: data.sections.length,
      students: data.students.length,
      schedules: data.schedules.length,
      records: data.records.length,
      schoolDays: data.schoolDays.length,
    },
  };
}

/** Wipes everything. Destructive and irreversible — always confirm before calling. */
export async function resetDatabase(): Promise<void> {
  const database = db();
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
      await Promise.all([
        database.sections.clear(),
        database.students.clear(),
        database.schedules.clear(),
        database.records.clear(),
        database.schoolDays.clear(),
      ]);
    },
  );
}

export interface StorageUsage {
  usedBytes: number;
  quotaBytes: number;
}

export async function storageUsage(): Promise<StorageUsage | null> {
  if (typeof navigator === "undefined" || !navigator.storage?.estimate) return null;
  const estimate = await navigator.storage.estimate();
  if (estimate.usage === undefined || estimate.quota === undefined) return null;
  return { usedBytes: estimate.usage, quotaBytes: estimate.quota };
}
