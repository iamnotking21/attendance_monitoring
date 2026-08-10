import { afterEach, beforeEach, describe, expect, it } from "vitest";

import type { AttendanceDatabase } from "@/lib/db";
import { openDay } from "@/lib/services/attendance";
import { isDatabaseEmpty, seedDemoData } from "@/lib/services/seed";

import { closeDatabase, freshDatabase } from "../helpers/db";

let database: AttendanceDatabase;

beforeEach(async () => {
  database = await freshDatabase();
});

afterEach(async () => {
  await closeDatabase(database);
});

describe("seedDemoData", () => {
  it("fills an empty database", async () => {
    expect(await isDatabaseEmpty()).toBe(true);
    expect(await seedDemoData(new Date(2024, 2, 15, 9, 0))).toBe(true);

    expect(await database.sections.count()).toBe(2);
    expect(await database.students.count()).toBe(34);
    expect(await database.records.count()).toBeGreaterThan(0);
  });

  it("refuses to seed a database that already has data", async () => {
    await seedDemoData(new Date(2024, 2, 15, 9, 0));
    expect(await seedDemoData(new Date(2024, 2, 15, 9, 0))).toBe(false);
    expect(await database.sections.count()).toBe(2);
  });

  it("seeds once even when called concurrently", async () => {
    // React's Strict Mode fires the bootstrap effect twice in development. Before the check and
    // the writes shared a transaction, both calls saw an empty database and every section was
    // created twice.
    await Promise.all([
      seedDemoData(new Date(2024, 2, 15, 9, 0)),
      seedDemoData(new Date(2024, 2, 15, 9, 0)),
    ]);

    // The counts are the assertion that matters: before the fix these were 4 and 68.
    expect(await database.sections.count()).toBe(2);
    expect(await database.students.count()).toBe(34);
    expect(await database.schedules.count()).toBe(3);
  });

  it("hands concurrent callers the same in-flight seed rather than starting a second", async () => {
    const first = seedDemoData(new Date(2024, 2, 15, 9, 0));
    const second = seedDemoData(new Date(2024, 2, 15, 9, 0));

    expect(second).toBe(first);
    await Promise.all([first, second]);
  });

  it("produces identical data on every run, given the same clock", async () => {
    await seedDemoData(new Date(2024, 2, 15, 9, 0));
    const first = await database.records.toArray();

    await database.delete();
    database = await freshDatabase();

    await seedDemoData(new Date(2024, 2, 15, 9, 0));
    const second = await database.records.toArray();

    expect(second).toEqual(first);
  });

  it("leaves a seeded database in a state openDay can settle without error", async () => {
    await seedDemoData(new Date(2024, 2, 15, 9, 0));
    await expect(openDay(new Date(2024, 2, 15, 20, 0))).resolves.toBeUndefined();
  });
});
