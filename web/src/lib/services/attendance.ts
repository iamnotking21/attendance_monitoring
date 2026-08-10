import {
  absentRecordsFor,
  resolveScan,
  windowStateAt,
  type ScheduleWindowState,
} from "@/domain/attendance";
import type { NewAttendanceRecord, Schedule, Student } from "@/domain/model";
import { studentNumberSchema } from "@/domain/primitives";
import { minutesOfDay, today } from "@/domain/time";
import {
  appendRecords,
  existingKeysForDate,
  existingKeysForSchedule,
} from "@/lib/repositories/records";
import { listActiveSchedules, listSchedulesBySection } from "@/lib/repositories/schedules";
import { markSchoolDay } from "@/lib/repositories/schoolDays";
import { findActiveByStudentNumber, listStudentsBySection } from "@/lib/repositories/students";

export type ScanResult =
  /** The QR payload was not a well-formed student number. Nothing was looked up. */
  | { kind: "malformed"; reason: string }
  /** Well formed, but no active student carries that number. */
  | { kind: "unknown"; studentNumber: string }
  /** The student exists but their section has no schedule collecting scans right now. */
  | { kind: "closed"; student: Student; states: ScheduleWindowState[] }
  /** Already recorded for every open schedule — a second scan changes nothing. */
  | { kind: "duplicate"; student: Student }
  | { kind: "recorded"; student: Student; records: NewAttendanceRecord[] };

/**
 * The whole scan path, from raw camera payload to stored records.
 *
 * Validation comes first and unconditionally: the payload is attacker-controlled in the sense
 * that anyone can print a QR code and hold it to the camera, so nothing reaches a database
 * query until it has passed the student-number schema.
 */
export async function recordScan(payload: string, now: Date = new Date()): Promise<ScanResult> {
  const parsed = studentNumberSchema.safeParse(payload);
  if (!parsed.success) {
    return { kind: "malformed", reason: "That QR code is not a student ID." };
  }
  const studentNumber = parsed.data;

  const student = await findActiveByStudentNumber(studentNumber);
  if (!student) {
    return { kind: "unknown", studentNumber };
  }

  const date = today(now);
  await markSchoolDay(date);

  const schedules = await listSchedulesBySection(student.sectionId);
  const atMinutes = minutesOfDay(now);

  const resolution = resolveScan({
    studentNumber,
    schedules,
    existingKeys: await existingKeysForDate(date),
    date,
    atMinutes,
    recordedAt: now.toISOString(),
  });

  if (resolution.created.length > 0) {
    const written = await appendRecords(resolution.created);
    // A zero here means the unique index rejected every row, which can only happen if another
    // tab wrote them first. From the operator's point of view that is a duplicate scan.
    if (written === 0) return { kind: "duplicate", student };
    return { kind: "recorded", student, records: resolution.created };
  }

  if (resolution.duplicateScheduleIds.length > 0) {
    return { kind: "duplicate", student };
  }

  return {
    kind: "closed",
    student,
    states: schedules.map((schedule) => windowStateAt(schedule, atMinutes)),
  };
}

/**
 * Marks absent everyone who never scanned, for every schedule whose late window has closed.
 *
 * Runs on app open and again as each window closes. Safe to call at any time: schedules still
 * open are skipped, and students already recorded are skipped, so repeated runs converge on the
 * same result rather than piling up duplicates.
 */
export async function sweepAbsentees(now: Date = new Date()): Promise<number> {
  const date = today(now);
  const atMinutes = minutesOfDay(now);
  const schedules = await listActiveSchedules();

  let written = 0;
  for (const schedule of schedules) {
    if (schedule.id === undefined) continue;
    if (windowStateAt(schedule, atMinutes) !== "closed") continue;

    const students = await listStudentsBySection(schedule.sectionId);
    if (students.length === 0) continue;

    const absentees = absentRecordsFor({
      schedule,
      activeStudentNumbers: students.map((student) => student.studentNumber),
      existingKeys: await existingKeysForSchedule(schedule.id, date),
      date,
      atMinutes,
      recordedAt: now.toISOString(),
    });

    written += await appendRecords(absentees);
  }

  return written;
}

/** Called once when the app opens: registers the school day and settles yesterday's leftovers. */
export async function openDay(now: Date = new Date()): Promise<void> {
  await markSchoolDay(today(now));
  await sweepAbsentees(now);
}

export function describeWindowState(state: ScheduleWindowState): string {
  switch (state) {
    case "before":
      return "Not started";
    case "present":
      return "Taking attendance";
    case "gap":
      return "Between windows";
    case "late":
      return "Late arrivals only";
    case "closed":
      return "Closed";
  }
}

export function collectingSchedules(
  schedules: readonly Schedule[],
  atMinutes: number,
): Schedule[] {
  return schedules.filter((schedule) => {
    const state = windowStateAt(schedule, atMinutes);
    return state === "present" || state === "late";
  });
}
