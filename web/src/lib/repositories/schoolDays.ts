import type { SchoolDay } from "@/domain/model";
import type { IsoDate } from "@/domain/primitives";
import { db } from "@/lib/db";

/**
 * Records that the app was open on this date, mirroring the legacy `days` table. It is the
 * denominator for attendance reporting: a date nobody ever opened the app is not a school day,
 * and counting it would invent absences that never happened.
 */
export async function markSchoolDay(date: IsoDate): Promise<void> {
  const existing = await db().schoolDays.get(date);
  if (existing) return;
  await db().schoolDays.put({ date, firstSeenAt: new Date().toISOString() });
}

export async function listSchoolDays(): Promise<SchoolDay[]> {
  const days = await db().schoolDays.toArray();
  return days.sort((a, b) => a.date.localeCompare(b.date));
}

export async function listSchoolDaysBetween(
  start: IsoDate,
  end: IsoDate,
): Promise<SchoolDay[]> {
  const days = await db().schoolDays.where("date").between(start, end, true, true).toArray();
  return days.sort((a, b) => a.date.localeCompare(b.date));
}
