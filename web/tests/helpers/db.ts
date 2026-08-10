import { AttendanceDatabase, __setDatabaseForTests } from "@/lib/db";

let counter = 0;

/**
 * A brand-new IndexedDB per test, so nothing leaks between them and any test can run alone.
 * Backed by fake-indexeddb, loaded in `tests/setup.ts`.
 */
export async function freshDatabase(): Promise<AttendanceDatabase> {
  counter += 1;
  const database = new AttendanceDatabase(`attendance-test-${counter}`);
  __setDatabaseForTests(database);
  await database.open();
  return database;
}

export async function closeDatabase(database: AttendanceDatabase): Promise<void> {
  database.close();
  await database.delete();
  __setDatabaseForTests(null);
}
