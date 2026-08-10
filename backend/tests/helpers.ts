import { randomUUID } from "node:crypto";

import { drizzle } from "drizzle-orm/postgres-js";
import postgres from "postgres";

import * as schema from "../src/schema";
import type { Database } from "../src/client";
import type { ChangeSet } from "../src/protocol";

export const DATABASE_URL =
  process.env.DATABASE_URL ??
  "postgres://attendance:attendance-local-only@127.0.0.1:55432/attendance";

let sql: postgres.Sql | null = null;

export function testDatabase(): Database {
  sql ??= postgres(DATABASE_URL, { max: 1, prepare: false });
  return drizzle(sql, { schema });
}

export async function closeTestDatabase(): Promise<void> {
  await sql?.end({ timeout: 5 });
  sql = null;
}

/**
 * Truncating between tests keeps them independent. `RESTART IDENTITY` is deliberately *not*
 * used on the shared sequence: cursors are only ever compared for ordering, and resetting the
 * sequence between tests would hide a bug where a cursor is compared across workspaces.
 */
export async function resetTables(): Promise<void> {
  const client = sql ?? postgres(DATABASE_URL, { max: 1, prepare: false });
  sql = client;
  await client`TRUNCATE workspaces, rate_limits CASCADE`;
}

export function changes(partial: Partial<ChangeSet> = {}): ChangeSet {
  return {
    sections: [],
    students: [],
    schedules: [],
    records: [],
    schoolDays: [],
    ...partial,
  };
}

const T0 = "2024-03-15T08:00:00.000Z";

export function aSection(overrides: Partial<ChangeSet["sections"][number]> = {}) {
  return {
    id: randomUUID(),
    name: "Grade 11 - Rizal",
    archived: false,
    createdAt: T0,
    updatedAt: T0,
    ...overrides,
  };
}

export function aSchedule(sectionId: string, overrides: Partial<ChangeSet["schedules"][number]> = {}) {
  return {
    id: randomUUID(),
    sectionId,
    title: "Morning Assembly",
    venue: "Quadrangle",
    present: { start: "07:00", end: "07:30" },
    late: { start: "07:30", end: "08:00" },
    archived: false,
    createdAt: T0,
    updatedAt: T0,
    ...overrides,
  };
}

export function aStudent(sectionId: string, overrides: Partial<ChangeSet["students"][number]> = {}) {
  return {
    id: randomUUID(),
    sectionId,
    studentNumber: "2024-1001",
    lastName: "Dela Cruz",
    firstName: "Juan",
    middleName: "Ramos",
    gender: "male" as const,
    archived: false,
    createdAt: T0,
    updatedAt: T0,
    ...overrides,
  };
}

export function aRecord(
  scheduleId: string,
  sectionId: string,
  overrides: Partial<ChangeSet["records"][number]> = {},
) {
  return {
    id: randomUUID(),
    scheduleId,
    sectionId,
    studentNumber: "2024-1001",
    date: "2024-03-15",
    status: "present" as const,
    scheduleTitle: "Morning Assembly",
    recordedAt: T0,
    ...overrides,
  };
}
