import Dexie from "dexie";

import { recordKey, type AttendanceRecord, type NewAttendanceRecord } from "@/domain/model";
import { newId } from "@/domain/primitives";
import type { IsoDate } from "@/domain/primitives";
import { db } from "@/lib/db";

export async function listRecordsByDate(date: IsoDate): Promise<AttendanceRecord[]> {
  return db().records.where({ date }).toArray();
}

export async function listRecordsBySectionAndDate(
  sectionId: string,
  date: IsoDate,
): Promise<AttendanceRecord[]> {
  return db().records.where("[sectionId+date]").equals([sectionId, date]).toArray();
}

export async function listRecordsBySectionBetween(
  sectionId: string,
  start: IsoDate,
  end: IsoDate,
): Promise<AttendanceRecord[]> {
  return db()
    .records.where("[sectionId+date]")
    .between([sectionId, start], [sectionId, end], true, true)
    .toArray();
}

export async function listRecordsBetween(
  start: IsoDate,
  end: IsoDate,
): Promise<AttendanceRecord[]> {
  return db().records.where("date").between(start, end, true, true).toArray();
}

export async function countRecords(): Promise<number> {
  return db().records.count();
}

/** Keys already written for a date, for the duplicate check the domain layer performs. */
export async function existingKeysForDate(date: IsoDate): Promise<Set<string>> {
  const records = await listRecordsByDate(date);
  return new Set(records.map(recordKey));
}

export async function existingKeysForSchedule(
  scheduleId: string,
  date: IsoDate,
): Promise<Set<string>> {
  const records = await db()
    .records.where("[scheduleId+date]")
    .equals([scheduleId, date])
    .toArray();
  return new Set(records.map(recordKey));
}

/**
 * Appends records, tolerating the unique-index rejections that a genuine race produces.
 *
 * Two tabs scanning the same badge, or a scan landing at the same instant as the absentee
 * sweep, both end with one side losing the insert. That is the index doing its job — the
 * correct response is to count the rejection and carry on, not to surface an error the operator
 * can do nothing about.
 */
export async function appendRecords(records: readonly NewAttendanceRecord[]): Promise<number> {
  if (records.length === 0) return 0;

  try {
    await db().records.bulkAdd(records.map((record) => ({ ...record, id: newId() })));
    return records.length;
  } catch (error) {
    if (error instanceof Dexie.BulkError) {
      return records.length - error.failures.length;
    }
    throw error;
  }
}
